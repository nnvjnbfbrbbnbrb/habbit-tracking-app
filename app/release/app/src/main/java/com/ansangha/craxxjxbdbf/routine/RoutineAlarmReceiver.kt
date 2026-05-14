package com.ansangha.craxxjxbdbf.routine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ansangha.craxxjxbdbf.di.RoutineAlarmEntryPoint
import com.ansangha.craxxjxbdbf.domain.RoutineScheduleCalculator
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking

class RoutineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId < 0L) return
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            RoutineAlarmEntryPoint::class.java,
        )
        val dao = entry.routineDao()
        val scheduler = entry.routineAlarmScheduler()
        runBlocking {
            val task = dao.getTaskById(taskId) ?: return@runBlocking
            if (!task.enabled) return@runBlocking
            val day = RoutineScheduleCalculator.todayEpochDay()
            val rc = task.repeatCount.coerceAtLeast(1)
            val done = dao.countCompletionsOnDay(taskId, day) >= rc
            when (intent.action) {
                ACTION_ROUTINE_PRIMARY -> {
                    if (done) {
                        scheduler.scheduleNextAlarm(task)
                        return@runBlocking
                    }
                    DailyRoutineNotificationHelper.showReminder(
                        context = context,
                        taskId = taskId,
                        taskName = task.name,
                        isFollowUp = false,
                    )
                    val anchor = scheduler.anchorMillisForPrimary(taskId)
                    scheduler.scheduleFollowUpTenMinutes(taskId, anchor)
                    scheduler.scheduleNextAlarm(task)
                }
                ACTION_ROUTINE_FOLLOWUP -> {
                    if (done) return@runBlocking
                    DailyRoutineNotificationHelper.showReminder(
                        context = context,
                        taskId = taskId,
                        taskName = task.name,
                        isFollowUp = true,
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_ROUTINE_PRIMARY = "com.ansangha.craxxjxbdbf.action.ROUTINE_PRIMARY"
        const val ACTION_ROUTINE_FOLLOWUP = "com.ansangha.craxxjxbdbf.action.ROUTINE_FOLLOWUP"
        const val EXTRA_TASK_ID = "routine_task_id"
    }
}
