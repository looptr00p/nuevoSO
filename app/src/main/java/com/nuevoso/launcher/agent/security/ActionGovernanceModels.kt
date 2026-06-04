package com.nuevoso.launcher.agent.security

import java.util.UUID

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
    NOT_EXECUTED,
    EXECUTED,
    BLOCKED_BY_POLICY,
    FAILED,
    PARTIAL_AUDIT_FAILURE,
}

enum class ActionLifecycleStage {
    REQUESTED,
    POLICY_ALLOWED,
    CONFIRMATION_REQUIRED,
    CONFIRMATION_GRANTED,
    CONFIRMATION_REJECTED,
    CONFIRMATION_EXPIRED,
    DENIED,
    EXECUTION_STARTED,
    EXECUTION_SUCCEEDED,
    EXECUTION_FAILED,
    AUDIT_FINALIZATION_FAILED,
    LEGACY_RECORDED,
}

enum class SafeFailureCode {
    NONE,
    AUDIT_PERSISTENCE_FAILED,
    EXECUTOR_UNAVAILABLE,
    PERMISSION_DENIED,
    VALIDATION_FAILED,
    NETWORK_ERROR,
    DATABASE_ERROR,
    UNKNOWN_FAILURE,
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
    val eventId: String = UUID.randomUUID().toString(),
    val actionId: String,
    val timestampMillis: Long,
    val toolName: String,
    val riskLevel: ActionRiskLevel,
    val policyDecision: PolicyDecisionType,
    val lifecycleStage: ActionLifecycleStage,
    val sanitizedSummary: String,
    val executionResultCategory: ExecutionResultCategory,
    val safeFailureCode: SafeFailureCode = SafeFailureCode.NONE,
)
