package com.ansangha.craxxjxbdbf.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ansangha.craxxjxbdbf.repository.AnalyticsRepository
import com.ansangha.craxxjxbdbf.repository.AnalyticsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _snapshot = MutableStateFlow<AnalyticsSnapshot?>(null)
    val snapshot: StateFlow<AnalyticsSnapshot?> = _snapshot.asStateFlow()

    init {
        refresh()
    }

    fun refresh(zone: ZoneId = ZoneId.systemDefault()) {
        viewModelScope.launch {
            _snapshot.value = analyticsRepository.buildSnapshot(zone = zone)
        }
    }
}
