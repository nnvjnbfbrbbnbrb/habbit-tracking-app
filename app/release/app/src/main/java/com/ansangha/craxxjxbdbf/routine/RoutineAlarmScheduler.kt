package com.ansangha.craxxjxbdbf.routine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineTaskEntity
import com.ansangha.craxxjxbdbf.domain.RoutineScheduleCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineAlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val primaryFireMillis = ConcurrentHashMap<Long, Long>()

    fun scheduleNextAlarm(task: RoutineTaskEntity) {
        if (!task.enabled) return
        val trigger = RoutineScheduleCalculator.nextFireMillis(
            daysOfWeekMask = task.daysOfWeekMask,
            timeMinutesFromMidnight = task.timeMinutesFromMidnight,
            fromMillis = System.currentTimeMillis(),
        )
        primaryFireMillis[task.id] = trigger
        val pi = primaryPendingIntent(task.id)
        setExact(trigger, pi)
    }

    fun anchorMillisForPrimary(taskId: Long): Long =
        primaryFireMillis[taskId] ?: System.currentTimeMillis()

    fun scheduleFollowUpTenMinutes(taskId: Long, anchorMillis: Long) {
        val trigger = anchorMillis + 10 * 60 * 1000L
        if (trigger <= System.currentTimeMillis()) return
        val pi = followUpPendingIntent(taskId)
        setExact(trigger, pi)
    }

    fun cancelAlarmsForTask(taskId: Long) {
        alarmManager.cancel(primaryPendingIntent(taskId))
        alarmManager.cancel(followUpPendingIntent(taskId))
        primaryFireMillis.remove(taskId)
    }

    fun cancelFollowUp(taskId: Long) {
        alarmManager.cancel(followUpPendingIntent(taskId))
    }

    private fun setExact(triggerAtMillis: Long, pi: PendingIntent) {
        val am = alarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            @Suppress("DEPRECATION")
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun primaryPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = RoutineAlarmReceiver.ACTION_ROUTINE_PRIMARY
            putExtra(RoutineAlarmReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodePrimary(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun followUpPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = RoutineAlarmReceiver.ACTION_ROUTINE_FOLLOWUP
            putExtra(RoutineAlarmReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFollowup(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCodePrimary(taskId: Long): Int =
        0x2_000_000 xor (taskId.toInt() and 0xfffffff)

    private fun requestCodeFollowup(taskId: Long): Int =
        0x3_000_000 xor (taskId.toInt() and 0xfffffff)

    companion object {
        /** Debug-friendly skew for tests (0 in production). */
        internal var clockSkewMillis: Long = 0L
    }
}
