package com.nuevoso.launcher.agent

import android.content.Context
import com.nuevoso.launcher.agent.security.ActionAuditEvent
import com.nuevoso.launcher.agent.security.ActionLifecycleStage
import com.nuevoso.launcher.agent.security.ActionRequest
import com.nuevoso.launcher.agent.security.ActionRequestFactory
import com.nuevoso.launcher.agent.security.ArgumentSanitizer
import com.nuevoso.launcher.agent.security.ExecutionResultCategory
import com.nuevoso.launcher.agent.security.PolicyDecision
import com.nuevoso.launcher.agent.security.PolicyEngine
import com.nuevoso.launcher.agent.security.SafeFailureCode
import com.nuevoso.launcher.agent.executors.AccessibilityExecutor
import com.nuevoso.launcher.agent.executors.InstallAppExecutor
import com.nuevoso.launcher.agent.executors.DialExecutor
import com.nuevoso.launcher.agent.executors.OpenAppExecutor
import com.nuevoso.launcher.agent.executors.SearchWebExecutor
import com.nuevoso.launcher.agent.executors.SetAlarmExecutor
import com.nuevoso.launcher.agent.executors.ToggleSettingExecutor
import com.nuevoso.launcher.ai.ToolCall
import com.nuevoso.launcher.ai.ToolResult
import com.nuevoso.launcher.data.apps.AppRepository
import com.nuevoso.launcher.data.memory.MemoryRepository

fun interface ToolCallExecutor {
    suspend fun execute(call: ToolCall): String
}

fun interface ActionAuditRecorder {
    suspend fun record(event: ActionAuditEvent)
}

class ActionDispatcher(
    private val executor: ToolCallExecutor,
    private val auditRecorder: ActionAuditRecorder,
    private val requestFactory: ActionRequestFactory = ActionRequestFactory(),
    private val policyEngine: PolicyEngine = PolicyEngine(),
) {
    private val fallbackDiagnostics = mutableListOf<SafeFailureCode>()

    constructor(
        context: Context,
        appRepository: AppRepository,
        memoryRepository: MemoryRepository,
    ) : this(
        executor = DefaultToolCallExecutor(context, appRepository, memoryRepository),
        auditRecorder = ActionAuditRecorder { event -> memoryRepository.recordActionAudit(event) },
    )

    suspend fun dispatch(call: ToolCall): ToolResult {
        val request = requestFactory.from(call)
        val decision = policyEngine.evaluate(request)

        return when (decision) {
            is PolicyDecision.Allow -> {
                if (!recordMandatoryAudit(
                        auditEvent(
                            request = request,
                            decision = decision,
                            lifecycleStage = ActionLifecycleStage.EXECUTION_STARTED,
                            category = ExecutionResultCategory.NOT_EXECUTED,
                        )
                    )
                ) {
                    return ToolResult(id = call.id, result = auditPersistenceFailureResult())
                }
                executeAfterPreAudit(call, request, decision)
            }
            is PolicyDecision.RequireConfirmation -> {
                if (!recordMandatoryAudit(
                        auditEvent(
                            request = request,
                            decision = decision,
                            lifecycleStage = ActionLifecycleStage.CONFIRMATION_REQUIRED,
                            category = ExecutionResultCategory.BLOCKED_BY_POLICY,
                        )
                    )
                ) {
                    return ToolResult(id = call.id, result = auditPersistenceFailureResult())
                }
                ToolResult(id = call.id, result = blockedResult(request, decision))
            }
            is PolicyDecision.Deny -> {
                if (!recordMandatoryAudit(
                        auditEvent(
                            request = request,
                            decision = decision,
                            lifecycleStage = ActionLifecycleStage.DENIED,
                            category = ExecutionResultCategory.BLOCKED_BY_POLICY,
                        )
                    )
                ) {
                    return ToolResult(id = call.id, result = auditPersistenceFailureResult())
                }
                ToolResult(id = call.id, result = blockedResult(request, decision))
            }
        }
    }

    private suspend fun executeAfterPreAudit(
        call: ToolCall,
        request: ActionRequest,
        decision: PolicyDecision,
    ): ToolResult {
        val execution = try {
            ExecutionOutcome(
                result = executor.execute(call),
                lifecycleStage = ActionLifecycleStage.EXECUTION_SUCCEEDED,
                category = ExecutionResultCategory.EXECUTED,
                safeFailureCode = SafeFailureCode.NONE,
            )
        } catch (e: Exception) {
            ExecutionOutcome(
                result = "Action failed safely. The local executor reported a controlled failure.",
                lifecycleStage = ActionLifecycleStage.EXECUTION_FAILED,
                category = ExecutionResultCategory.FAILED,
                safeFailureCode = e.toSafeFailureCode(),
            )
        }

        val finalAuditRecorded = recordFinalAudit(
            auditEvent(
                request = request,
                decision = decision,
                lifecycleStage = execution.lifecycleStage,
                category = execution.category,
                safeFailureCode = execution.safeFailureCode,
            )
        )

        return if (finalAuditRecorded) {
            ToolResult(id = call.id, result = execution.result)
        } else {
            ToolResult(
                id = call.id,
                result = "Action may have completed, but local audit finalization failed. The executor was not retried.",
            )
        }
    }

    private suspend fun recordMandatoryAudit(event: ActionAuditEvent): Boolean {
        return try {
            auditRecorder.record(event)
            true
        } catch (e: Exception) {
            recordFallbackDiagnostic(e.toSafeFailureCode())
            false
        }
    }

    private suspend fun recordFinalAudit(event: ActionAuditEvent): Boolean {
        return try {
            auditRecorder.record(event)
            true
        } catch (e: Exception) {
            recordFallbackDiagnostic(SafeFailureCode.AUDIT_PERSISTENCE_FAILED)
            false
        }
    }

    private fun recordFallbackDiagnostic(code: SafeFailureCode) {
        fallbackDiagnostics += code
    }

    private fun auditPersistenceFailureResult(): String {
        return "Action was not executed because the local security audit could not be recorded."
    }

    private fun blockedResult(request: ActionRequest, decision: PolicyDecision): String {
        // TODO(TASK-RUNTIME-001): Replace this safe refusal with a consent lifecycle and confirmation UI.
        return listOf(
            "Action blocked by policy.",
            "decision=${decision.type.name}",
            "risk=${request.riskLevel.name}",
            "tool=${request.toolName}",
            "reason=${decision.reason}",
        ).joinToString("\n")
    }

    private fun auditEvent(
        request: ActionRequest,
        decision: PolicyDecision,
        lifecycleStage: ActionLifecycleStage,
        category: ExecutionResultCategory,
        safeFailureCode: SafeFailureCode = SafeFailureCode.NONE,
    ) = ActionAuditEvent(
        actionId = request.actionId,
        timestampMillis = System.currentTimeMillis(),
        toolName = request.toolName,
        riskLevel = request.riskLevel,
        policyDecision = decision.type,
        lifecycleStage = lifecycleStage,
        sanitizedSummary = ArgumentSanitizer.summarize(request.sanitizedArguments),
        executionResultCategory = category,
        safeFailureCode = safeFailureCode,
    )

    private fun Exception.toSafeFailureCode(): SafeFailureCode {
        return when (this) {
            is SecurityException -> SafeFailureCode.PERMISSION_DENIED
            is IllegalArgumentException -> SafeFailureCode.VALIDATION_FAILED
            is IllegalStateException -> SafeFailureCode.EXECUTOR_UNAVAILABLE
            else -> SafeFailureCode.UNKNOWN_FAILURE
        }
    }

    private data class ExecutionOutcome(
        val result: String,
        val lifecycleStage: ActionLifecycleStage,
        val category: ExecutionResultCategory,
        val safeFailureCode: SafeFailureCode,
    )
}

