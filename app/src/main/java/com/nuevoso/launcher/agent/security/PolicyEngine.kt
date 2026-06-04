package com.nuevoso.launcher.agent.security

class PolicyEngine {
    fun evaluate(request: ActionRequest): PolicyDecision {
        val policy = ActionPolicyRegistry.classify(request.toolName, request.sanitizedArguments)
        val reason = policy.reason.ifBlank { "Policy decision recorded." }
        return when (policy.riskLevel) {
            ActionRiskLevel.R0_READ_ONLY,
            ActionRiskLevel.R1_REVERSIBLE -> PolicyDecision.Allow(reason)
            ActionRiskLevel.R2_SENSITIVE,
            ActionRiskLevel.R3_DESTRUCTIVE_OR_EXTERNAL ->
                PolicyDecision.RequireConfirmation("$reason Explicit user confirmation is required.")
            ActionRiskLevel.R4_BLOCKED -> PolicyDecision.Deny(reason)
        }
    }
}
