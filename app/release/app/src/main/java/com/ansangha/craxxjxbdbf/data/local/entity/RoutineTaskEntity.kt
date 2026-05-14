package com.ansangha.craxxjxbdbf.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_tasks",
    indices = [Index(value = ["enabled"])],
)
data class RoutineTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** Minutes from midnight [0, 1439]. */
    val timeMinutesFromMidnight: Int,
    /** Target completions on each scheduled day. */
    val repeatCount: Int,
    /** Bitmask: bit (dayOfWeek-1) for ISO Monday=1 … Sunday=7. */
    val daysOfWeekMask: Int,
    val enabled: Boolean,
    /** Minutes after scheduled time before a miss is recorded for analytics. */
    val graceMinutes: Int,
    val createdAt: Long,
)
