package com.nuevoso.launcher.agent.security

import com.nuevoso.launcher.ai.ToolCall
import java.util.concurrent.atomic.AtomicLong

class ActionRequestFactory {
    fun from(call: ToolCall, timestampMillis: Long = System.currentTimeMillis()): ActionRequest {
        val sanitizedArgs = ArgumentSanitizer.sanitize(call.name, call.args)
        val policy = ActionPolicyRegistry.classify(call.name, sanitizedArgs)
        return ActionRequest(
            actionId = buildActionId(call, timestampMillis),
            toolName = call.name,
            sanitizedArguments = sanitizedArgs,
            timestampMillis = timestampMillis,
            riskLevel = policy.riskLevel,
            explanation = policy.reason,
        )
    }

    private fun buildActionId(call: ToolCall, timestampMillis: Long): String {
        val base = call.id.ifBlank { call.name }
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
            .take(48)
            .ifBlank { "action" }
        return "$base-$timestampMillis-${sequence.incrementAndGet()}"
    }

    companion object {
        private val sequence = AtomicLong(0)
    }
}
