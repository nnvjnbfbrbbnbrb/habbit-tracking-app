package com.ansangha.craxxjxbdbf.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineBadgeEntity
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineCompletionEntity
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineTaskEntity
import com.ansangha.craxxjxbdbf.data.local.entity.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routine_tasks ORDER BY timeMinutesFromMidnight ASC, name ASC")
    fun observeTasks(): Flow<List<RoutineTaskEntity>>

    @Query("SELECT * FROM routine_tasks ORDER BY timeMinutesFromMidnight ASC, name ASC")
    suspend fun getAllTasksOnce(): List<RoutineTaskEntity>

    @Query("SELECT * FROM routine_tasks WHERE enabled = 1")
    suspend fun getEnabledTasksOnce(): List<RoutineTaskEntity>

    @Query("SELECT * FROM routine_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): RoutineTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: RoutineTaskEntity): Long

    @Update
    suspend fun updateTask(task: RoutineTaskEntity)

    @Query("DELETE FROM routine_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query(
        """
        SELECT COUNT(*) FROM routine_completions
        WHERE taskId = :taskId AND dateEpochDay = :dateEpochDay AND missed = 0
        """,
    )
    suspend fun countCompletionsOnDay(taskId: Long, dateEpochDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(row: RoutineCompletionEntity): Long

    @Query(
        """
        SELECT dateEpochDay FROM routine_completions
        WHERE taskId = :taskId AND missed = 0
        GROUP BY dateEpochDay
        HAVING COUNT(*) >= :repeatCount
        ORDER BY dateEpochDay DESC
        """,
    )
    suspend fun getSatisfiedDaysDescending(taskId: Long, repeatCount: Int): List<Long>

    @Query(
        """
        SELECT * FROM routine_completions
        WHERE taskId = :taskId AND dateEpochDay BETWEEN :startDay AND :endDay
        ORDER BY completedAt ASC
        """,
    )
    suspend fun getCompletionsBetweenDays(
        taskId: Long,
        startDay: Long,
        endDay: Long,
    ): List<RoutineCompletionEntity>

    @Query("SELECT * FROM user_routine_progress WHERE id = 1")
    fun observeUserProgress(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_routine_progress WHERE id = 1")
    suspend fun getUserProgressOnce(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProgress(row: UserProgressEntity)

    @Query("SELECT * FROM routine_badges ORDER BY unlockedAt DESC")
    fun observeBadges(): Flow<List<RoutineBadgeEntity>>

    @Query("SELECT * FROM routine_badges ORDER BY unlockedAt DESC")
    suspend fun getBadgesOnce(): List<RoutineBadgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(row: RoutineBadgeEntity)

    @Transaction
    suspend fun ensureDefaultProgressRow() {
        if (getUserProgressOnce() == null) {
            val now = System.currentTimeMillis()
            upsertUserProgress(UserProgressEntity(id = 1, xp = 0, level = 1, lastUpdated = now))
        }
    }
}
