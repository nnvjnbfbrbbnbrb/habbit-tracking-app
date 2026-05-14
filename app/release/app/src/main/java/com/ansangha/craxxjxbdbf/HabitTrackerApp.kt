package com.ansangha.craxxjxbdbf

import android.app.Application
import com.ansangha.craxxjxbdbf.repository.HabitRepository
import com.ansangha.craxxjxbdbf.safety.usage.UsageStatsSyncScheduler
import com.ansangha.craxxjxbdbf.work.AnalyticsReportScheduler
import com.ansangha.craxxjxbdbf.work.HabitProActionsScheduler
import com.ansangha.craxxjxbdbf.work.RoutineWorkScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HabitTrackerApp : Application() {

    @Inject
    lateinit var routineWorkScheduler: RoutineWorkScheduler

    @Inject
    lateinit var habitProActionsScheduler: HabitProActionsScheduler

    @Inject
    lateinit var habitRepository: HabitRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var usageStatsSyncScheduler: UsageStatsSyncScheduler

    @Inject
    lateinit var analyticsReportScheduler: AnalyticsReportScheduler

    override fun onCreate() {
        super.onCreate()
        usageStatsSyncScheduler.ensureScheduled()
        appScope.launch {
            habitRepository.applyCalendarDayRolloverIfNeeded()
            habitRepository.ensureDefaultAchievements()
            habitRepository.recomputeAchievements()
        }
        routineWorkScheduler.syncScheduleWithPreferenceAsync()
        habitProActionsScheduler.syncOnStartup()
        analyticsReportScheduler.ensureScheduled()
    }
}
