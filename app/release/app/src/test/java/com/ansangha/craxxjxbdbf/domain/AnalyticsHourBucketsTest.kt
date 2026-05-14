package com.ansangha.craxxjxbdbf.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class AnalyticsHourBucketsTest {

    @Test
    fun bucketCountsByLocalHour_groupsInto24Slots() {
        val zone = ZoneId.of("UTC")
        val t0 = java.time.ZonedDateTime.of(2026, 5, 14, 3, 30, 0, 0, zone).toInstant().toEpochMilli()
        val t1 = java.time.ZonedDateTime.of(2026, 5, 14, 3, 45, 0, 0, zone).toInstant().toEpochMilli()
        val t2 = java.time.ZonedDateTime.of(2026, 5, 14, 15, 0, 0, 0, zone).toInstant().toEpochMilli()
        val buckets = AnalyticsHourBuckets.bucketCountsByLocalHour(listOf(t0, t1, t2), zone)
        val expected = IntArray(24)
        expected[3] = 2
        expected[15] = 1
        assertArrayEquals(expected, buckets)
    }

    @Test
    fun hourOfDayUtcMillis_matchesWallClock() {
        val zone = ZoneId.of("America/New_York")
        val ms = java.time.ZonedDateTime.of(2026, 5, 14, 21, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(21, AnalyticsHourBuckets.hourOfDayUtcMillis(ms, zone))
    }
}
