package com.nuevoso.launcher.agent

import com.nuevoso.launcher.ai.AiProvider
import com.nuevoso.launcher.ai.Msg
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
        // Transcript creciente: arranca con el historial de texto y va acumulando los turnos
        // del modelo (texto + tool calls) y los resultados de las herramientas, en orden. Así
        // el modelo ve siempre su propio functionCall antes del functionResponse.
        val convo = history.toMutableList()
        var lastText = ""

        repeat(6) { // max rounds to prevent infinite loops
            val turn = provider.chat(
                system = systemPrompt,
                history = convo,
                tools = ALL_TOOLS,
                onTextDelta = onPartialText,
            )

            if (turn.text.isNotBlank()) lastText = turn.text

            convo += Msg(role = "model", text = turn.text, toolCalls = turn.toolCalls)

            if (turn.toolCalls.isEmpty()) return lastText

            val results = turn.toolCalls.map { dispatcher.dispatch(it) }
            convo += Msg(role = "tool", toolResults = results)
        }

        // Se agotaron las rondas con tool calls aún pendientes.
        return lastText.ifBlank { "No pude completar la acción en varios intentos." }
    }
}
