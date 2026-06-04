package com.nuevoso.launcher.agent.executors

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarEventExecutorTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun calculatesCalendarEventInterval() {
        val eventTimes = CalendarEventExecutor.calculateEventTimes(
            date = LocalDate.of(2026, 6, 5),
            startHour = 18,
            startMinute = 0,
            endHour = 21,
            endMinute = 0,
            zoneId = zone,
        )

        assertNotNull(eventTimes)
        assertEquals("2026-06-05T18:00:00Z", Instant.ofEpochMilli(eventTimes!!.beginMillis).toString())
        assertEquals("2026-06-05T21:00:00Z", Instant.ofEpochMilli(eventTimes.endMillis).toString())
    }

    @Test
    fun rejectsCalendarEventEndBeforeStart() {
        val eventTimes = CalendarEventExecutor.calculateEventTimes(
            date = LocalDate.of(2026, 6, 5),
            startHour = 21,
            startMinute = 0,
            endHour = 18,
            endMinute = 0,
            zoneId = zone,
        )

        assertNull(eventTimes)
    }
}
