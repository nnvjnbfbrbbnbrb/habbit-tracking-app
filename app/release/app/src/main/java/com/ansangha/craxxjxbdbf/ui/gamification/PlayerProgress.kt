package com.ansangha.craxxjxbdbf.ui.gamification

import com.ansangha.craxxjxbdbf.data.local.entity.HabitEntity

object PlayerProgress {
    fun totalXp(habits: List<HabitEntity>): Int =
        habits.sumOf { h ->
            h.completedDates.size * 22 + h.bestStreak * 9 + h.streakDays * 3
        }

    fun levelFromXp(xp: Int): Int = (xp / 220).coerceAtLeast(0) + 1

    /** Current segment progress within level: filled to next threshold. */
    fun levelProgress(xp: Int): Pair<Int, Int> {
        val level = levelFromXp(xp)
        val start = (level - 1) * 220
        val span = 220
        val into = (xp - start).coerceIn(0, span)
        return into to span
    }
}
