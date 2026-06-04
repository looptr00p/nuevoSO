package com.nuevoso.launcher.ai

import com.nuevoso.launcher.ai.gemini.GeminiProvider
import com.nuevoso.launcher.data.credentials.ApiKeyReadResult
import com.nuevoso.launcher.data.settings.AppSettings
import com.nuevoso.launcher.data.settings.SettingsRepository

class ProviderFactory(
    private val settingsRepository: SettingsRepository,
    private val providerBuilder: (providerId: String, apiKey: String, modelId: String) -> AiProvider? =
        { providerId, apiKey, modelId ->
            when (providerId) {
                "gemini" -> GeminiProvider(apiKey = apiKey, modelId = modelId)
                // future: "claude" -> ClaudeProvider(...)
                // future: "local"  -> LocalProvider(...)
                else -> null
            }
        },
) {

    suspend fun build(): Pair<AiProvider?, AppSettings> {
        val settings = settingsRepository.currentSettings()
        val apiKey = when (val result = settingsRepository.readApiKey()) {
            is ApiKeyReadResult.Available -> result.apiKey
            ApiKeyReadResult.Missing,
            is ApiKeyReadResult.Failure,
            -> return null to settings.copy(hasApiKey = false)
        }
        val provider = providerBuilder(settings.providerId, apiKey, settings.modelId)
        return provider to settings
    }
}
