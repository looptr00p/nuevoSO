package com.nuevoso.launcher.agent.security

enum class ActionRiskLevel {
    R0_READ_ONLY,
    R1_REVERSIBLE,
    R2_SENSITIVE,
    R3_DESTRUCTIVE_OR_EXTERNAL,
    R4_BLOCKED,
}

enum class PolicyDecisionType {
    ALLOW,
    REQUIRE_CONFIRMATION,
    DENY,
}

enum class ExecutionResultCategory {
    EXECUTED,
    BLOCKED_BY_POLICY,
    FAILED,
}

data class ActionPolicy(
    val toolName: String,
    val riskLevel: ActionRiskLevel,
    val reason: String,
)

data class ActionRequest(
    val actionId: String,
    val toolName: String,
    val sanitizedArguments: Map<String, String>,
    val timestampMillis: Long,
    val riskLevel: ActionRiskLevel,
    val explanation: String? = null,
)

sealed class PolicyDecision(
    val type: PolicyDecisionType,
    val reason: String,
) {
    class Allow(reason: String) : PolicyDecision(PolicyDecisionType.ALLOW, reason)
    class RequireConfirmation(reason: String) :
        PolicyDecision(PolicyDecisionType.REQUIRE_CONFIRMATION, reason)
    class Deny(reason: String) : PolicyDecision(PolicyDecisionType.DENY, reason)
}

data class ActionAuditEvent(
    val actionId: String,
    val timestampMillis: Long,
    val toolName: String,
    val riskLevel: ActionRiskLevel,
    val policyDecision: PolicyDecisionType,
    val sanitizedSummary: String,
    val executionResultCategory: ExecutionResultCategory,
    val failureReason: String? = null,
)
