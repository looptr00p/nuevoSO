package com.nuevoso.launcher.agent.executors

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

class SetAlarmExecutor(private val context: Context) {
    fun execute(hour: String, minute: String, label: String?): String {
        val h = hour.toIntOrNull() ?: return "Hora inválida: $hour."
        val m = minute.toIntOrNull() ?: return "Minutos inválidos: $minute."
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, h)
            putExtra(AlarmClock.EXTRA_MINUTES, m)
            if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Alarma configurada para las ${h}:${m.toString().padStart(2, '0')}."
    }
}
