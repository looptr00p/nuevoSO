package com.nuevoso.launcher.agent.executors

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nuevoso.launcher.accessibility.NuevoSOAccessibilityService
import kotlinx.coroutines.delay

class InstallAppExecutor(private val context: Context) {

    suspend fun execute(appName: String): String {
        if (appName.isBlank()) return "Especifica el nombre de la app a instalar."

        val service = NuevoSOAccessibilityService.instance
            ?: return "Para instalar apps automáticamente activa el asistente de pantalla: Ajustes → Accesibilidad → nuevoSO."

        // 1. Abrir Play Store en búsqueda directa
        val opened = openPlayStore(appName)
        if (!opened) return "No se pudo abrir Play Store."

        delay(3500) // esperar que cargue la búsqueda

        // 2. Tocar el primer resultado que coincida con el nombre
        service.tapElement(appName)
        delay(2500) // esperar que cargue la ficha de la app

        // 3. Tocar "Instalar" (intentar español e inglés)
        val installResult = tryTap(service, listOf("Instalar", "Install"))
        if (!installResult) {
            // Puede que ya esté instalada → buscar "Abrir"
            val screen = service.readScreen()
            if (screen.contains("Abrir", ignoreCase = true) || screen.contains("Open", ignoreCase = true)) {
                tryTap(service, listOf("Abrir", "Open"))
                return "$appName ya estaba instalada. Abierta."
            }
            return "No encontré el botón de instalar en Play Store. Verifica que la app existe."
        }

        // 4. Esperar que termine la instalación (máx ~90 s)
        repeat(45) {
            delay(2000)
            val screen = service.readScreen()
            when {
                screen.contains("Abrir", ignoreCase = true) || screen.contains("Open", ignoreCase = true) -> {
                    tryTap(service, listOf("Abrir", "Open"))
                    return "✓ $appName instalada y abierta."
                }
                screen.contains("Desinstalar", ignoreCase = true) || screen.contains("Uninstall", ignoreCase = true) -> {
                    // instalada pero sin botón Abrir visible todavía
                    launchByName(appName)
                    return "✓ $appName instalada. Intentando abrir…"
                }
            }
        }

        return "Instalación iniciada. Cuando termine pulsa Abrir en Play Store."
    }

    private fun openPlayStore(appName: String): Boolean {
        val marketIntent = Intent(Intent.ACTION_VIEW,
            Uri.parse("market://search?q=${Uri.encode(appName)}&c=apps"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(marketIntent)
            true
        } catch (e: Exception) {
            // Play Store no disponible: abrir web
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/search?q=${Uri.encode(appName)}&c=apps"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (e2: Exception) { false }
        }
    }

    private fun tryTap(service: NuevoSOAccessibilityService, labels: List<String>): Boolean {
        for (label in labels) {
            val result = service.tapElement(label)
            if (!result.contains("No se encontró")) return true
        }
        return false
    }

    private fun launchByName(appName: String) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .firstOrNull { it.loadLabel(pm).toString().contains(appName, ignoreCase = true) }
            ?.let { pm.getLaunchIntentForPackage(it.activityInfo.packageName) }
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let { context.startActivity(it) }
    }
}
