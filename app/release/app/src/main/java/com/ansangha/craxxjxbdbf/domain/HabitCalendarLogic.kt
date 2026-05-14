package com.ansangha.craxxjxbdbf.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Calendar-day habit rules (device local timezone). Pure functions for tests and repository use.
 */
object HabitCalendarLogic {

    fun localDate(zone: ZoneId, epochMillis: Long): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    /**
     * After a new local calendar day, a habit marked completed whose last completion was **before** today
     * should show as open again for daily tracking.
     */
    fun shouldResetDailyCompletionFlag(lastCompletedDateMillis: Long, nowMillis: Long, zone: ZoneId): Boolean {
        if (lastCompletedDateMillis <= 0L) return false
        val last = localDate(zone, lastCompletedDateMillis)
        val today = localDate(zone, nowMillis)
        return last.isBefore(today)
    }

    /**
     * Next streak length after logging a completion at [nowMillis], given the previous completion instant.
     */
    fun nextStreakDaysAfterCompletion(
        lastCompletedDateMillisBeforeThisCompletion: Long,
        nowMillis: Long,
        currentStreakDays: Int,
        zone: ZoneId,
    ): Int {
        if (lastCompletedDateMillisBeforeThisCompletion <= 0L) return 1
        val last = localDate(zone, lastCompletedDateMillisBeforeThisCompletion)
        val today = localDate(zone, nowMillis)
        return when {
            last == today -> currentStreakDays.coerceAtLeast(1)
            last == today.minusDays(1) -> currentStreakDays + 1
            else -> 1
        }
    }
}
