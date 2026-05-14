package com.ansangha.craxxjxbdbf.repository

import com.ansangha.craxxjxbdbf.data.local.dao.HabitCompletionDao
import com.ansangha.craxxjxbdbf.data.local.dao.HabitDao
import com.ansangha.craxxjxbdbf.data.local.entity.HabitCompletionEntity
import com.ansangha.craxxjxbdbf.data.local.entity.HabitEntity
import com.ansangha.craxxjxbdbf.domain.AnalyticsStreakLogic
import com.ansangha.craxxjxbdbf.network.CompletionWireDto
import com.ansangha.craxxjxbdbf.network.DeviceAnalyticsBundleDto
import com.ansangha.craxxjxbdbf.network.HabitSyncWireDto
import com.ansangha.craxxjxbdbf.safety.usage.UsageStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class DailyCompletionRate(
    val day: LocalDate,
    /** 0f..1f — distinct habits completed / max(active habits, 1). */
    val rate: Float,
)

data class HeatmapCell(
    val habitId: String,
    val habitName: String,
    /** 0 = start of window (oldest day in grid). */
    val dayIndex: Int,
    /** 0..1 intensity. */
    val intensity: Float,
)

data class MonthDayMarker(
    val day: Int,
    val rate: Float,
    val hasStreakHint: Boolean,
)

data class HabitConsistency(
    val habitId: String,
    val name: String,
    val rate: Float,
)

data class AnalyticsSnapshot(
    val dailyRates: List<DailyCompletionRate>,
    val heatmap: List<HeatmapCell>,
    val monthMarkers: List<MonthDayMarker>,
    val globalBestStreak: Int,
    val perHabitBestStreak: List<Pair<String, Int>>,
    val mostConsistent: HabitConsistency?,
    val gentleWeakest: HabitConsistency?,
    val hourlyHeat: List<Int>,
    val zone: ZoneId,
)

