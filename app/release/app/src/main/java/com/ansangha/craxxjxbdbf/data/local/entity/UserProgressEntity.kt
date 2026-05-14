package com.ansangha.craxxjxbdbf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_routine_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 1,
    val xp: Int,
    val level: Int,
    val lastUpdated: Long,
)
