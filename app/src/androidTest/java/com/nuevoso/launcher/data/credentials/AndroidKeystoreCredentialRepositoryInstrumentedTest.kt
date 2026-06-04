package com.nuevoso.launcher.data.credentials

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreCredentialRepositoryInstrumentedTest {
    @Test
    fun storesReadsAndClearsSyntheticApiKey() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = AndroidKeystoreCredentialRepository(context)
        val providerId = "gemini"
        val syntheticKey = "synthetic-test-api-key"

        try {
            assertEquals(CredentialOperationResult.Success, repository.saveApiKey(providerId, syntheticKey))
            assertEquals(ApiKeyReadResult.Available(syntheticKey), repository.readApiKey(providerId))
            assertEquals(CredentialOperationResult.Success, repository.clearApiKey(providerId))
            assertTrue(repository.readApiKey(providerId) is ApiKeyReadResult.Missing)
        } finally {
            repository.clearApiKey(providerId)
        }
    }
}