@Singleton
class AnalyticsRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao,
    private val usageStatsRepository: UsageStatsRepository,
) {

    private val dayKeyFmt: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun buildSnapshot(
        zone: ZoneId = ZoneId.systemDefault(),
        historyDays: Int = 30,
        heatmapDays: Int = 7,
    ): AnalyticsSnapshot = withContext(Dispatchers.Default) {
        val now = ZonedDateTime.now(zone)
        val today = now.toLocalDate()
        val startDay = today.minusDays((historyDays - 1).toLong())
        val startMillis = startDay.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val habits = habitDao.getAllHabitsOnce()
        val active = habits.filter { it.isActive }
        val denom = maxOf(active.size, 1)

        val byDayRows = completionDao.countDistinctHabitsByLocalDay(startMillis, endMillis)
        val byDayMap = byDayRows.associate { LocalDate.parse(it.day, dayKeyFmt) to it.habitsDone }

        val dailyRates = (0 until historyDays).map { offset ->
            val d = startDay.plusDays(offset.toLong())
            val done = byDayMap[d] ?: 0
            DailyCompletionRate(d, (done.toFloat() / denom).coerceIn(0f, 1f))
        }

        val heatStart = today.minusDays((heatmapDays - 1).toLong())
        val heatStartMs = heatStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val heatEndMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val habitDayRows = completionDao.completionsByHabitAndLocalDay(heatStartMs, heatEndMs)
        val grouped = habitDayRows.groupBy { it.habitId }
        val heatmap = mutableListOf<HeatmapCell>()
        val orderedActive = active.sortedByDescending { it.priority }.take(12)
        for (habit in orderedActive) {
            val mapByDay = grouped[habit.id].orEmpty().associate { LocalDate.parse(it.day, dayKeyFmt) to it.completions }
            for (i in 0 until heatmapDays) {
                val d = heatStart.plusDays(i.toLong())
                val c = mapByDay[d] ?: 0
                val target = habit.targetCount.coerceAtLeast(1)
                val intensity = (c.toFloat() / target).coerceIn(0f, 1f)
                heatmap += HeatmapCell(habit.id, habit.name, i, intensity)
            }
        }

        val ym = YearMonth.from(today)
        val monthStart = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthEnd = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val monthRows = completionDao.countDistinctHabitsByLocalDay(monthStart, monthEnd)
        val monthMap = monthRows.associate { LocalDate.parse(it.day, dayKeyFmt) to it.habitsDone }
        val monthMarkers = (1..ym.lengthOfMonth()).map { dom ->
            val d = ym.atDay(dom)
            val done = monthMap[d] ?: 0
            MonthDayMarker(dom, (done.toFloat() / denom).coerceIn(0f, 1f), done > 0)
        }

        val globalBest = habits.maxOfOrNull { maxOf(it.bestStreak, it.streakDays) } ?: 0
        val perHabitBest = habits.map { h ->
            val streakFromCompletions = maxStreakFromCompletions(h.id, zone)
            h.name to maxOf(h.bestStreak, streakFromCompletions)
        }.sortedByDescending { it.second }

        val windowStart = today.minusDays(29).atStartOfDay(zone).toInstant().toEpochMilli()
        val windowEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val consistency = active.map { h ->
            val daysWithAny = habitDayCountDistinct(h.id, windowStart, windowEnd, zone)
            val rate = daysWithAny / 30f
            HabitConsistency(h.id, h.name, rate.coerceIn(0f, 1f))
        }
        val most = consistency.maxByOrNull { it.rate }
        val gentleWeakest = consistency.filter { it.rate < 1f }.minByOrNull { it.rate }

        val hourRows = completionDao.countByLocalHour(windowStart, windowEnd)
        val hourly = IntArray(24)
        for (r in hourRows) {
            if (r.hour in 0..23) hourly[r.hour] = r.count
        }

        AnalyticsSnapshot(
            dailyRates = dailyRates,
            heatmap = heatmap,
            monthMarkers = monthMarkers,
            globalBestStreak = globalBest,
            perHabitBestStreak = perHabitBest,
            mostConsistent = most,
            gentleWeakest = gentleWeakest,
            hourlyHeat = hourly.toList(),
            zone = zone,
        )
    }

    suspend fun buildDeviceBundle(
        userId: String,
        sleepBedHour: Int? = null,
        sleepWakeHour: Int? = null,
    ): DeviceAnalyticsBundleDto = withContext(Dispatchers.Default) {
        val zone = ZoneId.systemDefault()
        val habits = habitDao.getAllHabitsOnce().map { HabitSyncWireDto.fromEntity(it) }
        val since = Instant.now().minus(120, ChronoUnit.DAYS).toEpochMilli()
        val until = Instant.now().toEpochMilli() + 60_000L
        val completions = completionDao.getCompletionsBetween(since, until).map {
            CompletionWireDto(habitId = it.habitId, completedAt = it.completedAt, mood = it.mood)
        }
        val usage = runCatching {
            if (usageStatsRepository.hasUsageAccess()) {
                usageStatsRepository.todayTotalsDisplayText()
            } else {
                null
            }
        }.getOrNull()
        DeviceAnalyticsBundleDto(
            userId = userId,
            habits = habits,
            completions = completions,
            usageScreenSummary = usage,
            sleepBedHour = sleepBedHour,
            sleepWakeHour = sleepWakeHour,
        )
    }

    private suspend fun habitDayCountDistinct(
        habitId: String,
        start: Long,
        end: Long,
        zone: ZoneId,
    ): Int {
        val list = completionDao.getCompletionsBetween(start, end).filter { it.habitId == habitId }
        return list.map { Instant.ofEpochMilli(it.completedAt).atZone(zone).toLocalDate() }.toSet().size
    }

    private suspend fun maxStreakFromCompletions(habitId: String, zone: ZoneId): Int {
        val rows = completionDao.getCompletionsForHabitOnce(habitId)
        val days = AnalyticsStreakLogic.localDatesFromCompletionMillis(
            rows.map { it.completedAt },
            zone,
        )
        return AnalyticsStreakLogic.maxConsecutiveLocalDays(days)
    }
}
