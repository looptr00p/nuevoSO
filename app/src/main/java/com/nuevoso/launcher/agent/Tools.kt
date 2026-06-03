package com.nuevoso.launcher.agent

import com.nuevoso.launcher.ai.ParamSpec
import com.nuevoso.launcher.ai.ToolSpec

val ALL_TOOLS = listOf(
    ToolSpec(
        name = "open_app",
        description = "Abre una aplicación instalada en el teléfono por nombre.",
        parameters = mapOf(
            "app_name" to ParamSpec("string", "Nombre de la app a abrir, ej: WhatsApp, Spotify, Cámara"),
        ),
        required = listOf("app_name"),
    ),
    ToolSpec(
        name = "search_web",
        description = "Busca algo en internet usando el navegador del teléfono.",
        parameters = mapOf(
            "query" to ParamSpec("string", "Texto a buscar"),
        ),
        required = listOf("query"),
    ),
    ToolSpec(
        name = "set_alarm",
        description = "Pone una alarma a la hora indicada.",
        parameters = mapOf(
            "hour" to ParamSpec("string", "Hora en formato 24h, ej: 7"),
            "minute" to ParamSpec("string", "Minutos, ej: 30"),
            "label" to ParamSpec("string", "Etiqueta opcional de la alarma"),
        ),
        required = listOf("hour", "minute"),
    ),
    ToolSpec(
        name = "call",
        description = "Abre el marcador para llamar a un número o contacto.",
        parameters = mapOf(
            "target" to ParamSpec("string", "Nombre del contacto o número de teléfono"),
        ),
        required = listOf("target"),
    ),
    ToolSpec(
        name = "toggle_setting",
        description = "Abre el panel de ajustes del teléfono para cambiar una configuración (Wi-Fi, Bluetooth, brillo, etc.) o activa/desactiva la linterna directamente.",
        parameters = mapOf(
            "setting" to ParamSpec(
                type = "string",
                description = "Ajuste a cambiar",
                enum = listOf("wifi", "bluetooth", "data", "brightness", "sound", "airplane", "flashlight"),
            ),
            "value" to ParamSpec("string", "on o off (solo para linterna)"),
        ),
        required = listOf("setting"),
    ),
    ToolSpec(
        name = "remember_fact",
        description = "Guarda un hecho importante sobre el usuario en la memoria local del teléfono para recordarlo en futuras conversaciones.",
        parameters = mapOf(
            "fact" to ParamSpec("string", "El hecho a recordar sobre el usuario, en tercera persona. Ej: Al usuario le gusta escuchar música clásica al trabajar."),
        ),
        required = listOf("fact"),
    ),
)
