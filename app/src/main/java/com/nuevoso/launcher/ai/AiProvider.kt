package com.nuevoso.launcher.ai

interface AiProvider {
    suspend fun chat(
        system: String,
        history: List<Msg>,
        tools: List<ToolSpec>,
        onTextDelta: (String) -> Unit = {},   // texto acumulado emitido durante el streaming
    ): AiTurn
}
