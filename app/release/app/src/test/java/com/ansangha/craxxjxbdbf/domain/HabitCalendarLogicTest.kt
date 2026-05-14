package com.ansangha.craxxjxbdbf.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class HabitCalendarLogicTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    @Test
    fun rollover_when_last_completion_was_previous_calendar_day() {
        val today = ZonedDateTime.of(2026, 5, 13, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val yesterday = ZonedDateTime.of(2026, 5, 12, 22, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertTrue(HabitCalendarLogic.shouldResetDailyCompletionFlag(yesterday, today, zone))
    }

    @Test
    fun no_rollover_when_last_completion_same_local_day() {
        val morning = ZonedDateTime.of(2026, 5, 13, 8, 0, 0, 0, zone).toInstant().toEpochMilli()
        val evening = ZonedDateTime.of(2026, 5, 13, 20, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertFalse(HabitCalendarLogic.shouldResetDailyCompletionFlag(morning, evening, zone))
    }

    @Test
    fun streak_increments_for_consecutive_calendar_days() {
        val last = ZonedDateTime.of(2026, 5, 12, 23, 0, 0, 0, zone).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 5, 13, 1, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(4, HabitCalendarLogic.nextStreakDaysAfterCompletion(last, now, 3, zone))
    }

    @Test
    fun streak_resets_after_gap() {
        val last = ZonedDateTime.of(2026, 5, 10, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 5, 13, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(1, HabitCalendarLogic.nextStreakDaysAfterCompletion(last, now, 5, zone))
    }

    @Test
    fun first_completion_starts_streak_at_one() {
        val now = ZonedDateTime.of(2026, 5, 13, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(1, HabitCalendarLogic.nextStreakDaysAfterCompletion(0L, now, 0, zone))
    }
}
