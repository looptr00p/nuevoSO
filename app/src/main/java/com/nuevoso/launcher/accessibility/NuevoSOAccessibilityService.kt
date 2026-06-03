package com.nuevoso.launcher.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class NuevoSOAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
    }

    // ── public API (called from executors) ──────────────────────────────────

    fun isConnected(): Boolean = instance != null

    /** Devuelve un resumen de texto legible de la pantalla actual. */
    fun readScreen(): String {
        val root = rootInActiveWindow ?: return "No se pudo leer la pantalla."
        val sb = StringBuilder()
        val packageName = root.packageName?.toString() ?: ""
        if (packageName.isNotBlank()) sb.appendLine("[APP: $packageName]")
        appendNode(root, sb, 0)
        root.recycle()
        return sb.toString().trim().take(4000) // limitar tamaño para el prompt
    }

    /** Hace tap en el primer nodo cuyo texto o content-description contenga [description]. */
    fun tapElement(description: String): String {
        val root = rootInActiveWindow ?: return "Error: no hay ventana activa."
        val node = findNode(root, description)
        root.recycle()
        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()
            if (result) "Toque en '$description' realizado." else "No se pudo hacer toque en '$description'."
        } else {
            "No se encontró el elemento '$description' en pantalla."
        }
    }

    /** Escribe texto en el nodo enfocado o en el primer EditText visible. */
    fun typeText(text: String): String {
        val root = rootInActiveWindow ?: return "Error: no hay ventana activa."
        // Buscar input enfocado primero, luego cualquier EditText
        var input = findFocusedInput(root) ?: findNodeByClass(root, "android.widget.EditText")
        root.recycle()
        return if (input != null) {
            input.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle().apply { putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
            val result = input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            input.recycle()
            if (result) "Texto '$text' escrito." else "No se pudo escribir el texto."
        } else {
            "No se encontró un campo de texto activo en pantalla."
        }
    }

    /** Hace scroll en la dirección indicada. */
    fun scrollScreen(direction: String): String {
        val root = rootInActiveWindow ?: return "Error: no hay ventana activa."
        val action = when (direction.lowercase()) {
            "down", "abajo" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        val scrollable = findScrollable(root)
        root.recycle()
        return if (scrollable != null) {
            val result = scrollable.performAction(action)
            scrollable.recycle()
            if (result) "Scroll $direction realizado." else "No se pudo hacer scroll."
        } else {
            "No se encontró un elemento desplazable en pantalla."
        }
    }

    /** Presiona el botón Atrás del sistema. */
    fun pressBack(): String {
        return if (performGlobalAction(GLOBAL_ACTION_BACK)) "Atrás pulsado."
        else "No se pudo pulsar Atrás."
    }

    /** Abre una URL en el navegador predeterminado controlando el intent desde accesibilidad.
     *  (El agente también puede usar open_app + type_url en la barra, pero esto es más directo.) */
    fun openUrl(url: String): String {
        // Se delega al contexto; el servicio no lanza intents directamente.
        // Usar SearchWebExecutor desde el dispatcher que ya abre el navegador.
        return "usa_search_web_con_url:$url"
    }

    // ── helpers privados ─────────────────────────────────────────────────────

    private fun appendNode(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth.coerceAtMost(6))
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val cls = node.className?.toString()?.substringAfterLast('.') ?: ""
        val clickable = if (node.isClickable) " [tap]" else ""
        val editable = if (node.isEditable) " [input]" else ""

        val label = when {
            !text.isNullOrBlank() -> text
            !desc.isNullOrBlank() -> desc
            else -> null
        }
        if (label != null) {
            sb.appendLine("$indent$cls$clickable$editable: $label")
        } else if ((node.isClickable || node.isEditable) && cls.isNotBlank()) {
            sb.appendLine("$indent$cls$clickable$editable")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            appendNode(child, sb, depth + 1)
            child.recycle()
        }
    }

    private fun findNode(root: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val q = query.trim().lowercase()
        // Búsqueda por texto exacto primero
        var found = root.findAccessibilityNodeInfosByText(query).firstOrNull()
        if (found != null && found.isClickable) return found
        // Búsqueda por contenido parcial recorriendo el árbol
        found = findNodeRecursive(root, q)
        return found
    }

    private fun findNodeRecursive(node: AccessibilityNodeInfo, query: String): AccessibilityNodeInfo? {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if ((text.contains(query) || desc.contains(query)) && (node.isClickable || node.isEditable)) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeRecursive(child, query)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun findFocusedInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isFocused && root.isEditable) return AccessibilityNodeInfo.obtain(root)
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findFocusedInput(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun findNodeByClass(root: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (root.className?.toString() == className) return AccessibilityNodeInfo.obtain(root)
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findNodeByClass(child, className)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun findScrollable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return AccessibilityNodeInfo.obtain(root)
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findScrollable(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    companion object {
        var instance: NuevoSOAccessibilityService? = null
            private set

        fun isEnabled(): Boolean = instance != null
    }
}
