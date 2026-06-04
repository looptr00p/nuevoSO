package com.nuevoso.launcher.data.settings

import com.nuevoso.launcher.data.credentials.CredentialFailureCode
import com.nuevoso.launcher.data.credentials.CredentialOperationResult
import com.nuevoso.launcher.test.FakeCredentialRepository
import com.nuevoso.launcher.test.FakeSettingsPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {
    @Test
    fun secureApiKeyTakesPriorityAndLegacyValueIsCleared() = runBlocking {
        val preferences = FakeSettingsPreferences(legacyApiKey = "legacy-synthetic-key")
        val credentials = FakeCredentialRepository().apply {
            apiKeys["gemini"] = "secure-synthetic-key"
        }
        val repository = SettingsRepository(preferences, credentials)

        val result = repository.migrateLegacyApiKeyIfPresent()

        assertEquals(CredentialOperationResult.Success, result)
        assertEquals("", preferences.legacyApiKey)
        assertTrue(credentials.writtenKeys.isEmpty())
        assertEquals(true, repository.settings.first().hasApiKey)
    }

    @Test
    fun legacyApiKeyMigratesOnlyAfterEncryptedWriteSucceeds() = runBlocking {
        val preferences = FakeSettingsPreferences(legacyApiKey = "legacy-synthetic-key")
        val credentials = FakeCredentialRepository()
        val repository = SettingsRepository(preferences, credentials)

        val result = repository.migrateLegacyApiKeyIfPresent()

        assertEquals(CredentialOperationResult.Success, result)
        assertEquals("legacy-synthetic-key", credentials.apiKeys["gemini"])
        assertEquals("", preferences.legacyApiKey)
    }

    @Test
    fun legacyApiKeyIsNotDeletedWhenEncryptedWriteFails() = runBlocking {
        val preferences = FakeSettingsPreferences(legacyApiKey = "legacy-synthetic-key")
        val credentials = FakeCredentialRepository().apply {
            writeFailureCode = CredentialFailureCode.WRITE_FAILED
        }
        val repository = SettingsRepository(preferences, credentials)

        val result = repository.migrateLegacyApiKeyIfPresent()

        assertEquals(CredentialOperationResult.Failure(CredentialFailureCode.WRITE_FAILED), result)
        assertEquals("legacy-synthetic-key", preferences.legacyApiKey)
        assertFalse(credentials.apiKeys.containsKey("gemini"))
    }

    @Test
    fun providerAndModelSettingsArePreservedOutsideCredentialStorage() = runBlocking {
        val preferences = FakeSettingsPreferences()
        val credentials = FakeCredentialRepository()
        val repository = SettingsRepository(preferences, credentials)

        repository.saveProvider("gemini")
        repository.saveModel("gemini-2.5-pro")
        val result = repository.saveApiKey("synthetic-api-key")
        val settings = repository.currentSettings()

        assertEquals(CredentialOperationResult.Success, result)
        assertEquals("gemini", settings.providerId)
        assertEquals("gemini-2.5-pro", settings.modelId)
        assertTrue(settings.hasApiKey)
        assertEquals("synthetic-api-key", credentials.apiKeys["gemini"])
    }

    @Test
    fun credentialFailuresReturnControlledCodesWithoutRawApiKey() = runBlocking {
        val rawKey = "raw-private-synthetic-api-key"
        val preferences = FakeSettingsPreferences()
        val credentials = FakeCredentialRepository().apply {
            writeFailureCode = CredentialFailureCode.SECURE_STORAGE_UNAVAILABLE
        }
        val repository = SettingsRepository(preferences, credentials)

        val result = repository.saveApiKey(rawKey)

        assertEquals(
            CredentialOperationResult.Failure(CredentialFailureCode.SECURE_STORAGE_UNAVAILABLE),
            result,
        )
        assertFalse(result.toString().contains(rawKey))
    }
}
