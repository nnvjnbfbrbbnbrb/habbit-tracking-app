package com.ansangha.craxxjxbdbf.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object AnalyticsStreakLogic {

    /** Max consecutive local days present in [days] (sorted or unsorted). */
    fun maxConsecutiveLocalDays(days: Collection<LocalDate>): Int {
        if (days.isEmpty()) return 0
        val sorted = days.toSortedSet()
        var best = 1
        var cur = 1
        var prev: LocalDate? = null
        for (d in sorted) {
            if (prev == null) {
                prev = d
                continue
            }
            if (d == prev.plusDays(1)) {
                cur++
                best = maxOf(best, cur)
            } else if (d == prev) {
                // duplicate day — ignore
            } else {
                cur = 1
            }
            prev = d
        }
        return best
    }

    fun localDatesFromCompletionMillis(
        completedAtMillis: List<Long>,
        zone: ZoneId,
    ): Set<LocalDate> {
        return completedAtMillis.map { ms ->
            Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
        }.toSet()
    }
}
