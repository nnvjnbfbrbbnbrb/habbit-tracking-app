package com.ansangha.craxxjxbdbf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ansangha.craxxjxbdbf.data.preferences.UserUiPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userUiPreferences: UserUiPreferences,
) : ViewModel() {

    val onboardingComplete = userUiPreferences.onboardingComplete
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setOnboardingComplete(complete: Boolean) {
        viewModelScope.launch {
            userUiPreferences.setOnboardingComplete(complete)
        }
    }
}
