package com.ansangha.craxxjxbdbf.safety.usage

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsSyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun ensureScheduled() {
        val wm = WorkManager.getInstance(context)
        val request = PeriodicWorkRequestBuilder<UsageStatsSyncWorker>(
            6,
            TimeUnit.HOURS,
        ).build()
        wm.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "habitpro_usage_stats_family_sync_v1"
    }
}
