package com.nuevoso.launcher.agent.security

object ArgumentSanitizer {
    private val sensitiveKeys = setOf(
        "api_key",
        "apikey",
        "authorization",
        "password",
        "token",
        "secret",
        "text",
        "fact",
        "message",
        "target",
        "phone",
        "number",
        "url",
    )

    fun sanitize(toolName: String, args: Map<String, String>): Map<String, String> {
        if (args.isEmpty()) return emptyMap()
        return args.mapValues { (key, value) ->
            when {
                key.lowercase() in sensitiveKeys -> redacted(value)
                toolName == "search_web" && key == "query" -> metadataOnly(value)
                value.startsWith("http://") || value.startsWith("https://") -> redacted(value)
                else -> value.take(120)
            }
        }
    }

    fun summarize(args: Map<String, String>): String {
        if (args.isEmpty()) return "no_arguments"
        return args.entries.joinToString(", ") { (key, value) -> "$key=$value" }.take(500)
    }

    private fun redacted(value: String): String = "[REDACTED length=${value.length}]"

    private fun metadataOnly(value: String): String = "[QUERY_REDACTED length=${value.length}]"
}
