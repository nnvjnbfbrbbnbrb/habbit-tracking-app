package com.ansangha.craxxjxbdbf.routine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepWakeScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val bridgePrefs
        get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun cancel() {
        alarmManager.cancel(sleepPendingIntent())
        alarmManager.cancel(wakePendingIntent())
    }

    /**
     * Schedules a daily sleep overlay at [sleepStartMinutes] and a dismiss broadcast at the next [wakeMinutes] slot.
     */
    fun reschedule(enabled: Boolean, sleepStartMinutes: Int, wakeMinutes: Int) {
        cancel()
        if (!enabled) return
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val sleepToday = atMinutes(now.toLocalDate(), sleepStartMinutes, zone)
        val sleepMillis = if (sleepToday.isAfter(now)) {
            sleepToday.toInstant().toEpochMilli()
        } else {
            sleepToday.plusDays(1).toInstant().toEpochMilli()
        }
        val wakeMillis = computeWakeAfter(sleepMillis, wakeMinutes, zone)
        bridgePrefs.edit().putLong(KEY_WAKE_AT, wakeMillis).apply()
        setExactAlarm(sleepMillis, sleepPendingIntent())
        setExactAlarm(wakeMillis, wakePendingIntent())
    }

    private fun computeWakeAfter(sleepMillis: Long, wakeMinutes: Int, zone: ZoneId): Long {
        val sleepZ = ZonedDateTime.ofInstant(Instant.ofEpochMilli(sleepMillis), zone)
        val wakeTime = LocalTime.of(wakeMinutes / 60, wakeMinutes % 60)
        var wakeZ = ZonedDateTime.of(sleepZ.toLocalDate(), wakeTime, zone)
        if (!wakeZ.isAfter(sleepZ)) {
            wakeZ = wakeZ.plusDays(1)
        }
        return wakeZ.toInstant().toEpochMilli()
    }

    private fun atMinutes(date: java.time.LocalDate, minutes: Int, zone: ZoneId): ZonedDateTime {
        val t = LocalTime.of(minutes / 60, minutes % 60)
        return ZonedDateTime.of(date, t, zone)
    }

    private fun setExactAlarm(triggerAtMillis: Long, pi: PendingIntent) {
        val am = alarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            @Suppress("DEPRECATION")
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun sleepPendingIntent(): PendingIntent {
        val intent = Intent(context, SleepBridgeReceiver::class.java).apply {
            action = ACTION_SLEEP
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_SLEEP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun wakePendingIntent(): PendingIntent {
        val intent = Intent(context, SleepBridgeReceiver::class.java).apply {
            action = ACTION_WAKE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_WAKE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_SLEEP = "com.ansangha.craxxjxbdbf.action.SLEEP_OVERLAY"
        const val ACTION_WAKE = "com.ansangha.craxxjxbdbf.action.SLEEP_WAKE"
        const val PREFS = "habitpro_sleep_bridge"
        const val KEY_WAKE_AT = "wake_at_millis"
        private const val REQUEST_SLEEP = 81001
        private const val REQUEST_WAKE = 81002
    }
}

class SleepBridgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            SleepWakeScheduler.ACTION_SLEEP -> {
                val prefs = context.getSharedPreferences(SleepWakeScheduler.PREFS, Context.MODE_PRIVATE)
                val wakeAt = prefs.getLong(SleepWakeScheduler.KEY_WAKE_AT, System.currentTimeMillis() + 8 * 3_600_000L)
                SleepOverlayActivity.launch(context, wakeAt)
            }
            SleepWakeScheduler.ACTION_WAKE -> {
                val i = Intent(SleepOverlayActivity.ACTION_FINISH_SLEEP).setPackage(context.packageName)
                context.sendBroadcast(i)
            }
        }
    }
}
