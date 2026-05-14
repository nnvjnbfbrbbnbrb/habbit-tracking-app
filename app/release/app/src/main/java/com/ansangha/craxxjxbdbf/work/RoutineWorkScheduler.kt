package com.ansangha.craxxjxbdbf.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ansangha.craxxjxbdbf.data.preferences.UserUiPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineWorkScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userUiPreferences: UserUiPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun syncScheduleWithPreferenceAsync() {
        scope.launch {
            if (userUiPreferences.routineRemindersEnabledSnapshot()) {
                ensureScheduled()
            } else {
                cancel()
            }
        }
    }

    fun ensureScheduled() {
        val request = PeriodicWorkRequestBuilder<RoutineIntegrityWorker>(
            6,
            TimeUnit.HOURS,
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "habit_routine_integrity_v1"
    }
}
