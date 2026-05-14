package com.ansangha.craxxjxbdbf.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ansangha.craxxjxbdbf.data.preferences.UserUiPreferences
import com.ansangha.craxxjxbdbf.di.WorkerEntryPoint
import com.ansangha.craxxjxbdbf.repository.AnalyticsRepository
import com.ansangha.craxxjxbdbf.repository.HabitRepository
import com.ansangha.craxxjxbdbf.sync.BridgeAnalyticsSync
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

/**
 * Hourly tick: pushes analytics bundle, sends scheduled Telegram digests (via bridge),
 * and one completion-drop alert per local day (DataStore dedupe).
 */
class AnalyticsReportWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java,
        )
        val bridge = entry.bridgeAnalyticsSync()
        val prefs = entry.userUiPreferences()
        val habitRepo = entry.habitRepository()
        val analyticsRepo = entry.analyticsRepository()

        bridge.pushAnalyticsBundleIfConfigured()

        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val today = now.toLocalDate()
        val dayKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val hour = now.hour
        val dow = today.dayOfWeek.value
        val dom = today.dayOfMonth

        if (hour == 22 && prefs.lastDailyReportDaySnapshot() != dayKey) {
            val snap = analyticsRepo.buildSnapshot(zone, historyDays = 14, heatmapDays = 7)
            val rateToday = snap.dailyRates.lastOrNull()?.rate ?: 0f
            val text = buildString {
                append("📊 Daily digest\n")
                append("Today completion: ${(rateToday * 100).toInt()}%\n")
                append("Best streak (any habit): ${snap.globalBestStreak}d\n")
                append("Most consistent (30d): ")
                append(snap.mostConsistent?.name ?: "—")
                append("\n")
            }
            if (bridge.sendTelegramReportIfConfigured(text)) {
                prefs.setLastDailyReportDay(dayKey)
            }
        }

        if (dow == 7 && hour == 20) {
            val wf = WeekFields.ISO
            val tag = "${today.get(wf.weekBasedYear())}-W${today.get(wf.weekOfWeekBasedYear())}"
            if (prefs.lastWeeklyReportTagSnapshot() != tag) {
                val snap = analyticsRepo.buildSnapshot(zone, historyDays = 30, heatmapDays = 7)
                val avg = snap.dailyRates.takeLast(7).map { it.rate }.average().toFloat()
                val msg = "📅 Weekly digest\n7d avg completion: ${(avg * 100).toInt()}%\nGlobal best streak: ${snap.globalBestStreak}d\n"
                if (bridge.sendTelegramReportIfConfigured(msg)) {
                    prefs.setLastWeeklyReportTag(tag)
                }
            }
        }

        if (dom == 1 && hour == 9) {
            val tag = "${today.year}-${today.monthValue}"
            if (prefs.lastMonthlyReportTagSnapshot() != tag) {
                val snap = analyticsRepo.buildSnapshot(zone, historyDays = 31, heatmapDays = 7)
                val msg = "🗓 Monthly pulse\nBest streak ${snap.globalBestStreak}d; open the app for the full month grid.\n"
                if (bridge.sendTelegramReportIfConfigured(msg)) {
                    prefs.setLastMonthlyReportTag(tag)
                }
            }
        }

        maybeSendDropAlert(habitRepo, analyticsRepo, prefs, bridge, today, dayKey, zone)

        Result.success()
    }

    private suspend fun maybeSendDropAlert(
        habitRepo: HabitRepository,
        analyticsRepo: AnalyticsRepository,
        prefs: UserUiPreferences,
        bridge: BridgeAnalyticsSync,
        today: LocalDate,
        dayKey: String,
        zone: ZoneId,
    ) {
        if (prefs.lastDropAlertDaySnapshot() == dayKey) return
        val denom = habitRepo.countActiveHabits().coerceAtLeast(1)
        val todayDone = habitRepo.distinctHabitsCompletedToday()
        val todayPct = todayDone.toFloat() / denom

        val snap = analyticsRepo.buildSnapshot(zone, historyDays = 14, heatmapDays = 7)
        val last7 = snap.dailyRates.takeLast(7).map { it.rate }
        if (last7.size < 3) return
        val avg7 = last7.average().toFloat()
        if (avg7 <= 0.01f) return
        if (todayPct >= 0.5f * avg7) return

        val msg =
            "⚠️ Gentle nudge: today’s habit coverage (${(todayPct * 100).toInt()}%) is below half of your 7-day average (${(avg7 * 100).toInt()}%). No pressure — small reps still count."
        if (bridge.sendTelegramReportIfConfigured(msg)) {
            prefs.setLastDropAlertDay(dayKey)
        }
    }
}
