package com.nuevoso.launcher.agent

import android.content.Context
import com.nuevoso.launcher.agent.executors.DialExecutor
import com.nuevoso.launcher.agent.executors.OpenAppExecutor
import com.nuevoso.launcher.agent.executors.SearchWebExecutor
import com.nuevoso.launcher.agent.executors.SetAlarmExecutor
import com.nuevoso.launcher.agent.executors.ToggleSettingExecutor
import com.nuevoso.launcher.ai.ToolCall
import com.nuevoso.launcher.ai.ToolResult
import com.nuevoso.launcher.data.apps.AppRepository
import com.nuevoso.launcher.data.memory.MemoryRepository

class ActionDispatcher(
    context: Context,
    appRepository: AppRepository,
    private val memoryRepository: MemoryRepository,
) {
    private val openApp = OpenAppExecutor(context, appRepository)
    private val searchWeb = SearchWebExecutor(context)
    private val setAlarm = SetAlarmExecutor(context)
    private val dial = DialExecutor(context)
    private val toggle = ToggleSettingExecutor(context)

    suspend fun dispatch(call: ToolCall): ToolResult {
        val result = try {
            when (call.name) {
                "open_app" -> openApp.execute(call.args["app_name"] ?: "")
                "search_web" -> searchWeb.execute(call.args["query"] ?: "")
                "set_alarm" -> setAlarm.execute(
                    hour = call.args["hour"] ?: "0",
                    minute = call.args["minute"] ?: "0",
                    label = call.args["label"],
                )
                "call" -> dial.execute(call.args["target"] ?: "")
                "toggle_setting" -> toggle.execute(
                    setting = call.args["setting"] ?: "",
                    value = call.args["value"],
                )
                "remember_fact" -> {
                    val fact = call.args["fact"] ?: ""
                    if (fact.isNotBlank()) memoryRepository.rememberFact(fact)
                    "Recordado."
                }
                else -> "Herramienta desconocida: ${call.name}"
            }
        } catch (e: Exception) {
            // No tumbar el turno: devolver el error como resultado para que el modelo reaccione.
            "Error: ${e.message ?: "fallo al ejecutar ${call.name}"}"
        }
        return ToolResult(id = call.id, result = result)
    }
}
