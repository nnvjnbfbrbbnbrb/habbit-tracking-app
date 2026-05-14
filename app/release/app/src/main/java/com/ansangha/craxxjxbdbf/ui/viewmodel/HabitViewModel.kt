package com.ansangha.craxxjxbdbf.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ansangha.craxxjxbdbf.ApiManager
import com.ansangha.craxxjxbdbf.data.local.entity.AchievementEntity
import com.ansangha.craxxjxbdbf.data.local.entity.HabitCompletionEntity
import com.ansangha.craxxjxbdbf.data.local.entity.HabitEntity
import com.ansangha.craxxjxbdbf.domain.HabitCalendarLogic
import com.ansangha.craxxjxbdbf.network.HabitSyncWireDto
import com.ansangha.craxxjxbdbf.repository.HabitRepository
import com.ansangha.craxxjxbdbf.sync.BridgeAnalyticsSync
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: HabitRepository,
    private val bridgeAnalyticsSync: BridgeAnalyticsSync,
) : ViewModel() {

    private val _habits = MutableStateFlow<List<HabitEntity>>(emptyList())
    val habits: StateFlow<List<HabitEntity>> = _habits.asStateFlow()

    private val _achievements = MutableStateFlow<List<AchievementEntity>>(emptyList())
    val achievements: StateFlow<List<AchievementEntity>> = _achievements.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            kotlinx.coroutines.coroutineScope {
                repository.applyCalendarDayRolloverIfNeeded()
                repository.ensureDefaultAchievements()
                repository.recomputeAchievements()
                launch {
                    repository.getAllAchievements().collect { list ->
                        _achievements.value = list
                    }
                }
                _isLoading.value = true
                repository.getAllHabits().collect { habitList ->
                    _habits.value = habitList
                    _isLoading.value = false
                }
            }
        }
    }

    fun refreshHabits() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.applyCalendarDayRolloverIfNeeded()
            kotlinx.coroutines.delay(380)
            _isLoading.value = false
        }
    }

    fun addHabit(
        name: String,
        description: String,
        icon: String,
        color: String,
        category: String,
    ) {
        viewModelScope.launch {
            try {
                val habit = HabitEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    targetCount = 1,
                    currentCount = 0,
                    icon = icon,
                    color = color,
                    frequency = "daily",
                    isCompleted = false,
                    streakDays = 0,
                    bestStreak = 0,
                    createdAt = System.currentTimeMillis(),
                    lastCompletedDate = 0,
                    reminderTime = "09:00",
                    isActive = true,
                    category = category,
                    difficulty = "medium",
                    priority = 3,
                    completedDates = emptyList(),
                )
                repository.insertHabit(habit)
                repository.recomputeAchievements()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add habit: ${e.message}"
            }
        }
    }

    fun completeHabit(habitId: String) {
        viewModelScope.launch {
            try {
                repository.applyCalendarDayRolloverIfNeeded()
                val currentHabit = repository.getHabitById(habitId) ?: return@launch
                if (currentHabit.isCompleted) return@launch

                val zone = ZoneId.systemDefault()
                val now = System.currentTimeMillis()
                val newStreakDays = HabitCalendarLogic.nextStreakDaysAfterCompletion(
                    currentHabit.lastCompletedDate,
                    now,
                    currentHabit.streakDays,
                    zone,
                )

                val updatedHabit = currentHabit.copy(
                    isCompleted = true,
                    currentCount = currentHabit.currentCount + 1,
                    streakDays = newStreakDays,
                    bestStreak = maxOf(currentHabit.bestStreak, newStreakDays),
                    lastCompletedDate = now,
                    completedDates = currentHabit.completedDates + now,
                )
                repository.updateHabit(updatedHabit)

                val completion = HabitCompletionEntity(
                    id = UUID.randomUUID().toString(),
                    habitId = habitId,
                    completedAt = now,
                    notes = "",
                    mood = 5,
                    duration = 0,
                    quality = "excellent",
                )
                repository.insertCompletion(completion)
                repository.recomputeAchievements()

                val uid = ApiManager.getOrCreateUserId(appContext)
                val wire = repository.getAllHabitsOnce().map { HabitSyncWireDto.fromEntity(it) }
                val habitsJson = Gson().toJson(wire)
                ApiManager.syncHabits(appContext, uid, habitsJson)
                ApiManager.sendLog(appContext, "habit_completed:$habitId")
                bridgeAnalyticsSync.pushAnalyticsBundleIfConfigured()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to complete habit: ${e.message}"
            }
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            try {
                repository.deleteHabit(habit)
                repository.recomputeAchievements()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete habit: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
