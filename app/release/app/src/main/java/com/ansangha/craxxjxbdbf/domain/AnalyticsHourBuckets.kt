package com.ansangha.craxxjxbdbf.domain

/**
 * Pure bucketing for completion timestamps → local hour (0–23).
 * Used by tests and mirrors SQLite strftime('%H', ...) semantics for consistency checks.
 */
object AnalyticsHourBuckets {

    fun hourOfDayUtcMillis(utcMillis: Long, zone: java.time.ZoneId): Int {
        val zdt = java.time.Instant.ofEpochMilli(utcMillis).atZone(zone)
        return zdt.hour
    }

    /**
     * Returns length-24 counts where index = hour of local day.
     */
    fun bucketCountsByLocalHour(utcMillisList: List<Long>, zone: java.time.ZoneId): IntArray {
        val out = IntArray(24)
        for (ms in utcMillisList) {
            val h = hourOfDayUtcMillis(ms, zone)
            if (h in 0..23) out[h]++
        }
        return out
    }
}