private class DefaultToolCallExecutor(
    context: Context,
    private val appRepository: AppRepository,
    private val memoryRepository: MemoryRepository,
) : ToolCallExecutor {
    private val accessibility = AccessibilityExecutor()
    private val installApp = InstallAppExecutor(context)
    private val openApp = OpenAppExecutor(context, appRepository)
    private val searchWeb = SearchWebExecutor(context)
    private val setAlarm = SetAlarmExecutor(context)
    private val dial = DialExecutor(context)
    private val toggle = ToggleSettingExecutor(context)

    override suspend fun execute(call: ToolCall): String {
        return when (call.name) {
            "open_app" -> openApp.execute(call.args["app_name"] ?: "")
            "search_web" -> searchWeb.execute(call.args["query"] ?: "")
            "set_alarm" -> setAlarm.execute(
                hour = call.args["hour"] ?: "0",
                minute = call.args["minute"] ?: "0",
                label = call.args["label"],
            )
            "call" -> dial.execute(call.args["target"] ?: "")
            "toggle_setting" -> toggle.execute(
                setting = call.args["setting"] ?: "",
                value = call.args["value"],
            )
            "install_app" -> installApp.execute(call.args["app_name"] ?: "")
            "read_screen" -> accessibility.readScreen()
            "tap_element" -> accessibility.tapElement(call.args["description"] ?: "")
            "type_text" -> accessibility.typeText(call.args["text"] ?: "")
            "scroll_screen" -> accessibility.scrollScreen(call.args["direction"] ?: "down")
            "press_back" -> accessibility.pressBack()
            "list_apps" -> {
                val apps = appRepository.getAllApps()
                if (apps.isEmpty()) "No se encontraron aplicaciones instaladas."
                else apps.joinToString(", ") { it.label }
            }
            "remember_fact" -> {
                val fact = call.args["fact"] ?: ""
                if (fact.isNotBlank()) memoryRepository.rememberFact(fact)
                "Recordado."
            }
            else -> "Herramienta desconocida: ${call.name}"
        }
    }
}
