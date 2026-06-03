package com.nuevoso.launcher.agent.executors

import com.nuevoso.launcher.accessibility.NuevoSOAccessibilityService

class AccessibilityExecutor {

    private val service get() = NuevoSOAccessibilityService.instance

    fun readScreen(): String {
        val svc = service ?: return notEnabled()
        return svc.readScreen()
    }

    fun tapElement(description: String): String {
        val svc = service ?: return notEnabled()
        return svc.tapElement(description)
    }

    fun typeText(text: String): String {
        val svc = service ?: return notEnabled()
        return svc.typeText(text)
    }

    fun scrollScreen(direction: String): String {
        val svc = service ?: return notEnabled()
        return svc.scrollScreen(direction)
    }

    fun pressBack(): String {
        val svc = service ?: return notEnabled()
        return svc.pressBack()
    }

    private fun notEnabled() =
        "El asistente de pantalla no está activo. El usuario debe habilitarlo en Ajustes > Accesibilidad > nuevoSO."
}
