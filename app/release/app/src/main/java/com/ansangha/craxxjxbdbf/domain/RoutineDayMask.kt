package com.ansangha.craxxjxbdbf.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object RoutineDayMask {

    fun bitFor(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun isScheduled(mask: Int, day: DayOfWeek): Boolean = (mask and bitFor(day)) != 0

    fun defaultWeekdayMask(): Int = (1 shl 7) - 1 // Mon–Sun all days

    fun label(mask: Int): String {
        if (mask == 0) return "None"
        val parts = mutableListOf<String>()
        for (d in DayOfWeek.entries) {
            if (isScheduled(mask, d)) {
                parts.add(d.name.take(3).lowercase().replaceFirstChar { it.titlecase() })
            }
        }
        return parts.joinToString(", ")
    }
}

object RoutineScheduleCalculator {

    /**
     * Next instant at or after [fromMillis] where the local calendar day is allowed by [daysOfWeekMask]
     * and local wall-clock is [timeMinutesFromMidnight].
     */
    fun nextFireMillis(
        daysOfWeekMask: Int,
        timeMinutesFromMidnight: Int,
        fromMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val mask = if (daysOfWeekMask == 0) RoutineDayMask.defaultWeekdayMask() else daysOfWeekMask
        val hour = timeMinutesFromMidnight / 60
        val minute = timeMinutesFromMidnight % 60
        val targetTime = LocalTime.of(hour, minute)
        var cursor = ZonedDateTime.ofInstant(Instant.ofEpochMilli(fromMillis), zone).toLocalDate()
        // Search up to 10 days ahead
        repeat(10) {
            val dow = DayOfWeek.from(cursor)
            if (RoutineDayMask.isScheduled(mask, dow)) {
                val candidate = ZonedDateTime.of(cursor, targetTime, zone)
                val ms = candidate.toInstant().toEpochMilli()
                if (ms > fromMillis) {
                    return ms
                }
            }
            cursor = cursor.plusDays(1)
        }
        // Fallback: tomorrow same time even if mask broken
        val z = ZonedDateTime.ofInstant(Instant.ofEpochMilli(fromMillis), zone)
        return z.plusDays(1).withHour(hour).withMinute(minute).withSecond(0).withNano(0).toInstant().toEpochMilli()
    }

    fun todayEpochDay(zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).toEpochDay()
}
