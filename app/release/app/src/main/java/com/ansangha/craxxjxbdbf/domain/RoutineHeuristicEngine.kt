package com.ansangha.craxxjxbdbf.domain

import com.ansangha.craxxjxbdbf.data.local.entity.RoutineCompletionEntity
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineTaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Lightweight heuristics over local DB rows — not ML and not marketed as AI.
 */
object RoutineHeuristicEngine {

    fun completionRateByHour(completions: List<RoutineCompletionEntity>): Map<Int, Double> {
        val zone = ZoneId.systemDefault()
        val counts = IntArray(24)
        for (c in completions) {
            if (c.missed) continue
            val h = Instant.ofEpochMilli(c.completedAt).atZone(zone).hour
            counts[h]++
        }
        val total = counts.sum().coerceAtLeast(1)
        return counts.indices.associateWith { counts[it].toDouble() / total }
    }

    fun suggestScheduleNudges(task: RoutineTaskEntity, completionsLast30d: List<RoutineCompletionEntity>): List<String> {
        val out = mutableListOf<String>()
        val mine = completionsLast30d.filter { it.taskId == task.id }
        val byHour = completionRateByHour(mine)
        val bestHour = byHour.maxByOrNull { it.value }?.key
        val currentHour = task.timeMinutesFromMidnight / 60
        if (bestHour != null && mine.size >= 5) {
            if (kotlin.math.abs(bestHour - currentHour) >= 2) {
                out.add(
                    "You often complete “${task.name}” around ${bestHour}:00. " +
                        "Consider shifting from ${currentHour}:00 if that fits your day better.",
                )
            }
        }
        val rate = recentScheduledSlotCompletionRate(task, mine, days = 14)
        val missP = RoutineGamification.predictMissProbabilityRecent(rate)
        if (missP >= 0.45) {
            out.add(
                "“${task.name}”: simple model flags a higher miss risk (~${(missP * 100).toInt()}%) over the last two weeks. " +
                    "Try a smaller repeat count or fewer days until it feels automatic.",
            )
        }
        if (out.isEmpty()) {
            out.add("“${task.name}”: keep going — not enough signal yet for strong schedule tweaks.")
        }
        return out
    }

    fun monthlyReportLines(
        taskNamesById: Map<Long, String>,
        completions: List<RoutineCompletionEntity>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<String> {
        val now = LocalDate.now(zone)
        val start = now.minusDays(29)
        val startDay = start.toEpochDay()
        val filtered = completions.filter { !it.missed && it.dateEpochDay in startDay..now.toEpochDay() }
        val byTask = filtered.groupingBy { it.taskId }.eachCount()
        val lines = mutableListOf<String>()
        lines += "Last 30 days: ${filtered.size} routine check-in(s) logged."
        val top = byTask.entries.sortedByDescending { it.value }.take(5)
        for (e in top) {
            val name = taskNamesById[e.key] ?: ("#" + e.key)
            lines += "• $name: ${e.value} logs"
        }
        return lines
    }

    private fun recentScheduledSlotCompletionRate(
        task: RoutineTaskEntity,
        completions: List<RoutineCompletionEntity>,
        days: Int,
    ): Double {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.minusDays(days.toLong())
        var scheduledSlots = 0
        var hitSlots = 0
        var d = start
        while (!d.isAfter(today)) {
            if (RoutineDayMask.isScheduled(
                    if (task.daysOfWeekMask == 0) RoutineDayMask.defaultWeekdayMask() else task.daysOfWeekMask,
                    d.dayOfWeek,
                )
            ) {
                scheduledSlots++
                val day = d.toEpochDay()
                val done = completions.count {
                    it.taskId == task.id && !it.missed && it.dateEpochDay == day
                } >= task.repeatCount
                if (done) hitSlots++
            }
            d = d.plusDays(1)
        }
        if (scheduledSlots == 0) return 1.0
        return hitSlots.toDouble() / scheduledSlots
    }

    fun hoursBetween(aMillis: Long, bMillis: Long): Long =
        ChronoUnit.HOURS.between(Instant.ofEpochMilli(aMillis), Instant.ofEpochMilli(bMillis))
}
