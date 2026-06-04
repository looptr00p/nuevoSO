package com.nuevoso.launcher.agent.security

import com.nuevoso.launcher.ai.ToolCall
import java.security.MessageDigest
import java.util.UUID

const val APPROVAL_EXPIRY_MILLIS: Long = 120_000

data class ApprovalPrompt(
    val token: String,
    val actionId: String,
    val argumentsHash: String,
    val toolName: String,
    val riskLevel: ActionRiskLevel,
    val sanitizedSummary: String,
    val issuedAtMillis: Long,
    val expiresAtMillis: Long,
)

data class PendingActionApproval(
    val prompt: ApprovalPrompt,
    val call: ToolCall,
    val request: ActionRequest,
    val decision: PolicyDecision.RequireConfirmation,
)

enum class ApprovalUserDecision {
    APPROVE,
    REJECT,
}

sealed class ApprovalConsumeResult {
    data class Approved(val pending: PendingActionApproval) : ApprovalConsumeResult()
    data class Rejected(val pending: PendingActionApproval) : ApprovalConsumeResult()
    data class Expired(val pending: PendingActionApproval) : ApprovalConsumeResult()
    data class Failed(val failure: ApprovalFailure) : ApprovalConsumeResult()
}

enum class ApprovalFailure {
    UNKNOWN_TOKEN,
    REPLAYED_TOKEN,
    ACTION_MISMATCH,
    ARGUMENT_MISMATCH,
}

interface ApprovalStore {
    fun issue(
        call: ToolCall,
        request: ActionRequest,
        decision: PolicyDecision.RequireConfirmation,
        issuedAtMillis: Long = System.currentTimeMillis(),
    ): PendingActionApproval

    fun consume(
        token: String,
        decision: ApprovalUserDecision,
        expectedActionId: String,
        expectedArgumentsHash: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): ApprovalConsumeResult

    fun expire(
        token: String,
        expectedActionId: String,
        expectedArgumentsHash: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): ApprovalConsumeResult
}

class InMemoryApprovalStore(
    private val expiryMillis: Long = APPROVAL_EXPIRY_MILLIS,
    private val tokenGenerator: () -> String = { UUID.randomUUID().toString() },
) : ApprovalStore {
    private val approvals = mutableMapOf<String, StoredApproval>()

    @Synchronized
    override fun issue(
        call: ToolCall,
        request: ActionRequest,
        decision: PolicyDecision.RequireConfirmation,
        issuedAtMillis: Long,
    ): PendingActionApproval {
        val token = tokenGenerator()
        val argumentsHash = ApprovalHasher.hash(request)
        val prompt = ApprovalPrompt(
            token = token,
            actionId = request.actionId,
            argumentsHash = argumentsHash,
            toolName = request.toolName,
            riskLevel = request.riskLevel,
            sanitizedSummary = ArgumentSanitizer.summarize(request.sanitizedArguments),
            issuedAtMillis = issuedAtMillis,
            expiresAtMillis = issuedAtMillis + expiryMillis,
        )
        val pending = PendingActionApproval(
            prompt = prompt,
            call = call,
            request = request,
            decision = decision,
        )
        approvals[token] = StoredApproval(pending = pending)
        return pending
    }

    @Synchronized
    override fun consume(
        token: String,
        decision: ApprovalUserDecision,
        expectedActionId: String,
        expectedArgumentsHash: String,
        nowMillis: Long,
    ): ApprovalConsumeResult {
        val stored = approvals[token] ?: return ApprovalConsumeResult.Failed(ApprovalFailure.UNKNOWN_TOKEN)
        if (stored.consumed) return ApprovalConsumeResult.Failed(ApprovalFailure.REPLAYED_TOKEN)

        val pending = stored.pending
        val validation = validate(stored, expectedActionId, expectedArgumentsHash, nowMillis)
        if (validation != null) return validation

        stored.consumed = true
        return when (decision) {
            ApprovalUserDecision.APPROVE -> ApprovalConsumeResult.Approved(pending)
            ApprovalUserDecision.REJECT -> ApprovalConsumeResult.Rejected(pending)
        }
    }

    @Synchronized
    override fun expire(
        token: String,
        expectedActionId: String,
        expectedArgumentsHash: String,
        nowMillis: Long,
    ): ApprovalConsumeResult {
        val stored = approvals[token] ?: return ApprovalConsumeResult.Failed(ApprovalFailure.UNKNOWN_TOKEN)
        if (stored.consumed) return ApprovalConsumeResult.Failed(ApprovalFailure.REPLAYED_TOKEN)

        val validation = validate(stored, expectedActionId, expectedArgumentsHash, nowMillis)
        return if (validation is ApprovalConsumeResult.Expired) {
            validation
        } else {
            stored.consumed = true
            ApprovalConsumeResult.Rejected(stored.pending)
        }
    }

    private fun validate(
        stored: StoredApproval,
        expectedActionId: String,
        expectedArgumentsHash: String,
        nowMillis: Long,
    ): ApprovalConsumeResult? {
        val pending = stored.pending
        if (pending.prompt.actionId != expectedActionId) {
            stored.consumed = true
            return ApprovalConsumeResult.Failed(ApprovalFailure.ACTION_MISMATCH)
        }
        if (pending.prompt.argumentsHash != expectedArgumentsHash) {
            stored.consumed = true
            return ApprovalConsumeResult.Failed(ApprovalFailure.ARGUMENT_MISMATCH)
        }
        if (ApprovalHasher.hash(pending.request) != pending.prompt.argumentsHash) {
            stored.consumed = true
            return ApprovalConsumeResult.Failed(ApprovalFailure.ARGUMENT_MISMATCH)
        }
        if (nowMillis > pending.prompt.expiresAtMillis) {
            stored.consumed = true
            return ApprovalConsumeResult.Expired(pending)
        }
        return null
    }

    private data class StoredApproval(
        val pending: PendingActionApproval,
        var consumed: Boolean = false,
    )
}

object ApprovalHasher {
    fun hash(request: ActionRequest): String {
        val canonical = buildString {
            append("tool=")
            append(request.toolName)
            append('\n')
            request.sanitizedArguments.toSortedMap().forEach { (key, value) ->
                append(key.length)
                append(':')
                append(key)
                append('=')
                append(value.length)
                append(':')
                append(value)
                append('\n')
            }
        }
        val bytes = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
