package com.ansangha.craxxjxbdbf.di

import com.ansangha.craxxjxbdbf.data.preferences.RoutineModesPreferences
import com.ansangha.craxxjxbdbf.repository.RoutineRepository
import com.ansangha.craxxjxbdbf.routine.SleepWakeScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RoutineBootEntryPoint {
    fun routineRepository(): RoutineRepository
    fun routineModesPreferences(): RoutineModesPreferences
    fun sleepWakeScheduler(): SleepWakeScheduler
}
