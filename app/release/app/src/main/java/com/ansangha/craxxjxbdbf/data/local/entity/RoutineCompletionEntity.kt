package com.ansangha.craxxjxbdbf.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine_completions",
    foreignKeys = [
        ForeignKey(
            entity = RoutineTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId"), Index(value = ["taskId", "dateEpochDay"])],
)
data class RoutineCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    /** LocalDate.toEpochDay() in system default zone semantics. */
    val dateEpochDay: Long,
    val completedAt: Long,
    val missed: Boolean,
)
