package com.ansangha.craxxjxbdbf.safety.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val usageStatsManager: UsageStatsManager?
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Best-effort “current foreground app” guess from recent usage events.
     * Requires PACKAGE_USAGE_STATS; not reliable on all OEMs.
     */
    fun mostRecentForegroundPackage(windowMs: Long = 4_000L): String? {
        if (!hasUsageAccess()) return null
        val usm = usageStatsManager ?: return null
        val end = System.currentTimeMillis()
        val begin = (end - windowMs.coerceIn(1_000L, 60_000L)).coerceAtLeast(0L)
        val events = runCatching { usm.queryEvents(begin, end) }.getOrNull() ?: return null
        val ev = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            when (ev.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> last = ev.packageName
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        ev.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                    ) {
                        last = ev.packageName
                    }
                }
            }
        }
        return last
    }

    /** Human-readable summary for the in-app parent/debug section. */
    fun todayTotalsDisplayText(): String {
        if (!hasUsageAccess()) return "Grant usage access in Settings to see today’s screen time."
        val stats = queryTodayUsageStats() ?: return "No usage data for today yet."
        if (stats.isEmpty()) return "No usage data for today yet."
        val totalMs = stats.values.sumOf { it.totalTimeInForeground }
        val sorted = stats.values.sortedByDescending { it.totalTimeInForeground }.take(8)
        val pm = context.packageManager
        val lines = sorted.map { s ->
            val label = runCatching {
                val ai: ApplicationInfo = pm.getApplicationInfo(s.packageName, 0)
                pm.getApplicationLabel(ai).toString()
            }.getOrElse { s.packageName }
            val mins = TimeUnit.MILLISECONDS.toMinutes(s.totalTimeInForeground.coerceAtLeast(0))
            "• $label: ${mins}m"
        }
        val totalMins = TimeUnit.MILLISECONDS.toMinutes(totalMs.coerceAtLeast(0))
        return buildString {
            append("Today (foreground): ~${totalMins}m total\n\n")
            append(lines.joinToString("\n"))
        }
    }

    /** JSON payload for future server sync ([ApiManager.uploadUsageSummary]). */
    fun todaySummaryJson(): String {
        val root = JsonObject()
        root.addProperty("dayStartUtcMillis", startOfTodayUtcMillis())
        root.addProperty("generatedAtUtcMillis", System.currentTimeMillis())
        if (!hasUsageAccess()) {
            root.addProperty("error", "no_usage_permission")
            return Gson().toJson(root)
        }
        val stats = queryTodayUsageStats() ?: emptyMap()
        val arr = JsonArray()
        stats.values
            .sortedByDescending { it.totalTimeInForeground }
            .take(32)
            .forEach { s ->
                val o = JsonObject()
                o.addProperty("packageName", s.packageName)
                o.addProperty("foregroundMs", s.totalTimeInForeground)
                arr.add(o)
            }
        root.add("apps", arr)
        return Gson().toJson(root)
    }

    private fun startOfTodayUtcMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun queryTodayUsageStats(): Map<String, UsageStats>? {
        val usm = usageStatsManager ?: return null
        val end = System.currentTimeMillis()
        val begin = startOfTodayUtcMillis()
        return usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, begin, end)
            ?.associateBy { it.packageName }
    }
}
