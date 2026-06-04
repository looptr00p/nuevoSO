package com.nuevoso.launcher.test

import com.nuevoso.launcher.data.settings.PersistedSettings
import com.nuevoso.launcher.data.settings.SettingsPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsPreferences(
    initialSettings: PersistedSettings = PersistedSettings(),
    var legacyApiKey: String = "",
) : SettingsPreferences {
    private val state = MutableStateFlow(initialSettings)
    var clearLegacyApiKeySucceeds = true

    override val settings: Flow<PersistedSettings> = state

    override suspend fun readLegacyApiKey(): String = legacyApiKey

    override suspend fun clearLegacyApiKey(): Boolean {
        if (!clearLegacyApiKeySucceeds) return false
        legacyApiKey = ""
        return true
    }

    override suspend fun saveModel(modelId: String) {
        state.value = state.value.copy(modelId = modelId)
    }

    override suspend fun saveProvider(providerId: String) {
        state.value = state.value.copy(providerId = providerId)
    }
}
