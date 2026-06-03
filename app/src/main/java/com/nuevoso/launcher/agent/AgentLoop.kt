package com.nuevoso.launcher.agent

import com.nuevoso.launcher.ai.AiProvider
import com.nuevoso.launcher.ai.Msg
import com.nuevoso.launcher.ai.ToolResult
import com.nuevoso.launcher.data.memory.MemoryRepository

class AgentLoop(
    private val provider: AiProvider,
    private val dispatcher: ActionDispatcher,
    private val memoryRepository: MemoryRepository,
) {
    suspend fun run(
        systemPrompt: String,
        history: List<Msg>,
        onPartialText: (String) -> Unit = {},
    ): String {
        var pendingToolResults: List<ToolResult> = emptyList()
        var lastText = ""

        repeat(6) { // max rounds to prevent infinite loops
            val turn = provider.chat(
                system = systemPrompt,
                history = history,
                tools = ALL_TOOLS,
                toolResults = pendingToolResults,
            )

            if (turn.text.isNotBlank()) {
                lastText = turn.text
                onPartialText(turn.text)
            }

            if (turn.toolCalls.isEmpty()) return lastText

            pendingToolResults = turn.toolCalls.map { dispatcher.dispatch(it) }
        }

        return lastText
    }
}
