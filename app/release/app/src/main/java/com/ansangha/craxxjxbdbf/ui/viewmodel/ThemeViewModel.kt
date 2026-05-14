package com.ansangha.craxxjxbdbf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ansangha.craxxjxbdbf.data.preferences.DarkThemePreference
import com.ansangha.craxxjxbdbf.data.preferences.UserUiPreferences
import com.ansangha.craxxjxbdbf.work.RoutineWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userUiPreferences: UserUiPreferences,
    private val routineWorkScheduler: RoutineWorkScheduler,
) : ViewModel() {

    val darkThemePreference = userUiPreferences.darkThemePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DarkThemePreference.FollowSystem)

    val useDynamicColor = userUiPreferences.useDynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val routineRemindersEnabled = userUiPreferences.routineRemindersEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setDarkThemePreference(value: DarkThemePreference) {
        viewModelScope.launch {
            userUiPreferences.setDarkThemePreference(value)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userUiPreferences.setDynamicColor(enabled)
        }
    }

    fun setRoutineRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userUiPreferences.setRoutineRemindersEnabled(enabled)
            if (enabled) {
                routineWorkScheduler.ensureScheduled()
            } else {
                routineWorkScheduler.cancel()
            }
        }
    }
}
