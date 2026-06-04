package com.nuevoso.launcher.data.credentials

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidKeystoreCredentialRepository(context: Context) : CredentialRepository {
    private val appContext = context.applicationContext

    override suspend fun readApiKey(providerId: String): ApiKeyReadResult = withContext(Dispatchers.IO) {
        val preferences = securePreferences()
            ?: return@withContext ApiKeyReadResult.Failure(CredentialFailureCode.SECURE_STORAGE_UNAVAILABLE)
        try {
            val key = preferences.getString(apiKeyName(providerId), null)
            if (key.isNullOrBlank()) ApiKeyReadResult.Missing else ApiKeyReadResult.Available(key)
        } catch (e: Exception) {
            ApiKeyReadResult.Failure(CredentialFailureCode.READ_FAILED)
        }
    }

    override suspend fun saveApiKey(
        providerId: String,
        apiKey: String,
    ): CredentialOperationResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext clearApiKey(providerId)
        val preferences = securePreferences()
            ?: return@withContext CredentialOperationResult.Failure(
                CredentialFailureCode.SECURE_STORAGE_UNAVAILABLE
            )
        try {
            val committed = preferences.edit()
                .putString(apiKeyName(providerId), apiKey)
                .commit()
            if (committed) {
                CredentialOperationResult.Success
            } else {
                CredentialOperationResult.Failure(CredentialFailureCode.WRITE_FAILED)
            }
        } catch (e: Exception) {
            CredentialOperationResult.Failure(CredentialFailureCode.WRITE_FAILED)
        }
    }

    override suspend fun clearApiKey(providerId: String): CredentialOperationResult = withContext(Dispatchers.IO) {
        val preferences = securePreferences()
            ?: return@withContext CredentialOperationResult.Failure(
                CredentialFailureCode.SECURE_STORAGE_UNAVAILABLE
            )
        try {
            val committed = preferences.edit()
                .remove(apiKeyName(providerId))
                .commit()
            if (committed) {
                CredentialOperationResult.Success
            } else {
                CredentialOperationResult.Failure(CredentialFailureCode.DELETE_FAILED)
            }
        } catch (e: Exception) {
            CredentialOperationResult.Failure(CredentialFailureCode.DELETE_FAILED)
        }
    }

    private fun securePreferences(): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun apiKeyName(providerId: String): String {
        val safeProviderId = providerId.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '-' }
        return "api_key_${safeProviderId.ifBlank { "unknown" }}"
    }

    companion object {
        private const val FILE_NAME = "secure_credentials"
    }
}
