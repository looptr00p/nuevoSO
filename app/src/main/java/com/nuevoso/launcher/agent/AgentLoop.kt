package com.nuevoso.launcher.agent

import com.nuevoso.launcher.ai.AiProvider
import com.nuevoso.launcher.ai.Msg
import com.nuevoso.launcher.ai.ToolResult
import com.nuevoso.launcher.agent.security.ApprovalPrompt

class AgentLoop(
    private val provider: AiProvider,
    private val dispatcher: ActionDispatcher,
) {
    suspend fun run(
        systemPrompt: String,
        history: List<Msg>,
        onPartialText: (String) -> Unit = {},
    ): AgentLoopResult {
        // Transcript creciente: arranca con el historial de texto y va acumulando los turnos
        // del modelo (texto + tool calls) y los resultados de las herramientas, en orden. Así
        // el modelo ve siempre su propio functionCall antes del functionResponse.
        return runTranscript(systemPrompt, history, onPartialText)
    }

    suspend fun continueAfterConfirmation(
        continuation: AgentContinuation,
        confirmationResult: ToolResult,
        onPartialText: (String) -> Unit = {},
    ): AgentLoopResult {
        val convo = continuation.conversation.toMutableList()
        convo += Msg(
            role = "tool",
            toolResults = continuation.completedToolResults +
                confirmationResult +
                continuation.deferredToolResults,
        )
        return runTranscript(continuation.systemPrompt, convo, onPartialText)
    }

    private suspend fun runTranscript(
        systemPrompt: String,
        initialConversation: List<Msg>,
        onPartialText: (String) -> Unit,
    ): AgentLoopResult {
        val convo = initialConversation.toMutableList()
        var lastText = ""

        repeat(15) { // max rounds — navegación web puede requerir varios pasos
            val turn = provider.chat(
                system = systemPrompt,
                history = convo,
                tools = ALL_TOOLS,
                onTextDelta = onPartialText,
            )

            if (turn.text.isNotBlank()) lastText = turn.text

            convo += Msg(role = "model", text = turn.text, toolCalls = turn.toolCalls)

            if (turn.toolCalls.isEmpty()) return AgentLoopResult.Completed(lastText)

            val results = mutableListOf<ToolResult>()
            turn.toolCalls.forEachIndexed { index, call ->
                when (val dispatchResult = dispatcher.dispatchForAgent(call)) {
                    is ActionDispatchResult.Completed -> results += dispatchResult.toolResult
                    is ActionDispatchResult.PendingConfirmation -> {
                        val remainingDeferred = turn.toolCalls
                            .drop(index + 1)
                            .map {
                                ToolResult(
                                    id = it.id,
                                    result = "Deferred until the current confirmation is resolved.",
                                )
                            }
                        return AgentLoopResult.PendingConfirmation(
                            textBeforeConfirmation = lastText,
                            prompt = dispatchResult.pending.prompt,
                            continuation = AgentContinuation(
                                systemPrompt = systemPrompt,
                                conversation = convo.toList(),
                                completedToolResults = results.toList(),
                                deferredToolResults = remainingDeferred,
                            ),
                        )
                    }
                }
            }
            convo += Msg(role = "tool", toolResults = results)
        }

        // Se agotaron las rondas con tool calls aún pendientes.
        return AgentLoopResult.Completed(lastText.ifBlank { "No pude completar la acción en varios intentos." })
    }
}

sealed class AgentLoopResult {
    data class Completed(val text: String) : AgentLoopResult()
    data class PendingConfirmation(
        val textBeforeConfirmation: String,
        val prompt: ApprovalPrompt,
        val continuation: AgentContinuation,
    ) : AgentLoopResult()
}

data class AgentContinuation(
    val systemPrompt: String,
    val conversation: List<Msg>,
    val completedToolResults: List<ToolResult>,
    val deferredToolResults: List<ToolResult>,
)
