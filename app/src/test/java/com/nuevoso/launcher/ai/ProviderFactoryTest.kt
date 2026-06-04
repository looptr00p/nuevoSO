package com.nuevoso.launcher.ai

import com.nuevoso.launcher.data.settings.SettingsRepository
import com.nuevoso.launcher.test.FakeCredentialRepository
import com.nuevoso.launcher.test.FakeSettingsPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ProviderFactoryTest {
    @Test
    fun returnsNullWhenSecureApiKeyIsMissing() = runBlocking {
        val repository = SettingsRepository(FakeSettingsPreferences(), FakeCredentialRepository())
        val factory = ProviderFactory(repository) { _, _, _ -> error("provider should not be built") }

        val (provider, settings) = factory.build()

        assertNull(provider)
        assertEquals(false, settings.hasApiKey)
    }

    @Test
    fun buildsProviderWithSecureApiKeyAndConfiguredModel() = runBlocking {
        val expectedProvider = object : AiProvider {
            override suspend fun chat(
                system: String,
                history: List<Msg>,
                tools: List<ToolSpec>,
                onTextDelta: (String) -> Unit,
            ): AiTurn = AiTurn(text = "")
        }
        val credentials = FakeCredentialRepository().apply {
            apiKeys["gemini"] = "secure-synthetic-key"
        }
        val repository = SettingsRepository(FakeSettingsPreferences(), credentials)
        var capturedApiKey = ""
        var capturedModelId = ""
        val factory = ProviderFactory(repository) { _, apiKey, modelId ->
            capturedApiKey = apiKey
            capturedModelId = modelId
            expectedProvider
        }

        val (provider, settings) = factory.build()

        assertSame(expectedProvider, provider)
        assertEquals("secure-synthetic-key", capturedApiKey)
        assertEquals("gemini-2.5-flash", capturedModelId)
        assertEquals(true, settings.hasApiKey)
    }
}
