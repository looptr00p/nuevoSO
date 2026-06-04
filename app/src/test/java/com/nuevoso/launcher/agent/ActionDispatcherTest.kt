package com.nuevoso.launcher.agent

import com.nuevoso.launcher.agent.security.ActionAuditEvent
import com.nuevoso.launcher.agent.security.ActionLifecycleStage
import com.nuevoso.launcher.agent.security.ExecutionResultCategory
import com.nuevoso.launcher.agent.security.PolicyDecisionType
import com.nuevoso.launcher.agent.security.SafeFailureCode
import com.nuevoso.launcher.ai.ToolCall
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionDispatcherTest {
    @Test
    fun preExecutionAuditFailurePreventsExecution() = runBlocking {
        var executed = false
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                executed = true
                "executed"
            },
            auditRecorder = ActionAuditRecorder { error("database path /private/value failed") },
        )

        val result = dispatcher.dispatch(
            ToolCall(id = "open", name = "open_app", args = mapOf("app_name" to "Clock"))
        )

        assertFalse(executed)
        assertEquals(
            "Action was not executed because the local security audit could not be recorded.",
            result.result,
        )
    }

    @Test
    fun confirmationRequiredActionsDoNotCallExecutorsAndAreAudited() = runBlocking {
        var executed = false
        val audits = mutableListOf<ActionAuditEvent>()
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                executed = true
                "executed"
            },
            auditRecorder = ActionAuditRecorder { audits += it },
        )

        val result = dispatcher.dispatch(
            ToolCall(id = "tap", name = "tap_element", args = mapOf("description" to "Confirm purchase"))
        )

        assertFalse(executed)
        assertTrue(result.result.contains("Action blocked by policy."))
        assertEquals(PolicyDecisionType.REQUIRE_CONFIRMATION, audits.single().policyDecision)
        assertEquals(ActionLifecycleStage.CONFIRMATION_REQUIRED, audits.single().lifecycleStage)
        assertEquals(ExecutionResultCategory.BLOCKED_BY_POLICY, audits.single().executionResultCategory)
    }

    @Test
    fun pendingApprovalIsIssuedOnlyForRequireConfirmationDecisions() = runBlocking {
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor { "executed" },
            auditRecorder = ActionAuditRecorder { },
        )

        val allow = dispatcher.dispatchForAgent(
            ToolCall(id = "open", name = "open_app", args = mapOf("app_name" to "Clock"))
        )
        val requireConfirmation = dispatcher.dispatchForAgent(
            ToolCall(id = "search", name = "search_web", args = mapOf("query" to "synthetic query"))
        )
        val deny = dispatcher.dispatchForAgent(
            ToolCall(id = "unknown", name = "do_anything", args = emptyMap())
        )

        assertTrue(allow is ActionDispatchResult.Completed)
        assertTrue(requireConfirmation is ActionDispatchResult.PendingConfirmation)
        assertTrue(deny is ActionDispatchResult.Completed)
    }

    @Test
    fun modelFacingDispatchDoesNotExposeApprovalToken() = runBlocking {
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor { "executed" },
            auditRecorder = ActionAuditRecorder { },
        )

        val result = dispatcher.dispatch(
            ToolCall(id = "tap", name = "tap_element", args = mapOf("description" to "Submit synthetic form"))
        )

        assertTrue(result.result.contains("Action blocked by policy."))
        assertFalse(result.result.contains("token"))
        assertFalse(result.result.contains("approval"))
    }

    @Test
    fun userRejectionPreventsExecutionAndAuditsRejection() = runBlocking {
        var executed = false
        val audits = mutableListOf<ActionAuditEvent>()
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                executed = true
                "executed"
            },
            auditRecorder = ActionAuditRecorder { audits += it },
        )

        val pending = dispatcher.dispatchForAgent(
            ToolCall(id = "tap", name = "tap_element", args = mapOf("description" to "Submit synthetic form"))
        ) as ActionDispatchResult.PendingConfirmation
        val result = dispatcher.resolveApproval(pending.pending.prompt, approved = false)

        assertFalse(executed)
        assertEquals("Action rejected by the user.", result.result)
        assertEquals(
            listOf(ActionLifecycleStage.CONFIRMATION_REQUIRED, ActionLifecycleStage.CONFIRMATION_REJECTED),
            audits.map { it.lifecycleStage },
        )
    }

    @Test
    fun validApprovalExecutesOnceAndRecordsGrantedLifecycle() = runBlocking {
        var executionCount = 0
        val audits = mutableListOf<ActionAuditEvent>()
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                executionCount += 1
                "searched"
            },
            auditRecorder = ActionAuditRecorder { audits += it },
        )

        val pending = dispatcher.dispatchForAgent(
            ToolCall(id = "search", name = "search_web", args = mapOf("query" to "synthetic query"))
        ) as ActionDispatchResult.PendingConfirmation
        val result = dispatcher.resolveApproval(pending.pending.prompt, approved = true)

        assertEquals(1, executionCount)
        assertEquals("searched", result.result)
        assertEquals(
            listOf(
                ActionLifecycleStage.CONFIRMATION_REQUIRED,
                ActionLifecycleStage.CONFIRMATION_GRANTED,
                ActionLifecycleStage.EXECUTION_STARTED,
                ActionLifecycleStage.EXECUTION_SUCCEEDED,
            ),
            audits.map { it.lifecycleStage },
        )
        assertTrue(audits.all { it.policyDecision == PolicyDecisionType.REQUIRE_CONFIRMATION })
        assertEquals(ExecutionResultCategory.EXECUTED, audits.last().executionResultCategory)
    }

    @Test
    fun approvedTokenIsSingleUseAndCannotReplayExecution() = runBlocking {
        var executionCount = 0
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                executionCount += 1
                "searched"
            },
            auditRecorder = ActionAuditRecorder { },
        )

        val pending = dispatcher.dispatchForAgent(
            ToolCall(id = "search", name = "search_web", args = mapOf("query" to "synthetic query"))
        ) as ActionDispatchResult.PendingConfirmation
        val first = dispatcher.resolveApproval(pending.pending.prompt, approved = true)
        val replay = dispatcher.resolveApproval(pending.pending.prompt, approved = true)

        assertEquals("searched", first.result)
        assertEquals(1, executionCount)
        assertEquals("Action was not executed because confirmation was invalid.", replay.result)
    }

    @Test
    fun expiredTokenIsRejectedAndAudited() = runBlocking {
        var executed = false
        val audits = mutableListOf<ActionAuditEvent>()
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                executed = true
                "searched"
            },
            auditRecorder = ActionAuditRecorder { audits += it },
        )

        val pending = dispatcher.dispatchForAgent(
            ToolCall(id = "search", name = "search_web", args = mapOf("query" to "synthetic query"))
        ) as ActionDispatchResult.PendingConfirmation
        val result = dispatcher.expireApproval(pending.pending.prompt)

        assertFalse(executed)
        assertEquals("Action confirmation expired before approval.", result.result)
        assertEquals(
            listOf(ActionLifecycleStage.CONFIRMATION_REQUIRED, ActionLifecycleStage.CONFIRMATION_EXPIRED),
            audits.map { it.lifecycleStage },
        )
    }

    @Test
    fun preExecutionAuditFailureStillPreventsExecutionAfterApproval() = runBlocking {
        var executed = false
        var auditCount = 0
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                executed = true
                "searched"
            },
            auditRecorder = ActionAuditRecorder {
                auditCount += 1
                if (auditCount == 3) error("private audit failure")
            },
        )

        val pending = dispatcher.dispatchForAgent(
            ToolCall(id = "search", name = "search_web", args = mapOf("query" to "synthetic query"))
        ) as ActionDispatchResult.PendingConfirmation
        val result = dispatcher.resolveApproval(pending.pending.prompt, approved = true)

        assertFalse(executed)
        assertEquals(3, auditCount)
        assertEquals(
            "Action was not executed because the local security audit could not be recorded.",
            result.result,
        )
    }

    @Test
    fun deniedActionsDoNotCallExecutorsAndAreAudited() = runBlocking {
        var executed = false
        val audits = mutableListOf<ActionAuditEvent>()
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                executed = true
                "executed"
            },
            auditRecorder = ActionAuditRecorder { audits += it },
        )

        val result = dispatcher.dispatch(
            ToolCall(id = "unknown", name = "do_anything", args = mapOf("payload" to "private value"))
        )

        assertFalse(executed)
        assertTrue(result.result.contains("Action blocked by policy."))
        assertEquals(PolicyDecisionType.DENY, audits.single().policyDecision)
        assertEquals(ActionLifecycleStage.DENIED, audits.single().lifecycleStage)
        assertFalse(audits.single().sanitizedSummary.contains("private value"))
    }

    @Test
    fun allowedActionsCallExecutorsOnceAndRecordLifecycleEvents() = runBlocking {
        var executionCount = 0
        val audits = mutableListOf<ActionAuditEvent>()
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                executionCount += 1
                "opened"
            },
            auditRecorder = ActionAuditRecorder { audits += it },
        )

        val result = dispatcher.dispatch(
            ToolCall(id = "open", name = "open_app", args = mapOf("app_name" to "Clock"))
        )

        assertEquals(1, executionCount)
        assertEquals("opened", result.result)
        assertEquals(2, audits.size)
        assertTrue(audits.map { it.eventId }.toSet().size == 2)
        assertTrue(audits.all { it.actionId == audits.first().actionId })
        assertEquals(ActionLifecycleStage.EXECUTION_STARTED, audits[0].lifecycleStage)
        assertEquals(ActionLifecycleStage.EXECUTION_SUCCEEDED, audits[1].lifecycleStage)
        assertEquals(PolicyDecisionType.ALLOW, audits[1].policyDecision)
        assertEquals(ExecutionResultCategory.EXECUTED, audits[1].executionResultCategory)
        assertEquals(SafeFailureCode.NONE, audits[1].safeFailureCode)
    }

    @Test
    fun executorFailurePersistsSafeFailureCodeWithoutRawMessage() = runBlocking {
        val audits = mutableListOf<ActionAuditEvent>()
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                throw IllegalArgumentException("private email nico@example.com")
            },
            auditRecorder = ActionAuditRecorder { audits += it },
        )

        val result = dispatcher.dispatch(
            ToolCall(id = "open", name = "open_app", args = mapOf("app_name" to "Clock"))
        )

        assertFalse(result.result.contains("nico@example.com"))
        assertEquals(ActionLifecycleStage.EXECUTION_FAILED, audits.last().lifecycleStage)
        assertEquals(SafeFailureCode.VALIDATION_FAILED, audits.last().safeFailureCode)
        assertFalse(audits.last().sanitizedSummary.contains("nico@example.com"))
    }

    @Test
    fun finalAuditFailureDoesNotRetryExecutor() = runBlocking {
        var executionCount = 0
        var auditCount = 0
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                executionCount += 1
                "opened"
            },
            auditRecorder = ActionAuditRecorder {
                auditCount += 1
                if (auditCount == 2) error("raw sqlite failure with /private/path")
            },
        )

        val result = dispatcher.dispatch(
            ToolCall(id = "open", name = "open_app", args = mapOf("app_name" to "Clock"))
        )

        assertEquals(1, executionCount)
        assertEquals(2, auditCount)
        assertTrue(result.result.contains("may have completed"))
        assertFalse(result.result.contains("/private/path"))
    }
}
