package com.nuevoso.launcher.agent.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArgumentSanitizerTest {
    @Test
    fun sanitizerRedactsSensitiveTypedTextAndFacts() {
        val typed = ArgumentSanitizer.sanitize("type_text", mapOf("text" to "my private message"))
        val fact = ArgumentSanitizer.sanitize("remember_fact", mapOf("fact" to "I live at a private address"))

        assertFalse(typed.getValue("text").contains("private message"))
        assertFalse(fact.getValue("fact").contains("private address"))
        assertTrue(typed.getValue("text").startsWith("[REDACTED"))
        assertTrue(fact.getValue("fact").startsWith("[REDACTED"))
    }

    @Test
    fun sanitizerStoresSearchQueryMetadataOnly() {
        val sanitized = ArgumentSanitizer.sanitize("search_web", mapOf("query" to "medical query"))
        val summary = ArgumentSanitizer.summarize(sanitized)

        assertFalse(summary.contains("medical query"))
        assertTrue(summary.contains("QUERY_REDACTED"))
    }
}
