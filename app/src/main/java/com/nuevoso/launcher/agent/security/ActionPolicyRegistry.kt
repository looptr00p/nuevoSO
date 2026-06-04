package com.nuevoso.launcher.agent.security

object ActionPolicyRegistry {
    fun classify(toolName: String, sanitizedArgs: Map<String, String>): ActionPolicy {
        if (hasMalformedArguments(toolName, sanitizedArgs)) {
            return policy(toolName, ActionRiskLevel.R4_BLOCKED, "Malformed or unsupported tool arguments fail closed.")
        }
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
        val value = sanitizedArgs["value"]?.lowercase()
        return when {
            setting == "flashlight" && value in setOf("on", "off") ->
                policy(toolName, ActionRiskLevel.R1_REVERSIBLE, "Flashlight changes are explicit, reversible, and locally bounded.")
            setting == "flashlight" ->
                policy(toolName, ActionRiskLevel.R4_BLOCKED, "Flashlight control requires explicit on or off.")
            setting in setOf("wifi", "bluetooth", "data", "brightness", "sound", "airplane") ->
                policy(toolName, ActionRiskLevel.R2_SENSITIVE, "Settings changes can affect persistent device state.")
            else ->
                policy(toolName, ActionRiskLevel.R4_BLOCKED, "Unknown settings fail closed.")
        }
    }

    private fun hasMalformedArguments(toolName: String, sanitizedArgs: Map<String, String>): Boolean {
        val allowedKeys = when (toolName) {
            "open_app", "install_app" -> setOf("app_name")
            "press_back", "list_apps", "read_screen" -> emptySet()
            "scroll_screen" -> setOf("direction")
            "toggle_setting" -> setOf("setting", "value")
            "search_web" -> setOf("query")
            "set_alarm" -> setOf("hour", "minute", "label")
            "call" -> setOf("target")
            "remember_fact" -> setOf("fact")
            "tap_element" -> setOf("description")
            "type_text" -> setOf("text")
            else -> return false
        }
        if (!allowedKeys.containsAll(sanitizedArgs.keys)) return true
        if (sanitizedArgs.values.any { it.startsWith("[REDACTED") || it.startsWith("[INVALID") }) return true
        return when (toolName) {
            "open_app", "install_app" -> sanitizedArgs["app_name"].isNullOrBlank()
            "scroll_screen" -> sanitizedArgs["direction"] !in setOf("down", "up")
            "set_alarm" -> sanitizedArgs["hour"].isNullOrBlank() || sanitizedArgs["minute"].isNullOrBlank()
            "call" -> sanitizedArgs["target"].isNullOrBlank()
            "remember_fact" -> sanitizedArgs["fact"].isNullOrBlank()
            "tap_element" -> sanitizedArgs["description"].isNullOrBlank()
            "type_text" -> sanitizedArgs["text"].isNullOrBlank()
            "search_web" -> sanitizedArgs["query"].isNullOrBlank()
            else -> false
        }
    }

    private fun policy(toolName: String, riskLevel: ActionRiskLevel, reason: String) =
        ActionPolicy(toolName = toolName, riskLevel = riskLevel, reason = reason)
}
