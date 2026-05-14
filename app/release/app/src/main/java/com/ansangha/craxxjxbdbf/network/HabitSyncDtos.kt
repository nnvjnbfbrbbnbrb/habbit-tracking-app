package com.ansangha.craxxjxbdbf.network

import com.ansangha.craxxjxbdbf.data.local.entity.HabitEntity
import com.google.gson.annotations.SerializedName

/**
 * Stable JSON shape for POST /sync-habits (control server). Uses snake_case keys common in Node APIs.
 */
data class HabitSyncWireDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("streak_days") val streakDays: Int,
    @SerializedName("best_streak") val bestStreak: Int,
    @SerializedName("is_completed_today") val isCompletedToday: Boolean,
    @SerializedName("last_completed_at") val lastCompletedAt: Long,
    @SerializedName("current_count") val currentCount: Int,
    @SerializedName("target_count") val targetCount: Int,
    @SerializedName("is_active") val isActive: Boolean,
) {
    companion object {
        fun fromEntity(e: HabitEntity) = HabitSyncWireDto(
            id = e.id,
            name = e.name,
            description = e.description,
            category = e.category,
            frequency = e.frequency,
            streakDays = e.streakDays,
            bestStreak = e.bestStreak,
            isCompletedToday = e.isCompleted,
            lastCompletedAt = e.lastCompletedDate,
            currentCount = e.currentCount,
            targetCount = e.targetCount,
            isActive = e.isActive,
        )
    }
}
