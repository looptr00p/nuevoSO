package com.nuevoso.launcher.security

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigurationHardeningTest {
    @Test
    fun backupIsDisabledInManifestAndRulesExcludeSensitiveStorage() {
        val manifest = readRepoFile("app/src/main/AndroidManifest.xml")
        val backupRules = readRepoFile("app/src/main/res/xml/backup_rules.xml")
        val extractionRules = readRepoFile("app/src/main/res/xml/data_extraction_rules.xml")

        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(backupRules.contains("domain=\"database\""))
        assertTrue(backupRules.contains("domain=\"file\""))
        assertTrue(extractionRules.contains("domain=\"database\""))
        assertTrue(extractionRules.contains("domain=\"file\""))
    }

    @Test
    fun geminiProviderDoesNotInstallHttpLoggingInterceptor() {
        val source = readRepoFile("app/src/main/java/com/nuevoso/launcher/ai/gemini/GeminiProvider.kt")

        assertFalse(source.contains("HttpLoggingInterceptor"))
        assertFalse(source.contains("addInterceptor(logging)"))
    }

    private fun readRepoFile(path: String): String {
        val candidates = listOf(
            File(path),
            File("../$path"),
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("Cannot find $path from ${File(".").absolutePath}")
        return file.readText()
    }
}
