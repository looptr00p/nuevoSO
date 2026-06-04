package com.nuevoso.launcher.agent.executors

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class SetAlarmExecutorTest {
    @Test
    fun relativeAlarmMinutesAreCalculatedFromLocalTime() {
        val base = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 1)
        }

        val alarmTime = SetAlarmExecutor.calculateRelativeAlarmTime(3, base)

        assertEquals(3, alarmTime.hour)
        assertEquals(4, alarmTime.minute)
    }

    @Test
    fun relativeAlarmMinutesCanCrossMidnight() {
        val base = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }

        val alarmTime = SetAlarmExecutor.calculateRelativeAlarmTime(3, base)

        assertEquals(0, alarmTime.hour)
        assertEquals(2, alarmTime.minute)
    }
}
