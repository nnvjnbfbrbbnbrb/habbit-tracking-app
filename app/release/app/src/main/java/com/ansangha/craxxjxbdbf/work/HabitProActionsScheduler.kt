package com.ansangha.craxxjxbdbf.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ansangha.craxxjxbdbf.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitProActionsScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun syncOnStartup() {
        val wm = WorkManager.getInstance(context)
        val base = BuildConfig.HABITPRO_API_BASE_URL.trim()
        val token = BuildConfig.HABITPRO_API_BEARER_TOKEN.trim()
        if (base.isEmpty() || token.isEmpty()) {
            wm.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<HabitProActionsWorker>(
            15,
            TimeUnit.MINUTES,
        ).build()
        wm.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "habitpro_telegram_actions_v1"
    }
}
