package com.ansangha.craxxjxbdbf.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ansangha.craxxjxbdbf.data.local.entity.AchievementEntity
import com.ansangha.craxxjxbdbf.data.local.entity.HabitCompletionEntity
import com.ansangha.craxxjxbdbf.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY priority DESC")
    fun getActiveHabits(): Flow<List<HabitEntity>>
    
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    suspend fun getAllHabitsOnce(): List<HabitEntity>
    
    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: String): HabitEntity?
    
    /** Active habits not yet marked complete (used for gentle routine reminders). */
    @Query("SELECT * FROM habits WHERE isActive = 1 AND isCompleted = 0 ORDER BY priority DESC, name ASC")
    suspend fun getIncompleteActiveHabits(): List<HabitEntity>

    @Query("SELECT COUNT(*) FROM habits WHERE isActive = 1")
    suspend fun countActiveHabits(): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)
    
    @Update
    suspend fun updateHabit(habit: HabitEntity)
    
    @Delete
    suspend fun deleteHabit(habit: HabitEntity)
}

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions ORDER BY completedAt DESC")
    suspend fun getAllCompletions(): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedAt DESC")
    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY completedAt ASC")
    suspend fun getCompletionsForHabitOnce(habitId: String): List<HabitCompletionEntity>
    
    @Query("SELECT COUNT(*) FROM habit_completions WHERE habitId = :habitId")
    suspend fun getCompletionCount(habitId: String): Int
    
    @Query("SELECT * FROM habit_completions WHERE completedAt >= :startInclusive AND completedAt < :endExclusive ORDER BY completedAt DESC")
    suspend fun getCompletionsBetween(startInclusive: Long, endExclusive: Long): List<HabitCompletionEntity>

    @Query("SELECT COUNT(DISTINCT habitId) FROM habit_completions WHERE completedAt >= :startInclusive AND completedAt < :endExclusive")
    suspend fun countDistinctHabitsCompletedBetween(startInclusive: Long, endExclusive: Long): Int

    @Query("SELECT COUNT(*) FROM habit_completions")
    suspend fun countTotalCompletions(): Int

    @Query(
        """
        SELECT strftime('%Y-%m-%d', completedAt / 1000, 'unixepoch', 'localtime') AS day,
               COUNT(DISTINCT habitId) AS habitsDone
        FROM habit_completions
        WHERE completedAt >= :startInclusive AND completedAt < :endExclusive
        GROUP BY day
        ORDER BY day ASC
        """,
    )
    suspend fun countDistinctHabitsByLocalDay(
        startInclusive: Long,
        endExclusive: Long,
    ): List<DayHabitCount>

    @Query(
        """
        SELECT habitId AS habitId,
               strftime('%Y-%m-%d', completedAt / 1000, 'unixepoch', 'localtime') AS day,
               COUNT(*) AS completions
        FROM habit_completions
        WHERE completedAt >= :startInclusive AND completedAt < :endExclusive
        GROUP BY habitId, day
        ORDER BY day ASC
        """,
    )
    suspend fun completionsByHabitAndLocalDay(
        startInclusive: Long,
        endExclusive: Long,
    ): List<HabitDayCompletionCount>

    @Query(
        """
        SELECT CAST(strftime('%H', completedAt / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hour,
               COUNT(*) AS count
        FROM habit_completions
        WHERE completedAt >= :startInclusive AND completedAt < :endExclusive
        GROUP BY hour
        ORDER BY hour ASC
        """,
    )
    suspend fun countByLocalHour(
        startInclusive: Long,
        endExclusive: Long,
    ): List<HourCountRow>

    @Query(
        """
        SELECT habitId AS habitId,
               CAST(strftime('%H', completedAt / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hour,
               COUNT(*) AS count
        FROM habit_completions
        WHERE completedAt >= :startInclusive AND completedAt < :endExclusive
        GROUP BY habitId, hour
        ORDER BY habitId ASC, hour ASC
        """,
    )
    suspend fun countByHabitLocalHour(
        startInclusive: Long,
        endExclusive: Long,
    ): List<HabitHourCountRow>

    @Query(
        """
        SELECT AVG(mood) AS avgMood, COUNT(*) AS samples
        FROM habit_completions
        WHERE completedAt >= :startInclusive AND completedAt < :endExclusive
          AND mood > 0
        """,
    )
    suspend fun moodAggregateBetween(startInclusive: Long, endExclusive: Long): MoodAggregateRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity)
    
    @Delete
    suspend fun deleteCompletion(completion: HabitCompletionEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun countAchievements(): Int

    @Query("SELECT * FROM achievements ORDER BY category ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>
    
    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements ORDER BY category ASC")
    suspend fun getAllAchievementsOnce(): List<AchievementEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)
    
    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)
}

data class DayHabitCount(
    @ColumnInfo(name = "day") val day: String,
    @ColumnInfo(name = "habitsDone") val habitsDone: Int,
)

data class HabitDayCompletionCount(
    @ColumnInfo(name = "habitId") val habitId: String,
    @ColumnInfo(name = "day") val day: String,
    @ColumnInfo(name = "completions") val completions: Int,
)

data class HourCountRow(
    @ColumnInfo(name = "hour") val hour: Int,
    @ColumnInfo(name = "count") val count: Int,
)

data class HabitHourCountRow(
    @ColumnInfo(name = "habitId") val habitId: String,
    @ColumnInfo(name = "hour") val hour: Int,
    @ColumnInfo(name = "count") val count: Int,
)

data class MoodAggregateRow(
    @ColumnInfo(name = "avgMood") val avgMood: Double?,
    @ColumnInfo(name = "samples") val samples: Int,
)