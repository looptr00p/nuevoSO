package com.nuevoso.launcher.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nuevoso.launcher.data.credentials.ApiKeyReadResult
import com.nuevoso.launcher.data.credentials.AndroidKeystoreCredentialRepository
import com.nuevoso.launcher.data.credentials.CredentialFailureCode
import com.nuevoso.launcher.data.credentials.CredentialOperationResult
import com.nuevoso.launcher.data.credentials.CredentialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val hasApiKey: Boolean = false,
    val modelId: String = "gemini-2.5-flash",
    val providerId: String = "gemini",
)

class SettingsRepository(
    private val preferences: SettingsPreferences,
    private val credentialRepository: CredentialRepository,
) {
    constructor(context: Context) : this(
        preferences = DataStoreSettingsPreferences(context.dataStore),
        credentialRepository = AndroidKeystoreCredentialRepository(context),
    )

    private val migrationMutex = Mutex()

    val settings: Flow<AppSettings> = preferences.settings.map { persisted ->
        persisted.toAppSettings(hasApiKey = credentialRepository.hasApiKey(persisted.providerId))
    }

    suspend fun currentSettings(): AppSettings {
        migrateLegacyApiKeyIfPresent()
        val persisted = preferences.settings.first()
        return persisted.toAppSettings(hasApiKey = credentialRepository.hasApiKey(persisted.providerId))
    }

    suspend fun readApiKey(): ApiKeyReadResult {
        migrateLegacyApiKeyIfPresent()
        val providerId = preferences.settings.first().providerId
        return credentialRepository.readApiKey(providerId)
    }

    suspend fun migrateLegacyApiKeyIfPresent(): CredentialOperationResult = migrationMutex.withLock {
        val persisted = preferences.settings.first()
        val providerId = persisted.providerId
        val secureResult = credentialRepository.readApiKey(providerId)

        when (secureResult) {
            is ApiKeyReadResult.Available -> {
                val legacyKey = preferences.readLegacyApiKey()
                if (legacyKey.isBlank() || preferences.clearLegacyApiKey()) {
                    CredentialOperationResult.Success
                } else {
                    CredentialOperationResult.Failure(CredentialFailureCode.DELETE_FAILED)
                }
            }
            ApiKeyReadResult.Missing -> {
                val legacyKey = preferences.readLegacyApiKey()
                if (legacyKey.isBlank()) {
                    CredentialOperationResult.Success
                } else {
                    when (val writeResult = credentialRepository.saveApiKey(providerId, legacyKey)) {
                        CredentialOperationResult.Success -> {
                            if (preferences.clearLegacyApiKey()) {
                                CredentialOperationResult.Success
                            } else {
                                CredentialOperationResult.Failure(CredentialFailureCode.DELETE_FAILED)
                            }
                        }
                        is CredentialOperationResult.Failure -> writeResult
                    }
                }
            }
            is ApiKeyReadResult.Failure -> CredentialOperationResult.Failure(secureResult.code)
        }
    }

    suspend fun saveApiKey(key: String): CredentialOperationResult {
        val providerId = preferences.settings.first().providerId
        val secureResult = credentialRepository.saveApiKey(providerId, key)
        if (secureResult != CredentialOperationResult.Success) return secureResult
        return if (preferences.clearLegacyApiKey()) {
            CredentialOperationResult.Success
        } else {
            CredentialOperationResult.Failure(CredentialFailureCode.DELETE_FAILED)
        }
    }

    suspend fun clearApiKey(): CredentialOperationResult {
        val providerId = preferences.settings.first().providerId
        val secureResult = credentialRepository.clearApiKey(providerId)
        if (secureResult != CredentialOperationResult.Success) return secureResult
        return if (preferences.clearLegacyApiKey()) {
            CredentialOperationResult.Success
        } else {
            CredentialOperationResult.Failure(CredentialFailureCode.DELETE_FAILED)
        }
    }

    suspend fun saveModel(modelId: String) {
        preferences.saveModel(modelId)
    }

    suspend fun saveProvider(providerId: String) {
        preferences.saveProvider(providerId)
    }
}

private suspend fun CredentialRepository.hasApiKey(providerId: String): Boolean {
    return readApiKey(providerId) is ApiKeyReadResult.Available
}

private fun PersistedSettings.toAppSettings(hasApiKey: Boolean): AppSettings {
    return AppSettings(
        hasApiKey = hasApiKey,
        modelId = modelId,
        providerId = providerId,
    )
}

data class PersistedSettings(
    val modelId: String = "gemini-2.5-flash",
    val providerId: String = "gemini",
)

interface SettingsPreferences {
    val settings: Flow<PersistedSettings>
    suspend fun readLegacyApiKey(): String
    suspend fun clearLegacyApiKey(): Boolean
    suspend fun saveModel(modelId: String)
    suspend fun saveProvider(providerId: String)
}

class DataStoreSettingsPreferences(
    private val dataStore: DataStore<Preferences>,
) : SettingsPreferences {
    private object Keys {
        val LEGACY_API_KEY = stringPreferencesKey("api_key")
        val MODEL_ID = stringPreferencesKey("model_id")
        val PROVIDER_ID = stringPreferencesKey("provider_id")
    }

    override val settings: Flow<PersistedSettings> = dataStore.data.map { prefs ->
        PersistedSettings(
            modelId = prefs[Keys.MODEL_ID] ?: "gemini-2.5-flash",
            providerId = prefs[Keys.PROVIDER_ID] ?: "gemini",
        )
    }

    override suspend fun readLegacyApiKey(): String {
        return runCatching {
            dataStore.data.first()[Keys.LEGACY_API_KEY] ?: ""
        }.getOrDefault("")
    }

    override suspend fun clearLegacyApiKey(): Boolean {
        return runCatching {
            dataStore.edit { it.remove(Keys.LEGACY_API_KEY) }
        }.isSuccess
    }

    override suspend fun saveModel(modelId: String) {
        dataStore.edit { it[Keys.MODEL_ID] = modelId }
    }

    override suspend fun saveProvider(providerId: String) {
        dataStore.edit { it[Keys.PROVIDER_ID] = providerId }
    }
}
