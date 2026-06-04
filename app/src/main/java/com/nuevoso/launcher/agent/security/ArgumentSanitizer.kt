package com.nuevoso.launcher.agent.security

object ArgumentSanitizer {
    private const val MAX_VALUE_LENGTH = 80
    private const val MAX_SUMMARY_LENGTH = 500
    private val settings = setOf("wifi", "bluetooth", "data", "brightness", "sound", "airplane", "flashlight")
    private val binaryValues = setOf("on", "off")
    private val scrollDirections = setOf("down", "up")
    private val relativeDays = setOf("today", "tomorrow")

    fun sanitize(toolName: String, args: Map<String, String>): Map<String, String> {
        if (args.isEmpty()) return emptyMap()
        val normalizedArgs = args.entries.associate { (key, value) -> key.trim().lowercase() to value }
        return normalizedArgs.mapValues { (key, value) ->
            when (toolName) {
                "open_app", "install_app" -> when (key) {
                    "app_name" -> allowString(value)
                    else -> redacted(value)
                }
                "press_back", "list_apps", "read_screen" -> redacted(value)
                "scroll_screen" -> when (key) {
                    "direction" -> allowEnum(value, scrollDirections)
                    else -> redacted(value)
                }
                "toggle_setting" -> when (key) {
                    "setting" -> allowEnum(value, settings)
                    "value" -> allowEnum(value, binaryValues)
                    else -> redacted(value)
                }
                "search_web" -> when (key) {
                    "query" -> metadataOnly("QUERY_REDACTED", value)
                    else -> redacted(value)
                }
                "set_alarm" -> when (key) {
                    "hour" -> allowHour(value)
                    "minute" -> allowMinute(value)
                    "delay_minutes" -> allowDelayMinutes(value)
                    "label" -> metadataOnly("LABEL_REDACTED", value)
                    else -> redacted(value)
                }
                "create_calendar_event" -> when (key) {
                    "title" -> requiredMetadataOnly("TITLE_REDACTED", value)
                    "day" -> allowEnum(value, relativeDays)
                    "date" -> allowDate(value)
                    "start_hour", "end_hour" -> allowHour(value)
                    "start_minute", "end_minute" -> allowMinute(value)
                    "location" -> metadataOnly("LOCATION_REDACTED", value)
                    "description" -> metadataOnly("DESCRIPTION_REDACTED", value)
                    else -> redacted(value)
                }
                "call" -> when (key) {
                    "target" -> metadataOnly("TARGET_REDACTED", value)
                    else -> redacted(value)
                }
                "remember_fact" -> when (key) {
                    "fact" -> metadataOnly("FACT_REDACTED", value)
                    else -> redacted(value)
                }
                "tap_element" -> when (key) {
                    "description" -> metadataOnly("DESCRIPTION_REDACTED", value)
                    else -> redacted(value)
                }
                "type_text" -> when (key) {
                    "text" -> metadataOnly("TEXT_REDACTED", value)
                    else -> redacted(value)
                }
                else -> redacted(value)
            }
        }
    }

    fun summarize(args: Map<String, String>): String {
        if (args.isEmpty()) return "no_arguments"
        return args.entries
            .sortedBy { it.key }
            .joinToString(", ") { (key, value) -> "${key.take(MAX_VALUE_LENGTH)}=$value" }
            .take(MAX_SUMMARY_LENGTH)
    }

    private fun redacted(value: String): String = "[REDACTED length=${value.length}]"

    private fun metadataOnly(label: String, value: String): String = "[$label length=${value.length}]"

    private fun requiredMetadataOnly(label: String, value: String): String {
        return if (value.isBlank()) "[INVALID_EMPTY]" else metadataOnly(label, value)
    }

    private fun allowString(value: String): String {
        return value.trim()
            .filterNot { it.isISOControl() }
            .take(MAX_VALUE_LENGTH)
            .ifBlank { "[INVALID_EMPTY]" }
    }

    private fun allowEnum(value: String, allowedValues: Set<String>): String {
        val normalized = value.trim().lowercase()
        return if (normalized in allowedValues) normalized else redacted(value)
    }

    private fun allowHour(value: String): String {
        val hour = value.trim().toIntOrNull()
        return if (hour != null && hour in 0..23) hour.toString() else "[INVALID_HOUR]"
    }

    private fun allowMinute(value: String): String {
        val minute = value.trim().toIntOrNull()
        return if (minute != null && minute in 0..59) minute.toString() else "[INVALID_MINUTE]"
    }

    private fun allowDelayMinutes(value: String): String {
        val delay = value.trim().toIntOrNull()
        return if (delay != null && delay in 1..1440) delay.toString() else "[INVALID_DELAY_MINUTES]"
    }

    private fun allowDate(value: String): String {
        val normalized = value.trim()
        return if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(normalized)) normalized else "[INVALID_DATE]"
    }
}
