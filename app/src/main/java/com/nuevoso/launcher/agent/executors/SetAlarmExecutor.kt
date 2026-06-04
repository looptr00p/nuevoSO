package com.nuevoso.launcher.agent.executors

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.util.Calendar

class SetAlarmExecutor(private val context: Context) {
    fun execute(hour: String?, minute: String?, delayMinutes: String?, label: String?): String {
        val alarmTime = when {
            !delayMinutes.isNullOrBlank() -> {
                val delay = delayMinutes.toIntOrNull() ?: return "Retraso inválido: $delayMinutes."
                calculateRelativeAlarmTime(delay)
            }
            !hour.isNullOrBlank() && !minute.isNullOrBlank() -> {
                val h = hour.toIntOrNull() ?: return "Hora inválida: $hour."
                val m = minute.toIntOrNull() ?: return "Minutos inválidos: $minute."
                AlarmTime(hour = h, minute = m)
            }
            else -> return "La alarma requiere una hora exacta o delay_minutes."
        }
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, alarmTime.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, alarmTime.minute)
            if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Alarma configurada para las ${alarmTime.hour}:${alarmTime.minute.toString().padStart(2, '0')}."
    }

    internal data class AlarmTime(val hour: Int, val minute: Int)

    companion object {
        internal fun calculateRelativeAlarmTime(delayMinutes: Int, base: Calendar = Calendar.getInstance()): AlarmTime {
            base.add(Calendar.MINUTE, delayMinutes)
            return AlarmTime(
                hour = base.get(Calendar.HOUR_OF_DAY),
                minute = base.get(Calendar.MINUTE),
            )
        }
    }
}
