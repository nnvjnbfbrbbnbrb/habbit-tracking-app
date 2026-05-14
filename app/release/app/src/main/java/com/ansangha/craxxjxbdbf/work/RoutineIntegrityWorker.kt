package com.ansangha.craxxjxbdbf.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ansangha.craxxjxbdbf.di.WorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * Inexact periodic job (WorkManager, ~every 6h) that performs a lightweight "routine integrity"
 * scan: if reminders are enabled, habits are still open, and notifications are allowed, nudge the user.
 */
class RoutineIntegrityWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java,
        )
        entry.habitRepository().applyCalendarDayRolloverIfNeeded()
        if (!entry.userUiPreferences().routineRemindersEnabledSnapshot()) {
            return Result.success()
        }
        val open = entry.habitRepository().getIncompleteActiveHabits()
        if (open.isEmpty()) {
            return Result.success()
        }
        if (!notificationsAllowed()) {
            return Result.success()
        }
        RoutineNotificationHelper.showOpenHabitsReminder(
            applicationContext,
            open.size,
            open.take(3).map { it.name },
        )
        return Result.success()
    }

    private fun notificationsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
