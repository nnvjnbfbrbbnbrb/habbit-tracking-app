package com.ansangha.craxxjxbdbf.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_badges")
data class RoutineBadgeEntity(
    @PrimaryKey val badgeId: String,
    val unlockedAt: Long,
)
