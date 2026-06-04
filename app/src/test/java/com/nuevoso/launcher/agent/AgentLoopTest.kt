package com.nuevoso.launcher.agent

import com.nuevoso.launcher.ai.AiProvider
import com.nuevoso.launcher.ai.AiTurn
import com.nuevoso.launcher.ai.Msg
import com.nuevoso.launcher.ai.ToolCall
import com.nuevoso.launcher.ai.ToolSpec
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentLoopTest {
    @Test
    fun pendingConfirmationTokenDoesNotEnterModelTranscript() = runBlocking {
        val provider = ScriptedProvider(
            AiTurn(
                text = "I need approval.",
                toolCalls = listOf(
                    ToolCall(
                        id = "tap",
                        name = "tap_element",
                        args = mapOf("description" to "Submit synthetic form"),
                    )
                ),
            ),
            AiTurn(text = "I did not execute it."),
        )
        val dispatcher = ActionDispatcher(
            executor = ToolCallExecutor { "executed" },
            auditRecorder = ActionAuditRecorder { },
        )
        val loop = AgentLoop(provider, dispatcher)

        val pending = loop.run(systemPrompt = "system", history = listOf(Msg(role = "user", text = "submit"))) as
            AgentLoopResult.PendingConfirmation
        val token = pending.prompt.token
        val rejection = dispatcher.resolveApproval(pending.prompt, approved = false)
        val completed = loop.continueAfterConfirmation(pending.continuation, rejection)

        assertTrue(completed is AgentLoopResult.Completed)
        assertFalse(provider.histories.flattenForAssertion().contains(token))
    }

    private class ScriptedProvider(vararg turns: AiTurn) : AiProvider {
        private val turns = ArrayDeque(turns.toList())
        val histories = mutableListOf<List<Msg>>()

        override suspend fun chat(
            system: String,
            history: List<Msg>,
            tools: List<ToolSpec>,
            onTextDelta: (String) -> Unit,
        ): AiTurn {
            histories += history
            return turns.removeFirst()
        }
    }

    private fun List<List<Msg>>.flattenForAssertion(): String {
        return joinToString("\n") { history ->
            history.joinToString("\n") { msg ->
                listOf(
                    msg.role,
                    msg.text,
                    msg.toolCalls.joinToString { "${it.id}:${it.name}:${it.args}" },
                    msg.toolResults.joinToString { "${it.id}:${it.result}" },
                ).joinToString("|")
            }
        }
    }
}
