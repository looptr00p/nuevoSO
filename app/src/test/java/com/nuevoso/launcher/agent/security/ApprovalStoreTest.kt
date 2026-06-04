package com.nuevoso.launcher.agent.security

import com.nuevoso.launcher.ai.ToolCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalStoreTest {
    private val factory = ActionRequestFactory()

    @Test
    fun tokenIsBoundToActionId() {
        val store = InMemoryApprovalStore(tokenGenerator = { "token-1" })
        val request = requestFor("search", "search_web", mapOf("query" to "synthetic query"))
        val pending = store.issue(
            call = ToolCall(id = "search", name = "search_web", args = mapOf("query" to "synthetic query")),
            request = request,
            decision = PolicyDecision.RequireConfirmation("confirmation required"),
            issuedAtMillis = 1_000,
        )

        val result = store.consume(
            token = pending.prompt.token,
            decision = ApprovalUserDecision.APPROVE,
            expectedActionId = "different-action",
            expectedArgumentsHash = pending.prompt.argumentsHash,
            nowMillis = 1_001,
        )

        assertEquals(ApprovalConsumeResult.Failed(ApprovalFailure.ACTION_MISMATCH), result)
    }

    @Test
    fun tokenIsBoundToSanitizedArgumentHash() {
        val store = InMemoryApprovalStore(tokenGenerator = { "token-1" })
        val request = requestFor("search", "search_web", mapOf("query" to "synthetic query"))
        val pending = store.issue(
            call = ToolCall(id = "search", name = "search_web", args = mapOf("query" to "synthetic query")),
            request = request,
            decision = PolicyDecision.RequireConfirmation("confirmation required"),
            issuedAtMillis = 1_000,
        )

        val result = store.consume(
            token = pending.prompt.token,
            decision = ApprovalUserDecision.APPROVE,
            expectedActionId = pending.prompt.actionId,
            expectedArgumentsHash = "different-hash",
            nowMillis = 1_001,
        )

        assertEquals(ApprovalConsumeResult.Failed(ApprovalFailure.ARGUMENT_MISMATCH), result)
    }

    @Test
    fun expiredTokenFailsClosedAndBecomesSingleUse() {
        val store = InMemoryApprovalStore(expiryMillis = 10, tokenGenerator = { "token-1" })
        val request = requestFor("search", "search_web", mapOf("query" to "synthetic query"))
        val pending = store.issue(
            call = ToolCall(id = "search", name = "search_web", args = mapOf("query" to "synthetic query")),
            request = request,
            decision = PolicyDecision.RequireConfirmation("confirmation required"),
            issuedAtMillis = 1_000,
        )

        val expired = store.consume(
            token = pending.prompt.token,
            decision = ApprovalUserDecision.APPROVE,
            expectedActionId = pending.prompt.actionId,
            expectedArgumentsHash = pending.prompt.argumentsHash,
            nowMillis = 1_011,
        )
        val replay = store.consume(
            token = pending.prompt.token,
            decision = ApprovalUserDecision.APPROVE,
            expectedActionId = pending.prompt.actionId,
            expectedArgumentsHash = pending.prompt.argumentsHash,
            nowMillis = 1_012,
        )

        assertTrue(expired is ApprovalConsumeResult.Expired)
        assertEquals(ApprovalConsumeResult.Failed(ApprovalFailure.REPLAYED_TOKEN), replay)
    }

    @Test
    fun unknownTokenFailsClosed() {
        val store = InMemoryApprovalStore(tokenGenerator = { "token-1" })

        val result = store.consume(
            token = "missing-token",
            decision = ApprovalUserDecision.APPROVE,
            expectedActionId = "action",
            expectedArgumentsHash = "hash",
            nowMillis = 1_000,
        )

        assertEquals(ApprovalConsumeResult.Failed(ApprovalFailure.UNKNOWN_TOKEN), result)
    }

    private fun requestFor(id: String, name: String, args: Map<String, String>): ActionRequest {
        return factory.from(ToolCall(id = id, name = name, args = args), timestampMillis = 1_000)
    }
}
