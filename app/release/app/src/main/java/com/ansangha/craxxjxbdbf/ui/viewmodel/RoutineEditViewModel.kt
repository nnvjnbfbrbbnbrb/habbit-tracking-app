package com.ansangha.craxxjxbdbf.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineTaskEntity
import com.ansangha.craxxjxbdbf.domain.RoutineDayMask
import com.ansangha.craxxjxbdbf.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineEditViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: 0L

    private val _existing = MutableStateFlow<RoutineTaskEntity?>(null)
    val existing: StateFlow<RoutineTaskEntity?> = _existing.asStateFlow()

    init {
        if (taskId > 0L) {
            viewModelScope.launch {
                _existing.value = routineRepository.getTaskOnce(taskId)
            }
        }
    }

    fun save(
        name: String,
        hour: Int,
        minute: Int,
        repeatCount: Int,
        daysMask: Int,
        enabled: Boolean,
        graceMinutes: Int,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            val minutes = (hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59)).coerceIn(0, 1439)
            val rc = repeatCount.coerceIn(1, 12)
            val mask = if (daysMask == 0) RoutineDayMask.defaultWeekdayMask() else daysMask
            val now = System.currentTimeMillis()
            val existing = _existing.value
            if (existing == null) {
                val entity = RoutineTaskEntity(
                    id = 0L,
                    name = name.trim().ifEmpty { "Routine" },
                    timeMinutesFromMidnight = minutes,
                    repeatCount = rc,
                    daysOfWeekMask = mask,
                    enabled = enabled,
                    graceMinutes = graceMinutes.coerceIn(5, 120),
                    createdAt = now,
                )
                routineRepository.insertTask(entity)
            } else {
                routineRepository.updateTask(
                    existing.copy(
                        name = name.trim().ifEmpty { "Routine" },
                        timeMinutesFromMidnight = minutes,
                        repeatCount = rc,
                        daysOfWeekMask = mask,
                        enabled = enabled,
                        graceMinutes = graceMinutes.coerceIn(5, 120),
                    ),
                )
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (taskId <= 0L) return
        viewModelScope.launch {
            routineRepository.deleteTask(taskId)
            onDone()
        }
    }
}
