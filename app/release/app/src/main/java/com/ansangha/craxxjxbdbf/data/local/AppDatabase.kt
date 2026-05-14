package com.ansangha.craxxjxbdbf.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.ansangha.craxxjxbdbf.data.local.dao.AchievementDao
import com.ansangha.craxxjxbdbf.data.local.dao.HabitCompletionDao
import com.ansangha.craxxjxbdbf.data.local.dao.HabitDao
import com.ansangha.craxxjxbdbf.data.local.dao.RoutineDao
import com.ansangha.craxxjxbdbf.data.local.entity.AchievementEntity
import com.ansangha.craxxjxbdbf.data.local.entity.HabitCompletionEntity
import com.ansangha.craxxjxbdbf.data.local.entity.HabitEntity
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineBadgeEntity
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineCompletionEntity
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineTaskEntity
import com.ansangha.craxxjxbdbf.data.local.entity.UserProgressEntity
import com.ansangha.craxxjxbdbf.data.local.converters.DateConverters
import com.ansangha.craxxjxbdbf.data.local.converters.ListConverters

@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        AchievementEntity::class,
        RoutineTaskEntity::class,
        RoutineCompletionEntity::class,
        UserProgressEntity::class,
        RoutineBadgeEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(DateConverters::class, ListConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun routineDao(): RoutineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_tracker_database",
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
