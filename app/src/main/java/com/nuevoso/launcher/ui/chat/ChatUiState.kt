package com.nuevoso.launcher.ui.chat

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val role: String,  // "user" | "assistant"
    val text: String,
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isThinking: Boolean = false,
    val streamingText: String? = null,  // respuesta del asistente mientras llega por streaming
    val error: String? = null,
    val hasApiKey: Boolean = false,
)
