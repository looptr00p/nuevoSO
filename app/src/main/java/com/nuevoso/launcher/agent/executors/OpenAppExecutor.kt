package com.nuevoso.launcher.agent.executors

import android.content.Context
import com.nuevoso.launcher.data.apps.AppRepository

class OpenAppExecutor(
    private val context: Context,
    private val appRepository: AppRepository,
) {
    fun execute(appName: String): String {
        val app = appRepository.findByName(appName)
            ?: return "No encontré ninguna app que se llame \"$appName\"."
        val intent = appRepository.getLaunchIntent(app.packageName)
            ?: return "No pude abrir ${app.label}."
        context.startActivity(intent)
        return "${app.label} abierta."
    }
}
