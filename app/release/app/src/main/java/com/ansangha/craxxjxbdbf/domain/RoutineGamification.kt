package com.ansangha.craxxjxbdbf.domain

import kotlin.math.roundToInt

/**
 * XP / level curve and per-task streak from satisfied schedule days.
 * Level-up uses a simple escalating threshold ladder (no external deps).
 */
object RoutineGamification {

    const val XP_PER_DAY_TARGET_HIT: Int = 35

    fun levelFromXp(xp: Int): Int {
        var level = 1
        var pool = xp.coerceAtLeast(0)
        var step = 120
        while (pool >= step) {
            pool -= step
            level += 1
            step = (step * 1.2).roundToInt().coerceAtMost(50_000)
        }
        return level
    }

    fun xpProgressInCurrentLevel(xp: Int): Pair<Int, Int> {
        val level = levelFromXp(xp)
        var remainder = xp.coerceAtLeast(0)
        var step = 120
        repeat(level - 1) {
            remainder -= step
            step = (step * 1.2).roundToInt().coerceAtMost(50_000)
        }
        val need = step
        val into = remainder.coerceIn(0, need)
        return into to need
    }

    /**
     * [satisfiedEpochDaysDescending] must be distinct days descending (newest first),
     * only days where the user hit [repeatCount] completions.
     */
    fun streakFromSatisfiedDays(
        satisfiedEpochDaysDescending: List<Long>,
        repeatCount: Int,
        todayEpochDay: Long,
        daysOfWeekMask: Int,
    ): Int {
        if (repeatCount <= 0) return 0
        val satisfied = satisfiedEpochDaysDescending.toSet()
        var streak = 0
        var day = todayEpochDay
        // Walk back at most 730 local days
        repeat(730) {
            val date = java.time.LocalDate.ofEpochDay(day)
            val dow = date.dayOfWeek
            val scheduled = RoutineDayMask.isScheduled(
                if (daysOfWeekMask == 0) RoutineDayMask.defaultWeekdayMask() else daysOfWeekMask,
                dow,
            )
            when {
                !scheduled -> { /* neutral */ }
                satisfied.contains(day) -> streak++
                else -> return streak
            }
            day -= 1
        }
        return streak
    }

    fun predictMissProbabilityRecent(
        completionRateLast7ScheduledSlots: Double,
    ): Double {
        val p = 1.0 - completionRateLast7ScheduledSlots.coerceIn(0.0, 1.0)
        return p.coerceIn(0.0, 1.0)
    }
}
