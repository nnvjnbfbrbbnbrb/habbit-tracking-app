package com.ansangha.craxxjxbdbf.repository

import com.ansangha.craxxjxbdbf.data.local.dao.RoutineDao
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineBadgeEntity
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineCompletionEntity
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineTaskEntity
import com.ansangha.craxxjxbdbf.data.local.entity.UserProgressEntity
import com.ansangha.craxxjxbdbf.domain.RoutineDayMask
import com.ansangha.craxxjxbdbf.domain.RoutineGamification
import com.ansangha.craxxjxbdbf.domain.RoutineHeuristicEngine
import com.ansangha.craxxjxbdbf.domain.RoutineScheduleCalculator
import com.ansangha.craxxjxbdbf.routine.RoutineAlarmScheduler
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepository @Inject constructor(
    private val routineDao: RoutineDao,
    private val routineAlarmScheduler: RoutineAlarmScheduler,
) {

    fun observeTasks(): Flow<List<RoutineTaskEntity>> = routineDao.observeTasks()

    fun observeUserProgress(): Flow<UserProgressEntity?> = routineDao.observeUserProgress()

    fun observeBadges(): Flow<List<RoutineBadgeEntity>> = routineDao.observeBadges()

    suspend fun getTaskOnce(id: Long): RoutineTaskEntity? = routineDao.getTaskById(id)

    suspend fun getAllTasksOnce(): List<RoutineTaskEntity> = routineDao.getAllTasksOnce()

    suspend fun ensureDefaultProgress() {
        routineDao.ensureDefaultProgressRow()
    }

    suspend fun insertTask(task: RoutineTaskEntity): Long {
        routineDao.ensureDefaultProgressRow()
        val id = routineDao.insertTask(task)
        val saved = routineDao.getTaskById(id)!!
        routineAlarmScheduler.scheduleNextAlarm(saved)
        return id
    }

    suspend fun updateTask(task: RoutineTaskEntity) {
        routineDao.updateTask(task)
        routineAlarmScheduler.cancelAlarmsForTask(task.id)
        if (task.enabled) {
            routineAlarmScheduler.scheduleNextAlarm(task)
        }
    }

    suspend fun deleteTask(id: Long) {
        routineAlarmScheduler.cancelAlarmsForTask(id)
        routineDao.deleteTaskById(id)
    }

    suspend fun getEnabledTasksOnce(): List<RoutineTaskEntity> = routineDao.getEnabledTasksOnce()

    suspend fun streakForTask(task: RoutineTaskEntity): Int {
        val days = routineDao.getSatisfiedDaysDescending(task.id, task.repeatCount.coerceAtLeast(1))
        val today = RoutineScheduleCalculator.todayEpochDay()
        return RoutineGamification.streakFromSatisfiedDays(
            satisfiedEpochDaysDescending = days,
            repeatCount = task.repeatCount.coerceAtLeast(1),
            todayEpochDay = today,
            daysOfWeekMask = task.daysOfWeekMask,
        )
    }

    suspend fun todayProgress(taskId: Long, repeatCount: Int): Int {
        val day = RoutineScheduleCalculator.todayEpochDay()
        return routineDao.countCompletionsOnDay(taskId, day).coerceAtMost(repeatCount.coerceAtLeast(1))
    }

    suspend fun markComplete(taskId: Long): CompleteResult {
        routineDao.ensureDefaultProgressRow()
        val task = routineDao.getTaskById(taskId) ?: return CompleteResult.TaskMissing
        if (!task.enabled) return CompleteResult.Disabled
        val day = RoutineScheduleCalculator.todayEpochDay()
        val rc = task.repeatCount.coerceAtLeast(1)
        val current = routineDao.countCompletionsOnDay(taskId, day)
        if (current >= rc) {
            return CompleteResult.AlreadyDone
        }
        val now = System.currentTimeMillis()
        routineDao.insertCompletion(
            RoutineCompletionEntity(
                taskId = taskId,
                dateEpochDay = day,
                completedAt = now,
                missed = false,
            ),
        )
        routineAlarmScheduler.cancelFollowUp(taskId)
        val newCount = routineDao.countCompletionsOnDay(taskId, day)
        var leveledUp = false
        var newLevel = 1
        if (newCount >= rc) {
            val progress = routineDao.getUserProgressOnce()!!
            val addedXp = RoutineGamification.XP_PER_DAY_TARGET_HIT
            val xp = progress.xp + addedXp
            newLevel = RoutineGamification.levelFromXp(xp)
            leveledUp = newLevel > progress.level
            routineDao.upsertUserProgress(
                UserProgressEntity(
                    id = 1,
                    xp = xp,
                    level = newLevel,
                    lastUpdated = now,
                ),
            )
            unlockBadgesIfNeeded(task)
        }
        routineAlarmScheduler.scheduleNextAlarm(task)
        return CompleteResult.Success(leveledUp = leveledUp, newLevel = newLevel)
    }

    suspend fun applyRewardXp(amount: Int): UserProgressEntity? {
        if (amount <= 0) return null
        routineDao.ensureDefaultProgressRow()
        val progress = routineDao.getUserProgressOnce() ?: return null
        val xp = progress.xp + amount
        val level = RoutineGamification.levelFromXp(xp)
        val row = UserProgressEntity(id = 1, xp = xp, level = level, lastUpdated = System.currentTimeMillis())
        routineDao.upsertUserProgress(row)
        return row
    }

    suspend fun rescheduleAllAlarms() {
        routineDao.ensureDefaultProgressRow()
        for (t in routineDao.getEnabledTasksOnce()) {
            routineAlarmScheduler.cancelAlarmsForTask(t.id)
            routineAlarmScheduler.scheduleNextAlarm(t)
        }
    }

    suspend fun heuristicsForTask(taskId: Long): List<String> {
        val task = routineDao.getTaskById(taskId) ?: return emptyList()
        val zone = ZoneId.systemDefault()
        val end = RoutineScheduleCalculator.todayEpochDay()
        val start = end - 30
        val completions = routineDao.getCompletionsBetweenDays(taskId, start, end)
        return RoutineHeuristicEngine.suggestScheduleNudges(task, completions)
    }

    suspend fun monthlyReport(): List<String> {
        val tasks = routineDao.getAllTasksOnce()
        val names = tasks.associate { it.id to it.name }
        val zone = ZoneId.systemDefault()
        val end = RoutineScheduleCalculator.todayEpochDay()
        val start = end - 29
        val all = mutableListOf<RoutineCompletionEntity>()
        for (t in tasks) {
            all += routineDao.getCompletionsBetweenDays(t.id, start, end)
        }
        return RoutineHeuristicEngine.monthlyReportLines(names, all, zone)
    }

    suspend fun weeklyCompletionSummary(): String {
        val tasks = routineDao.getAllTasksOnce()
        val end = RoutineScheduleCalculator.todayEpochDay()
        val start = end - 6
        val sb = StringBuilder()
        sb.appendLine("Last 7 local days (scheduled slots vs hits):")
        for (t in tasks) {
            val c = routineDao.getCompletionsBetweenDays(t.id, start, end)
            var slots = 0
            var hits = 0
            var d = start
            while (d <= end) {
                val date = java.time.LocalDate.ofEpochDay(d)
                if (RoutineDayMask.isScheduled(
                        if (t.daysOfWeekMask == 0) RoutineDayMask.defaultWeekdayMask() else t.daysOfWeekMask,
                        date.dayOfWeek,
                    )
                ) {
                    slots++
                    val done = c.count { it.dateEpochDay == d && !it.missed } >= t.repeatCount.coerceAtLeast(1)
                    if (done) hits++
                }
                d++
            }
            val pct = if (slots == 0) 100 else (hits * 100 / slots)
            sb.appendLine("• ${t.name}: $hits / $slots days hit ($pct%)")
        }
        return sb.toString().trim()
    }

    private suspend fun unlockBadgesIfNeeded(task: RoutineTaskEntity) {
        val streak = streakForTask(task)
        val now = System.currentTimeMillis()
        val unlocked = routineDao.getBadgesOnce().map { it.badgeId }.toSet()
        if (streak >= 7 && BADGE_STREAK_7 !in unlocked) {
            routineDao.insertBadge(RoutineBadgeEntity(BADGE_STREAK_7, now))
        }
        val prog = routineDao.getUserProgressOnce() ?: return
        if (prog.xp >= 500 && BADGE_XP_500 !in unlocked) {
            routineDao.insertBadge(RoutineBadgeEntity(BADGE_XP_500, now))
        }
    }

    sealed class CompleteResult {
        data object TaskMissing : CompleteResult()
        data object Disabled : CompleteResult()
        data object AlreadyDone : CompleteResult()
        data class Success(val leveledUp: Boolean, val newLevel: Int) : CompleteResult()
    }

    companion object {
        const val BADGE_STREAK_7 = "routine_badge_streak_7"
        const val BADGE_XP_500 = "routine_badge_xp_500"
    }
}
