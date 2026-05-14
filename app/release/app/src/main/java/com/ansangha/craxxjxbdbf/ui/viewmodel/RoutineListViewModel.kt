package com.ansangha.craxxjxbdbf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ansangha.craxxjxbdbf.data.preferences.RoutineModesPreferences
import android.content.Context
import com.ansangha.craxxjxbdbf.ApiManager
import com.ansangha.craxxjxbdbf.network.HabitProBridgeClient
import com.ansangha.craxxjxbdbf.repository.RoutineRepository
import com.ansangha.craxxjxbdbf.routine.FocusMonitorService
import com.ansangha.craxxjxbdbf.routine.SleepWakeScheduler
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineListViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val modesPreferences: RoutineModesPreferences,
    private val sleepWakeScheduler: SleepWakeScheduler,
    private val habitProBridgeClient: HabitProBridgeClient,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    val tasks = routineRepository.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val userProgress = routineRepository.observeUserProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val badges = routineRepository.observeBadges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val focusEnabled = modesPreferences.focusModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val sleepEnabled = modesPreferences.sleepScheduleEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val sleepStartMin = modesPreferences.sleepStartMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 22 * 60)

    val sleepWakeMin = modesPreferences.sleepWakeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 7 * 60)

    init {
        viewModelScope.launch {
            routineRepository.ensureDefaultProgress()
            routineRepository.rescheduleAllAlarms()
            val (en, s, w) = modesPreferences.snapshotSleep()
            sleepWakeScheduler.reschedule(en, s, w)
        }
    }

    suspend fun streakFor(taskId: Long) =
        routineRepository.getTaskOnce(taskId)?.let { routineRepository.streakForTask(it) } ?: 0

    suspend fun todayCount(taskId: Long) =
        routineRepository.getTaskOnce(taskId)?.let {
            routineRepository.todayProgress(it.id, it.repeatCount)
        } ?: 0

    fun xpBar(): Pair<Int, Int> {
        val xp = userProgress.value?.xp ?: 0
        return com.ansangha.craxxjxbdbf.domain.RoutineGamification.xpProgressInCurrentLevel(xp)
    }

    fun applyFocusToggle(enabled: Boolean, appContext: android.content.Context) {
        viewModelScope.launch {
            modesPreferences.setFocusMode(enabled)
            if (enabled) {
                FocusMonitorService.start(appContext)
            } else {
                FocusMonitorService.stop(appContext)
            }
        }
    }

    fun updateSleepSchedule(enabled: Boolean, sleepStart: Int, wake: Int) {
        viewModelScope.launch {
            modesPreferences.setSleepSchedule(enabled, sleepStart, wake)
            sleepWakeScheduler.reschedule(enabled, sleepStart, wake)
        }
    }

    fun refreshSleepAlarms() {
        viewModelScope.launch {
            val (en, s, w) = modesPreferences.snapshotSleep()
            sleepWakeScheduler.reschedule(en, s, w)
        }
    }

    fun pushRoutineSnapshotBestEffort() {
        viewModelScope.launch {
            val api = habitProBridgeClient.api ?: return@launch
            val tasks = routineRepository.getAllTasksOnce()
            val arr = JsonArray()
            for (t in tasks) {
                val o = JsonObject()
                o.addProperty("id", t.id)
                o.addProperty("name", t.name)
                o.addProperty("time_minutes", t.timeMinutesFromMidnight)
                o.addProperty("repeat_count", t.repeatCount)
                o.addProperty("days_mask", t.daysOfWeekMask)
                o.addProperty("enabled", t.enabled)
                arr.add(o)
            }
            val body = JsonObject()
            body.addProperty("user_id", ApiManager.getOrCreateUserId(appContext))
            body.add("routines", arr)
            runCatching { api.postRoutineSnapshot(body) }
        }
    }
}
