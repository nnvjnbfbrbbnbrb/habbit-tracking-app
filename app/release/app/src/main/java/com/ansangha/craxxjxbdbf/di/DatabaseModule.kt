package com.ansangha.craxxjxbdbf.di

import android.content.Context
import androidx.room.Room
import com.ansangha.craxxjxbdbf.data.local.AppDatabase
import com.ansangha.craxxjxbdbf.data.local.MIGRATION_1_2
import com.ansangha.craxxjxbdbf.data.local.dao.AchievementDao
import com.ansangha.craxxjxbdbf.data.local.dao.HabitCompletionDao
import com.ansangha.craxxjxbdbf.data.local.dao.HabitDao
import com.ansangha.craxxjxbdbf.data.local.dao.RoutineDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "habit_tracker_database",
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideHabitDao(database: AppDatabase): HabitDao {
        return database.habitDao()
    }

    @Provides
    fun provideHabitCompletionDao(database: AppDatabase): HabitCompletionDao {
        return database.habitCompletionDao()
    }

    @Provides
    fun provideAchievementDao(database: AppDatabase): AchievementDao {
        return database.achievementDao()
    }

    @Provides
    fun provideRoutineDao(database: AppDatabase): RoutineDao {
        return database.routineDao()
    }
}