package com.nuevoso.launcher.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nuevoso.launcher.App
import com.nuevoso.launcher.agent.ActionDispatcher
import com.nuevoso.launcher.agent.AgentContinuation
import com.nuevoso.launcher.agent.AgentLoop
import com.nuevoso.launcher.agent.AgentLoopResult
import com.nuevoso.launcher.agent.security.ApprovalPrompt
import com.nuevoso.launcher.ai.Msg
import com.nuevoso.launcher.ai.buildSystemPrompt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private var pendingSession: PendingConfirmationSession? = null
    private var confirmationTimeoutJob: Job? = null

    init {
        app.settingsRepository.settings
            .onEach { settings -> _state.update { it.copy(hasApiKey = settings.apiKey.isNotBlank()) } }
            .launchIn(viewModelScope)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (_state.value.pendingConfirmation != null) {
            _state.update { it.copy(error = "Resuelve la confirmación pendiente primero.") }
            return
        }

        val userMsg = ChatMessage(role = "user", text = text)
        _state.update {
            it.copy(
                messages = it.messages + userMsg,
                isThinking = true,
                error = null,
                pendingConfirmation = null,
            )
        }

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
                    approvalStore = app.approvalStore,
                )
                val loop = AgentLoop(provider, dispatcher)

                val result = loop.run(
                    systemPrompt = systemPrompt,
                    history = history,
                    onPartialText = { acc ->
                        _state.update { it.copy(isThinking = false, streamingText = acc) }
                    },
                )

                handleLoopResult(result, loop, dispatcher)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isThinking = false,
                        streamingText = null,
                        pendingConfirmation = null,
                        error = "Error seguro: la acción no se completó.",
                    )
                }
            }
        }
    }

    fun approvePendingConfirmation() {
        resolvePendingConfirmation(approved = true)
    }

    fun rejectPendingConfirmation() {
        resolvePendingConfirmation(approved = false)
    }

    private fun resolvePendingConfirmation(approved: Boolean) {
        val session = pendingSession ?: return
        clearPendingConfirmation()
        _state.update { it.copy(isThinking = true, streamingText = null, error = null) }

        viewModelScope.launch {
            try {
                val toolResult = session.dispatcher.resolveApproval(session.prompt, approved)
                val result = session.loop.continueAfterConfirmation(
                    continuation = session.continuation,
                    confirmationResult = toolResult,
                    onPartialText = { acc ->
                        _state.update { it.copy(isThinking = false, streamingText = acc) }
                    },
                )
                handleLoopResult(result, session.loop, session.dispatcher)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isThinking = false,
                        streamingText = null,
                        pendingConfirmation = null,
                        error = "Error seguro: la acción no se completó.",
                    )
                }
            }
        }
    }

    private fun expirePendingConfirmation(token: String) {
        val session = pendingSession ?: return
        if (session.prompt.token != token) return
        clearPendingConfirmation()
        _state.update { it.copy(isThinking = true, streamingText = null, error = null) }

        viewModelScope.launch {
            try {
                val toolResult = session.dispatcher.expireApproval(session.prompt)
                val result = session.loop.continueAfterConfirmation(
                    continuation = session.continuation,
                    confirmationResult = toolResult,
                    onPartialText = { acc ->
                        _state.update { it.copy(isThinking = false, streamingText = acc) }
                    },
                )
                handleLoopResult(result, session.loop, session.dispatcher)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isThinking = false,
                        streamingText = null,
                        pendingConfirmation = null,
                        error = "Error seguro: la acción no se completó.",
                    )
                }
            }
        }
    }

    private suspend fun handleLoopResult(
        result: AgentLoopResult,
        loop: AgentLoop,
        dispatcher: ActionDispatcher,
    ) {
        when (result) {
            is AgentLoopResult.Completed -> {
                val assistantMsg = ChatMessage(role = "assistant", text = result.text.ifBlank { "OK" })
                app.memoryRepository.saveMessage("model", assistantMsg.text)
                _state.update {
                    it.copy(
                        messages = it.messages + assistantMsg,
                        isThinking = false,
                        streamingText = null,
                        pendingConfirmation = null,
                    )
                }
            }
            is AgentLoopResult.PendingConfirmation -> {
                if (result.textBeforeConfirmation.isNotBlank()) {
                    val assistantMsg = ChatMessage(role = "assistant", text = result.textBeforeConfirmation)
                    app.memoryRepository.saveMessage("model", assistantMsg.text)
                    _state.update { it.copy(messages = it.messages + assistantMsg) }
                }
                val session = PendingConfirmationSession(
                    prompt = result.prompt,
                    loop = loop,
                    dispatcher = dispatcher,
                    continuation = result.continuation,
                )
                pendingSession = session
                _state.update {
                    it.copy(
                        isThinking = false,
                        streamingText = null,
                        pendingConfirmation = result.prompt,
                    )
                }
                scheduleConfirmationTimeout(result.prompt)
            }
        }
    }

    private fun scheduleConfirmationTimeout(prompt: ApprovalPrompt) {
        confirmationTimeoutJob?.cancel()
        confirmationTimeoutJob = viewModelScope.launch {
            val delayMillis = (prompt.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
            delay(delayMillis)
            expirePendingConfirmation(prompt.token)
        }
    }

    private fun clearPendingConfirmation() {
        confirmationTimeoutJob?.cancel()
        confirmationTimeoutJob = null
        pendingSession = null
        _state.update { it.copy(pendingConfirmation = null) }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private data class PendingConfirmationSession(
        val prompt: ApprovalPrompt,
        val loop: AgentLoop,
        val dispatcher: ActionDispatcher,
        val continuation: AgentContinuation,
    )
}
