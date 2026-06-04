package com.nuevoso.launcher.agent.security

import com.nuevoso.launcher.agent.ALL_TOOLS
import com.nuevoso.launcher.ai.ToolCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyEngineTest {
    private val factory = ActionRequestFactory()
    private val engine = PolicyEngine()

    @Test
    fun unknownToolsAreDenied() {
        val decision = evaluate(ToolCall(id = "1", name = "do_anything", args = emptyMap()))

        assertEquals(PolicyDecisionType.DENY, decision.type)
        assertTrue(decision.reason.isNotBlank())
    }

    @Test
    fun reversibleToolsCanProceed() {
        val decision = evaluate(ToolCall(id = "1", name = "open_app", args = mapOf("app_name" to "Clock")))

        assertEquals(PolicyDecisionType.ALLOW, decision.type)
        assertTrue(decision.reason.isNotBlank())
    }

    @Test
    fun sensitiveToolsRequireConfirmation() {
        val decision = evaluate(ToolCall(id = "1", name = "search_web", args = mapOf("query" to "private search")))

        assertEquals(PolicyDecisionType.REQUIRE_CONFIRMATION, decision.type)
        assertTrue(decision.reason.isNotBlank())
    }

    @Test
    fun flashlightRequiresExplicitDeclarativeValue() {
        val missing = evaluate(ToolCall(id = "1", name = "toggle_setting", args = mapOf("setting" to "flashlight")))
        val malformed = evaluate(
            ToolCall(id = "1", name = "toggle_setting", args = mapOf("setting" to "flashlight", "value" to "toggle"))
        )
        val explicit = evaluate(
            ToolCall(id = "1", name = "toggle_setting", args = mapOf("setting" to "flashlight", "value" to "off"))
        )

        assertEquals(PolicyDecisionType.DENY, missing.type)
        assertEquals(PolicyDecisionType.DENY, malformed.type)
        assertEquals(PolicyDecisionType.ALLOW, explicit.type)
    }

    @Test
    fun genericAccessibilityTapsRequireConfirmation() {
        val decision = evaluate(ToolCall(id = "1", name = "tap_element", args = mapOf("description" to "Buy now")))

        assertEquals(PolicyDecisionType.REQUIRE_CONFIRMATION, decision.type)
        assertTrue(decision.reason.isNotBlank())
    }

    @Test
    fun genericTextInputRequiresConfirmation() {
        val decision = evaluate(ToolCall(id = "1", name = "type_text", args = mapOf("text" to "send this")))

        assertEquals(PolicyDecisionType.REQUIRE_CONFIRMATION, decision.type)
        assertTrue(decision.reason.isNotBlank())
    }

    @Test
    fun allDeclaredToolsHaveKnownClassifications() {
        val blockedTools = ALL_TOOLS.map { tool ->
            factory.from(ToolCall(id = tool.name, name = tool.name, args = sampleArgs(tool.name)))
        }.filter { it.riskLevel == ActionRiskLevel.R4_BLOCKED }

        assertTrue("Unclassified tools: ${blockedTools.map { it.toolName }}", blockedTools.isEmpty())
    }

    private fun evaluate(call: ToolCall): PolicyDecision = engine.evaluate(factory.from(call))

    private fun sampleArgs(toolName: String): Map<String, String> {
        return when (toolName) {
            "open_app" -> mapOf("app_name" to "Clock")
            "search_web" -> mapOf("query" to "weather")
            "set_alarm" -> mapOf("hour" to "7", "minute" to "30")
            "call" -> mapOf("target" to "+100000000")
            "toggle_setting" -> mapOf("setting" to "flashlight", "value" to "on")
            "install_app" -> mapOf("app_name" to "Maps")
            "tap_element" -> mapOf("description" to "Continue")
            "type_text" -> mapOf("text" to "hello")
            "scroll_screen" -> mapOf("direction" to "down")
            "remember_fact" -> mapOf("fact" to "User likes concise answers.")
            else -> emptyMap()
        }
    }
}
