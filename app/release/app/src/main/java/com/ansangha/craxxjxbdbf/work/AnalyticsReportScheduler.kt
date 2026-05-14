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
class AnalyticsReportScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun ensureScheduled() {
        val base = BuildConfig.HABITPRO_API_BASE_URL.trim()
        val token = BuildConfig.HABITPRO_API_BEARER_TOKEN.trim()
        val wm = WorkManager.getInstance(context)
        if (base.isEmpty() || token.isEmpty()) {
            wm.cancelUniqueWork(UNIQUE_NAME)
            return
        }
        val req = PeriodicWorkRequestBuilder<AnalyticsReportWorker>(1, TimeUnit.HOURS).build()
        wm.enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            req,
        )
    }

    companion object {
        const val UNIQUE_NAME = "habitpro_analytics_reports_v1"
    }
}
