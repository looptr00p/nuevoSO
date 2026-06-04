package com.nuevoso.launcher.agent.security

object ActionPolicyRegistry {
    fun classify(toolName: String, sanitizedArgs: Map<String, String>): ActionPolicy {
        return when (toolName) {
            "open_app" -> policy(toolName, ActionRiskLevel.R1_REVERSIBLE, "Opening an installed app is reversible.")
            "press_back" -> policy(toolName, ActionRiskLevel.R1_REVERSIBLE, "System back navigation is reversible.")
            "scroll_screen" -> policy(toolName, ActionRiskLevel.R1_REVERSIBLE, "Scrolling only changes the visible viewport.")
            "toggle_setting" -> classifyToggleSetting(toolName, sanitizedArgs)
            "search_web" -> policy(toolName, ActionRiskLevel.R2_SENSITIVE, "Web search sends a query to an external service.")
            "set_alarm" -> policy(toolName, ActionRiskLevel.R2_SENSITIVE, "Setting alarms changes persistent user state.")
            "call" -> policy(toolName, ActionRiskLevel.R2_SENSITIVE, "Calling opens a sensitive communication flow.")
            "remember_fact" -> policy(toolName, ActionRiskLevel.R2_SENSITIVE, "Remembering facts persists user data locally.")
            "install_app" -> policy(toolName, ActionRiskLevel.R2_SENSITIVE, "Installing apps changes device state and may involve Play Store.")
            "list_apps" -> policy(toolName, ActionRiskLevel.R2_SENSITIVE, "Installed app lists can reveal sensitive user context to a remote model.")
            "read_screen" -> policy(toolName, ActionRiskLevel.R2_SENSITIVE, "Screen contents may contain private third-party data.")
            "tap_element" -> policy(toolName, ActionRiskLevel.R3_DESTRUCTIVE_OR_EXTERNAL, "Generic taps can submit, purchase, delete, publish, or accept in third-party apps.")
            "type_text" -> policy(toolName, ActionRiskLevel.R3_DESTRUCTIVE_OR_EXTERNAL, "Generic text input can fill private forms or send messages.")
            else -> policy(toolName, ActionRiskLevel.R4_BLOCKED, "Unknown tools fail closed.")
        }
    }

    private fun classifyToggleSetting(toolName: String, sanitizedArgs: Map<String, String>): ActionPolicy {
        val setting = sanitizedArgs["setting"]?.lowercase()
        return if (setting == "flashlight") {
            policy(toolName, ActionRiskLevel.R1_REVERSIBLE, "Flashlight changes are reversible and locally bounded.")
        } else {
            policy(toolName, ActionRiskLevel.R2_SENSITIVE, "Settings changes can affect persistent device state.")
        }
    }

    private fun policy(toolName: String, riskLevel: ActionRiskLevel, reason: String) =
        ActionPolicy(toolName = toolName, riskLevel = riskLevel, reason = reason)
}
