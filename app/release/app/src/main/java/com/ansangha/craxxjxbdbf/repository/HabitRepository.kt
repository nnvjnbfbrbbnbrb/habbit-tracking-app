package com.ansangha.craxxjxbdbf.repository

import com.ansangha.craxxjxbdbf.data.local.dao.AchievementDao
import com.ansangha.craxxjxbdbf.data.local.dao.HabitCompletionDao
import com.ansangha.craxxjxbdbf.data.local.dao.HabitDao
import com.ansangha.craxxjxbdbf.data.local.entity.AchievementEntity
import com.ansangha.craxxjxbdbf.data.local.entity.HabitCompletionEntity
import com.ansangha.craxxjxbdbf.data.local.entity.HabitEntity
import com.ansangha.craxxjxbdbf.domain.HabitCalendarLogic
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
    private val achievementDao: AchievementDao,
) {

    fun getAllHabits(): Flow<List<HabitEntity>> = habitDao.getAllHabits()

    suspend fun getAllHabitsOnce(): List<HabitEntity> = habitDao.getAllHabitsOnce()
    fun getActiveHabits(): Flow<List<HabitEntity>> = habitDao.getActiveHabits()

    suspend fun getHabitById(id: String): HabitEntity? = habitDao.getHabitById(id)

    suspend fun getIncompleteActiveHabits(): List<HabitEntity> = habitDao.getIncompleteActiveHabits()

    suspend fun insertHabit(habit: HabitEntity) = habitDao.insertHabit(habit)
    suspend fun updateHabit(habit: HabitEntity) = habitDao.updateHabit(habit)
    suspend fun deleteHabit(habit: HabitEntity) = habitDao.deleteHabit(habit)

    fun getCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>> =
        completionDao.getCompletionsForHabit(habitId)

    suspend fun getCompletionCount(habitId: String): Int = completionDao.getCompletionCount(habitId)

    suspend fun getTodayCompletionsForLocalDay(): List<HabitCompletionEntity> {
        val (start, end) = localDayBoundsMillis()
        return completionDao.getCompletionsBetween(start, end)
    }

    suspend fun distinctHabitsCompletedToday(): Int {
        val (start, end) = localDayBoundsMillis()
        return completionDao.countDistinctHabitsCompletedBetween(start, end)
    }

    suspend fun countActiveHabits(): Int = habitDao.countActiveHabits()

    suspend fun getAllCompletions(): List<HabitCompletionEntity> =
        completionDao.getAllCompletions()

    suspend fun insertCompletion(completion: HabitCompletionEntity) = completionDao.insertCompletion(completion)

    suspend fun deleteCompletion(completion: HabitCompletionEntity) = completionDao.deleteCompletion(completion)

    fun getAllAchievements(): Flow<List<AchievementEntity>> = achievementDao.getAllAchievements()
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>> = achievementDao.getUnlockedAchievements()

    suspend fun insertAchievement(achievement: AchievementEntity) = achievementDao.insertAchievement(achievement)
    suspend fun updateAchievement(achievement: AchievementEntity) = achievementDao.updateAchievement(achievement)

    /**
     * Clears [HabitEntity.isCompleted] when a new local calendar day started since [HabitEntity.lastCompletedDate].
     */
    suspend fun applyCalendarDayRolloverIfNeeded() {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        for (h in habitDao.getAllHabitsOnce()) {
            if (!h.isActive || !h.isCompleted) continue
            if (!h.frequency.equals("daily", ignoreCase = true)) continue
            if (HabitCalendarLogic.shouldResetDailyCompletionFlag(h.lastCompletedDate, now, zone)) {
                habitDao.updateHabit(h.copy(isCompleted = false))
            }
        }
    }

    suspend fun ensureDefaultAchievements() {
        if (achievementDao.countAchievements() > 0) return
        listOf(
            AchievementEntity(
                ACH_FIRST,
                "First habit",
                "Create your first habit.",
                "🌟",
                "getting_started",
                false,
                null,
                0,
                1,
                0,
            ),
            AchievementEntity(
                ACH_STREAK7,
                "Week strong",
                "Reach a 7-day streak on any habit.",
                "🔥",
                "streak",
                false,
                null,
                0,
                7,
                0,
            ),
            AchievementEntity(
                ACH_COMPLETE25,
                "25 check-ins",
                "Log 25 habit completions in total.",
                "✅",
                "volume",
                false,
                null,
                0,
                25,
                0,
            ),
        ).forEach { achievementDao.insertAchievement(it) }
    }

    suspend fun getAllAchievementsOnce(): List<AchievementEntity> = achievementDao.getAllAchievementsOnce()

    suspend fun mergeCompletionsFromRestore(rows: List<HabitCompletionEntity>) {
        for (c in rows) {
            completionDao.insertCompletion(c)
        }
    }

    suspend fun recomputeAchievements() {
        ensureDefaultAchievements()
        val habits = habitDao.getAllHabitsOnce()
        val totalActive = habits.count { it.isActive }
        val maxStreak = habits.maxOfOrNull { max(it.streakDays, it.bestStreak) } ?: 0
        val completions = completionDao.countTotalCompletions()
        for (a in achievementDao.getAllAchievementsOnce()) {
            val triple = when (a.id) {
                ACH_FIRST -> Triple(1, totalActive.coerceAtMost(1), totalActive >= 1)
                ACH_STREAK7 -> Triple(7, maxStreak.coerceAtMost(7), maxStreak >= 7)
                ACH_COMPLETE25 -> Triple(25, completions.coerceAtMost(25), completions >= 25)
                else -> null
            } ?: continue
            val (req, prog, shouldUnlock) = triple
            val progressPct = ((prog * 100) / req).coerceIn(0, 100)
            val newlyUnlocked = shouldUnlock && !a.isUnlocked
            val updated = a.copy(
                currentProgress = prog,
                requirementValue = req,
                progress = if (a.isUnlocked || shouldUnlock) 100 else progressPct,
                isUnlocked = a.isUnlocked || shouldUnlock,
                unlockedAt = when {
                    newlyUnlocked -> System.currentTimeMillis()
                    else -> a.unlockedAt
                },
            )
            achievementDao.updateAchievement(updated)
        }
    }

    private fun localDayBoundsMillis(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    companion object {
        const val ACH_FIRST = "ach_first_habit"
        const val ACH_STREAK7 = "ach_streak_7"
        const val ACH_COMPLETE25 = "ach_completions_25"
    }
}
