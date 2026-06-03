package com.nuevoso.launcher.ai

interface AiProvider {
    suspend fun chat(
        system: String,
        history: List<Msg>,
        tools: List<ToolSpec>,
        toolResults: List<ToolResult> = emptyList(),
    ): AiTurn
}
