package com.ansangha.craxxjxbdbf.di

import com.ansangha.craxxjxbdbf.data.preferences.RoutineModesPreferences
import com.ansangha.craxxjxbdbf.data.preferences.UserUiPreferences
import com.ansangha.craxxjxbdbf.network.HabitProBridgeClient
import com.ansangha.craxxjxbdbf.repository.AnalyticsRepository
import com.ansangha.craxxjxbdbf.repository.HabitRepository
import com.ansangha.craxxjxbdbf.repository.RoutineRepository
import com.ansangha.craxxjxbdbf.routine.SleepWakeScheduler
import com.ansangha.craxxjxbdbf.safety.usage.UsageStatsRepository
import com.ansangha.craxxjxbdbf.sync.BridgeAnalyticsSync
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun habitRepository(): HabitRepository
    fun routineRepository(): RoutineRepository
    fun routineModesPreferences(): RoutineModesPreferences
    fun sleepWakeScheduler(): SleepWakeScheduler
    fun userUiPreferences(): UserUiPreferences
    fun habitProBridgeClient(): HabitProBridgeClient
    fun usageStatsRepository(): UsageStatsRepository
    fun analyticsRepository(): AnalyticsRepository
    fun bridgeAnalyticsSync(): BridgeAnalyticsSync
}
