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
        assertTrue(typed.getValue("text").startsWith("[TEXT_REDACTED"))
        assertTrue(fact.getValue("fact").startsWith("[FACT_REDACTED"))
    }

    @Test
    fun sanitizerStoresSearchQueryMetadataOnly() {
        val sanitized = ArgumentSanitizer.sanitize("search_web", mapOf("query" to "medical query"))
        val summary = ArgumentSanitizer.summarize(sanitized)

        assertFalse(summary.contains("medical query"))
        assertTrue(summary.contains("QUERY_REDACTED"))
    }

    @Test
    fun sanitizerRedactsUnknownArgumentsByDefault() {
        val rawValues = listOf(
            "raw payload body",
            "nico@example.com",
            "742 Evergreen Terrace",
            "Private Contact",
            "secret-token-123",
            "private-value-456",
        )
        val sanitized = ArgumentSanitizer.sanitize(
            "unknown_tool",
            mapOf(
                "payload" to rawValues[0],
                "email" to rawValues[1],
                "address" to rawValues[2],
                "contact_name" to rawValues[3],
                "access_token" to rawValues[4],
                "private_value" to rawValues[5],
            ),
        )
        val summary = ArgumentSanitizer.summarize(sanitized)

        rawValues.forEach { raw -> assertFalse(summary.contains(raw)) }
        assertTrue(summary.contains("payload=[REDACTED length=16]"))
        assertTrue(summary.contains("access_token=[REDACTED length=16]"))
    }

    @Test
    fun sanitizerAllowsOnlyExplicitFlashlightValues() {
        val valid = ArgumentSanitizer.sanitize(
            "toggle_setting",
            mapOf("SETTING" to "Flashlight", "VALUE" to "ON"),
        )
        val invalid = ArgumentSanitizer.sanitize(
            "toggle_setting",
            mapOf("setting" to "flashlight", "value" to "toggle"),
        )

        assertTrue(valid["setting"] == "flashlight")
        assertTrue(valid["value"] == "on")
        assertTrue(invalid.getValue("value").startsWith("[REDACTED"))
    }

    @Test
    fun sanitizerAllowsBoundedRelativeAlarmMinutes() {
        val valid = ArgumentSanitizer.sanitize("set_alarm", mapOf("delay_minutes" to "3"))
        val invalid = ArgumentSanitizer.sanitize("set_alarm", mapOf("delay_minutes" to "0"))

        assertTrue(valid["delay_minutes"] == "3")
        assertTrue(invalid["delay_minutes"] == "[INVALID_DELAY_MINUTES]")
    }

    @Test
    fun sanitizerRedactsCalendarEventPrivateDetails() {
        val sanitized = ArgumentSanitizer.sanitize(
            "create_calendar_event",
            mapOf(
                "title" to "Cine con persona privada",
                "day" to "tomorrow",
                "start_hour" to "18",
                "start_minute" to "0",
                "end_hour" to "21",
                "end_minute" to "0",
                "location" to "Dirección privada",
                "description" to "Detalle privado",
            ),
        )
        val summary = ArgumentSanitizer.summarize(sanitized)

        assertTrue(sanitized["day"] == "tomorrow")
        assertTrue(sanitized["start_hour"] == "18")
        assertTrue(summary.contains("TITLE_REDACTED"))
        assertTrue(summary.contains("LOCATION_REDACTED"))
        assertTrue(summary.contains("DESCRIPTION_REDACTED"))
        assertFalse(summary.contains("persona privada"))
        assertFalse(summary.contains("Dirección privada"))
        assertFalse(summary.contains("Detalle privado"))
    }
}
