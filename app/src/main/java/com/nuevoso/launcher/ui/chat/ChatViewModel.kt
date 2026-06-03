package com.nuevoso.launcher.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nuevoso.launcher.App
import com.nuevoso.launcher.agent.ActionDispatcher
import com.nuevoso.launcher.agent.AgentLoop
import com.nuevoso.launcher.ai.Msg
import com.nuevoso.launcher.ai.buildSystemPrompt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = App.get(application)
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        app.settingsRepository.settings
            .onEach { settings -> _state.update { it.copy(hasApiKey = settings.apiKey.isNotBlank()) } }
            .launchIn(viewModelScope)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(role = "user", text = text)
        _state.update { it.copy(messages = it.messages + userMsg, isThinking = true, error = null) }

        viewModelScope.launch {
            app.memoryRepository.saveMessage("user", text)

            val (provider, _) = app.providerFactory.build()
            if (provider == null) {
                _state.update {
                    it.copy(
                        isThinking = false,
                        error = "Configura tu API Key en Ajustes primero.",
                    )
                }
                return@launch
            }

            try {
                val memContext = app.memoryRepository.buildMemoryContext()
                val systemPrompt = buildSystemPrompt(memContext)

                val history = _state.value.messages
                    .dropLast(1) // exclude the user msg we just added — we pass it separately
                    .takeLast(20)
                    .map { Msg(role = if (it.role == "user") "user" else "model", text = it.text) } +
                        Msg(role = "user", text = text)

                val dispatcher = ActionDispatcher(
                    context = app,
                    appRepository = app.appRepository,
                    memoryRepository = app.memoryRepository,
                )
                val loop = AgentLoop(provider, dispatcher, app.memoryRepository)

                val reply = loop.run(systemPrompt = systemPrompt, history = history)

                val assistantMsg = ChatMessage(role = "assistant", text = reply.ifBlank { "✓" })
                app.memoryRepository.saveMessage("model", assistantMsg.text)
                _state.update { it.copy(messages = it.messages + assistantMsg, isThinking = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isThinking = false,
                        error = "Error: ${e.message ?: "desconocido"}",
                    )
                }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}
