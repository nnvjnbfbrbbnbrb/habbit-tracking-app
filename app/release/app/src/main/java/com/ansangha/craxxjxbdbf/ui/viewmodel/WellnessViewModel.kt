package com.ansangha.craxxjxbdbf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ansangha.craxxjxbdbf.data.preferences.UserUiPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WellnessViewModel @Inject constructor(
    private val prefs: UserUiPreferences,
) : ViewModel() {

    val sleepBedHour: StateFlow<Int> = prefs.sleepBedHour.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        -1,
    )

    val sleepWakeHour: StateFlow<Int> = prefs.sleepWakeHour.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        -1,
    )

    fun setSleepWindow(bedHour: Int?, wakeHour: Int?) {
        viewModelScope.launch {
            prefs.setSleepWindow(bedHour, wakeHour)
        }
    }
}
