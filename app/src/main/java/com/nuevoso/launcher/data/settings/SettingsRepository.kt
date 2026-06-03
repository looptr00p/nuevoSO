package com.nuevoso.launcher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val apiKey: String = "",
    val modelId: String = "gemini-2.5-flash",
    val providerId: String = "gemini",
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_ID = stringPreferencesKey("model_id")
        val PROVIDER_ID = stringPreferencesKey("provider_id")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            apiKey = prefs[Keys.API_KEY] ?: "",
            modelId = prefs[Keys.MODEL_ID] ?: "gemini-2.5-flash",
            providerId = prefs[Keys.PROVIDER_ID] ?: "gemini",
        )
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { it[Keys.API_KEY] = key }
    }

    suspend fun saveModel(modelId: String) {
        context.dataStore.edit { it[Keys.MODEL_ID] = modelId }
    }

    suspend fun saveProvider(providerId: String) {
        context.dataStore.edit { it[Keys.PROVIDER_ID] = providerId }
    }
}
