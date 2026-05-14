package com.ansangha.craxxjxbdbf.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ansangha.craxxjxbdbf.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineCompleteViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<Long>("taskId") ?: -1L

    private val _taskName = MutableStateFlow("")
    val taskName: StateFlow<String> = _taskName.asStateFlow()

    private val _result = MutableStateFlow<RoutineRepository.CompleteResult?>(null)
    val result: StateFlow<RoutineRepository.CompleteResult?> = _result.asStateFlow()

    init {
        viewModelScope.launch {
            val t = routineRepository.getTaskOnce(taskId)
            _taskName.value = t?.name.orEmpty()
        }
    }

    fun markComplete() {
        viewModelScope.launch {
            _result.value = routineRepository.markComplete(taskId)
        }
    }
}
