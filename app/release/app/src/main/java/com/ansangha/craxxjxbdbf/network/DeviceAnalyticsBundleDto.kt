package com.ansangha.craxxjxbdbf.network

import com.google.gson.annotations.SerializedName

data class CompletionWireDto(
    @SerializedName("habit_id") val habitId: String,
    @SerializedName("completed_at") val completedAt: Long,
    @SerializedName("mood") val mood: Int,
)

data class DeviceAnalyticsBundleDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("habits") val habits: List<HabitSyncWireDto>,
    @SerializedName("completions") val completions: List<CompletionWireDto>,
    @SerializedName("usage_screen_summary") val usageScreenSummary: String? = null,
    @SerializedName("sleep_bed_hour") val sleepBedHour: Int? = null,
    @SerializedName("sleep_wake_hour") val sleepWakeHour: Int? = null,
)
