package com.nuevoso.launcher.agent

import android.content.Context
import com.nuevoso.launcher.agent.security.ActionAuditEvent
import com.nuevoso.launcher.agent.security.ActionRequest
import com.nuevoso.launcher.agent.security.ActionRequestFactory
import com.nuevoso.launcher.agent.security.ArgumentSanitizer
import com.nuevoso.launcher.agent.security.ExecutionResultCategory
import com.nuevoso.launcher.agent.security.PolicyDecision
import com.nuevoso.launcher.agent.security.PolicyEngine
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

        var result: String
        var category: ExecutionResultCategory
        var failureReason: String?

        when (decision) {
            is PolicyDecision.Allow -> {
                try {
                    result = executor.execute(call)
                    category = ExecutionResultCategory.EXECUTED
                    failureReason = null
                } catch (e: Exception) {
                    result = "Error: ${e.message ?: "fallo al ejecutar ${call.name}"}"
                    category = ExecutionResultCategory.FAILED
                    failureReason = e.message ?: "Unknown execution failure"
                }
            }
            is PolicyDecision.RequireConfirmation,
            is PolicyDecision.Deny -> {
                result = blockedResult(request, decision)
                category = ExecutionResultCategory.BLOCKED_BY_POLICY
                failureReason = decision.reason
            }
        }
        auditRecorder.record(auditEvent(request, decision, category, failureReason))
        return ToolResult(id = call.id, result = result)
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
        category: ExecutionResultCategory,
        failureReason: String?,
    ) = ActionAuditEvent(
        actionId = request.actionId,
        timestampMillis = request.timestampMillis,
        toolName = request.toolName,
        riskLevel = request.riskLevel,
        policyDecision = decision.type,
        sanitizedSummary = ArgumentSanitizer.summarize(request.sanitizedArguments),
        executionResultCategory = category,
        failureReason = failureReason,
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
