package com.nuevoso.launcher.agent

import com.nuevoso.launcher.agent.security.ActionAuditEvent
import com.nuevoso.launcher.agent.security.ExecutionResultCategory
import com.nuevoso.launcher.agent.security.PolicyDecisionType
import com.nuevoso.launcher.ai.ToolCall
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionDispatcherTest {
    @Test
    fun deniedActionsDoNotCallExecutors() = runBlocking {
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
        assertEquals(ExecutionResultCategory.BLOCKED_BY_POLICY, audits.single().executionResultCategory)
    }

    @Test
    fun allowedActionsCallExecutorsAndAreAudited() = runBlocking {
        var executed = false
        val audits = mutableListOf<ActionAuditEvent>()
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor {
                executed = true
                "opened"
            },
            auditRecorder = ActionAuditRecorder { audits += it },
        )

        val result = dispatcher.dispatch(
            ToolCall(id = "open", name = "open_app", args = mapOf("app_name" to "Clock"))
        )

        assertTrue(executed)
        assertEquals("opened", result.result)
        assertEquals(PolicyDecisionType.ALLOW, audits.single().policyDecision)
        assertEquals(ExecutionResultCategory.EXECUTED, audits.single().executionResultCategory)
    }
}
