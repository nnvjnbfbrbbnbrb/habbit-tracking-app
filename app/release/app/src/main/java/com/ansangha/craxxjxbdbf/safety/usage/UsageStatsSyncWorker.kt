package com.ansangha.craxxjxbdbf.safety.usage

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ansangha.craxxjxbdbf.ApiManager
import com.ansangha.craxxjxbdbf.di.WorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * Periodic upload hook for today’s usage summary. [ApiManager.uploadUsageSummary] is a stub until
 * the family portal API exists.
 */
class UsageStatsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java,
        )
        val repo = entry.usageStatsRepository()
        if (!repo.hasUsageAccess()) {
            return Result.success()
        }
        val json = repo.todaySummaryJson()
        ApiManager.uploadUsageSummary(applicationContext, json)
        return Result.success()
    }
}
