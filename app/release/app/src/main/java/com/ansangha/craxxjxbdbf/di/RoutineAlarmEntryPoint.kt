package com.ansangha.craxxjxbdbf.di

import com.ansangha.craxxjxbdbf.data.local.dao.RoutineDao
import com.ansangha.craxxjxbdbf.routine.RoutineAlarmScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RoutineAlarmEntryPoint {
    fun routineDao(): RoutineDao
    fun routineAlarmScheduler(): RoutineAlarmScheduler
}
