package com.nuevoso.launcher.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nuevoso.launcher.App
import com.nuevoso.launcher.data.credentials.CredentialOperationResult
import com.nuevoso.launcher.data.memory.UserFact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val hasApiKey: Boolean = false,
    val modelId: String = "gemini-2.5-flash",
    val providerId: String = "gemini",
    val facts: List<UserFact> = emptyList(),
    val credentialError: Boolean = false,
)

val GEMINI_MODELS = listOf(
    "gemini-2.5-flash" to "Gemini 2.5 Flash (recomendado)",
    "gemini-2.5-pro" to "Gemini 2.5 Pro",
    "gemini-2.0-flash" to "Gemini 2.0 Flash",
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = App.get(application)
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        app.settingsRepository.settings
            .onEach { s ->
                _state.update {
                    it.copy(hasApiKey = s.hasApiKey, modelId = s.modelId, providerId = s.providerId)
                }
            }
            .launchIn(viewModelScope)

        app.memoryRepository.facts
            .onEach { facts -> _state.update { it.copy(facts = facts) } }
            .launchIn(viewModelScope)
    }

    fun saveApiKey(key: String) = viewModelScope.launch {
        val result = app.settingsRepository.saveApiKey(key)
        _state.update { it.copy(credentialError = result != CredentialOperationResult.Success) }
    }

    fun clearApiKey() = viewModelScope.launch {
        val result = app.settingsRepository.clearApiKey()
        _state.update { it.copy(credentialError = result != CredentialOperationResult.Success) }
    }

    fun saveModel(modelId: String) = viewModelScope.launch {
        app.settingsRepository.saveModel(modelId)
    }

    fun clearMemory() = viewModelScope.launch {
        app.memoryRepository.clearFacts()
        app.memoryRepository.clearHistory()
    }
}
