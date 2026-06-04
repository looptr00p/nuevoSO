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
        val requiredDomains = listOf(
            "root",
            "file",
            "database",
            "sharedpref",
            "external",
            "device_root",
            "device_file",
            "device_database",
            "device_sharedpref",
        )

        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        requiredDomains.forEach { domain ->
            assertTrue("backup_rules.xml missing $domain", backupRules.contains("domain=\"$domain\""))
            assertTrue("data_extraction_rules.xml missing $domain", extractionRules.contains("domain=\"$domain\""))
        }
        assertTrue(extractionRules.contains("<cloud-backup"))
        assertTrue(extractionRules.contains("<device-transfer>"))
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
