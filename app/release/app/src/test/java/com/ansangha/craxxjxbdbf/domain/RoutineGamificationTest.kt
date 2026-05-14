package com.ansangha.craxxjxbdbf.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineGamificationTest {

    @Test
    fun levelFromXp_increases() {
        assertEquals(1, RoutineGamification.levelFromXp(0))
        assertEquals(2, RoutineGamification.levelFromXp(120))
    }

    @Test
    fun streak_counts_consecutive_scheduled_days() {
        val mask = RoutineDayMask.defaultWeekdayMask()
        val streak = RoutineGamification.streakFromSatisfiedDays(
            satisfiedEpochDaysDescending = listOf(100L, 99L, 98L),
            repeatCount = 1,
            todayEpochDay = 100L,
            daysOfWeekMask = mask,
        )
        assertEquals(3, streak)
    }
}
