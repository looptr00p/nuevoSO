package com.nuevoso.launcher.ai

import com.nuevoso.launcher.ai.gemini.GeminiProvider
import com.nuevoso.launcher.data.settings.AppSettings
import com.nuevoso.launcher.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first

class ProviderFactory(private val settingsRepository: SettingsRepository) {

    suspend fun build(): Pair<AiProvider?, AppSettings> {
        val settings = settingsRepository.settings.first()
        if (settings.apiKey.isBlank()) return null to settings
        val provider = when (settings.providerId) {
            "gemini" -> GeminiProvider(apiKey = settings.apiKey, modelId = settings.modelId)
            // future: "claude" -> ClaudeProvider(...)
            // future: "local"  -> LocalProvider(...)
            else -> GeminiProvider(apiKey = settings.apiKey, modelId = settings.modelId)
        }
        return provider to settings
    }
}
