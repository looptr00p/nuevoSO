package com.nuevoso.launcher.agent.executors

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

class CalendarEventExecutor(private val context: Context) {
    fun execute(args: Map<String, String>): String {
        val title = args["title"]?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return "El evento requiere un título."
        val startHour = args["start_hour"]?.toIntOrNull()
            ?: return "El evento requiere hora de inicio válida."
        val startMinute = args["start_minute"]?.toIntOrNull()
            ?: return "El evento requiere minuto de inicio válido."
        val endHour = args["end_hour"]?.toIntOrNull()
            ?: return "El evento requiere hora de término válida."
        val endMinute = args["end_minute"]?.toIntOrNull()
            ?: return "El evento requiere minuto de término válido."
        val date = resolveDate(day = args["day"], date = args["date"])
            ?: return "El evento requiere un día relativo o una fecha válida."
        val eventTimes = calculateEventTimes(
            date = date,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
        ) ?: return "El evento requiere una hora de término posterior al inicio."

        val intent = Intent(Intent.ACTION_INSERT).apply {
            setDataAndType(CalendarContract.Events.CONTENT_URI, EVENT_MIME_TYPE)
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventTimes.beginMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventTimes.endMillis)
            args["location"]?.takeIf { it.isNotBlank() }?.let {
                putExtra(CalendarContract.Events.EVENT_LOCATION, it)
            }
            args["description"]?.takeIf { it.isNotBlank() }?.let {
                putExtra(CalendarContract.Events.DESCRIPTION, it)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Evento de calendario preparado: $title."
    }

    private fun resolveDate(day: String?, date: String?): LocalDate? {
        return when (day?.trim()?.lowercase()) {
            "today" -> LocalDate.now()
            "tomorrow" -> LocalDate.now().plusDays(1)
            else -> parseDate(date)
        }
    }

    private fun parseDate(date: String?): LocalDate? {
        return try {
            date?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    internal data class EventTimes(val beginMillis: Long, val endMillis: Long)

    companion object {
        private const val EVENT_MIME_TYPE = "vnd.android.cursor.item/event"

        internal fun calculateEventTimes(
            date: LocalDate,
            startHour: Int,
            startMinute: Int,
            endHour: Int,
            endMinute: Int,
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): EventTimes? {
            if (startHour !in 0..23 || endHour !in 0..23) return null
            if (startMinute !in 0..59 || endMinute !in 0..59) return null
            val start = LocalDateTime.of(date.year, date.month, date.dayOfMonth, startHour, startMinute)
            val end = LocalDateTime.of(date.year, date.month, date.dayOfMonth, endHour, endMinute)
            if (!end.isAfter(start)) return null
            return EventTimes(
                beginMillis = start.atZone(zoneId).toInstant().toEpochMilli(),
                endMillis = end.atZone(zoneId).toInstant().toEpochMilli(),
            )
        }
    }
}
