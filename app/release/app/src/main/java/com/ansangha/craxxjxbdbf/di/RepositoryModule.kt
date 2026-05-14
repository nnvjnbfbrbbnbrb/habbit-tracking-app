package com.ansangha.craxxjxbdbf.di

import com.ansangha.craxxjxbdbf.data.local.dao.RoutineDao
import com.ansangha.craxxjxbdbf.repository.HabitRepository
import com.ansangha.craxxjxbdbf.repository.RoutineRepository
import com.ansangha.craxxjxbdbf.routine.RoutineAlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideHabitRepository(
        habitDao: com.ansangha.craxxjxbdbf.data.local.dao.HabitDao,
        completionDao: com.ansangha.craxxjxbdbf.data.local.dao.HabitCompletionDao,
        achievementDao: com.ansangha.craxxjxbdbf.data.local.dao.AchievementDao
    ): HabitRepository {
        return HabitRepository(habitDao, completionDao, achievementDao)
    }

    @Provides
    @Singleton
    fun provideRoutineRepository(
        routineDao: RoutineDao,
        routineAlarmScheduler: RoutineAlarmScheduler,
    ): RoutineRepository {
        return RoutineRepository(routineDao, routineAlarmScheduler)
    }
}