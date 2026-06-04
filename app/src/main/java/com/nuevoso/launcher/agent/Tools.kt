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
        description = "Abre el panel de ajustes del teléfono para cambiar una configuración (Wi-Fi, Bluetooth, brillo, etc.) o controla la linterna con un valor explícito.",
        parameters = mapOf(
            "setting" to ParamSpec(
                type = "string",
                description = "Ajuste a cambiar",
                enum = listOf("wifi", "bluetooth", "data", "brightness", "sound", "airplane", "flashlight"),
            ),
            "value" to ParamSpec("string", "Obligatorio para flashlight: on para encender u off para apagar", enum = listOf("on", "off")),
        ),
        required = listOf("setting"),
    ),
    ToolSpec(
        name = "install_app",
        description = "Abre Play Store buscando una app para instalarla. Después usa read_screen para ver los resultados y tap_element('Instalar') para completar la instalación.",
        parameters = mapOf(
            "app_name" to ParamSpec("string", "Nombre de la app a instalar, ej: WhatsApp, Spotify, Chrome"),
        ),
        required = listOf("app_name"),
    ),
    ToolSpec(
        name = "read_screen",
        description = "Lee el contenido de la pantalla actual del teléfono: texto visible, botones, inputs y elementos interactivos de cualquier app abierta. Úsala para saber qué hay en pantalla antes de interactuar.",
        parameters = emptyMap(),
        required = emptyList(),
    ),
    ToolSpec(
        name = "tap_element",
        description = "Hace tap en un elemento visible en pantalla buscándolo por su texto o descripción. Úsala para pulsar botones, links o elementos de menú en cualquier app.",
        parameters = mapOf(
            "description" to ParamSpec("string", "Texto o descripción del elemento a pulsar, ej: 'Buscar', 'Confirmar pedido', 'Ver más'"),
        ),
        required = listOf("description"),
    ),
    ToolSpec(
        name = "type_text",
        description = "Escribe texto en el campo de entrada activo o visible en pantalla. Úsala para rellenar formularios, barras de búsqueda o cualquier input.",
        parameters = mapOf(
            "text" to ParamSpec("string", "Texto a escribir en el campo"),
        ),
        required = listOf("text"),
    ),
    ToolSpec(
        name = "scroll_screen",
        description = "Desplaza la pantalla hacia arriba o abajo para ver más contenido.",
        parameters = mapOf(
            "direction" to ParamSpec("string", "Dirección del scroll", enum = listOf("down", "up")),
        ),
        required = listOf("direction"),
    ),
    ToolSpec(
        name = "press_back",
        description = "Pulsa el botón Atrás del sistema para navegar a la pantalla anterior.",
        parameters = emptyMap(),
        required = emptyList(),
    ),
    ToolSpec(
        name = "list_apps",
        description = "Devuelve la lista completa de aplicaciones instaladas en el teléfono. Úsala cuando el usuario pregunte qué apps tiene, quiera buscar una app o necesites saber si una app está instalada antes de abrirla.",
        parameters = emptyMap(),
        required = emptyList(),
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
