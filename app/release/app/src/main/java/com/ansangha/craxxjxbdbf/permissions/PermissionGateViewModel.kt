package com.ansangha.craxxjxbdbf.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class PermissionGateViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    private val _rows = MutableStateFlow(
        PermissionOrchestrator.evaluate(application, mediaProjectionConsentInSession = false),
    )
    val rows: StateFlow<List<PermissionOrchestrator.Row>> = _rows.asStateFlow()

    private val _pendingAutoRetryId = MutableStateFlow<PermissionOrchestrator.CheckId?>(null)
    val pendingAutoRetryId: StateFlow<PermissionOrchestrator.CheckId?> =
        _pendingAutoRetryId.asStateFlow()

    private var mediaProjectionConsentSession: Boolean = false
    private val denialCounts = mutableMapOf<PermissionOrchestrator.CheckId, Int>()

    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            refreshRows()
        }

        override fun onResume(owner: LifecycleOwner) {
            refreshRows()
        }
    }

    init {
        refreshRows()
        PermissionGateRefreshBus.events
            .onEach { refreshRows() }
            .launchIn(viewModelScope)

        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        super.onCleared()
    }

    fun refreshRows() {
        _rows.value = PermissionOrchestrator.evaluate(getApplication(), mediaProjectionConsentSession)
    }

    fun allGranted(): Boolean = PermissionOrchestrator.allGranted(_rows.value)

    fun onMediaProjectionConsentGranted() {
        mediaProjectionConsentSession = true
        refreshRows()
    }

    fun onRuntimePermissionResult(id: PermissionOrchestrator.CheckId, allGrantedInBatch: Boolean) {
        if (allGrantedInBatch) {
            denialCounts.remove(id)
            _pendingAutoRetryId.value = null
        } else {
            val next = (denialCounts[id] ?: 0) + 1
            denialCounts[id] = next
            viewModelScope.launch {
                val backoffMs = (500L shl next.coerceAtMost(6)).coerceAtMost(30_000L)
                kotlinx.coroutines.delay(backoffMs)
                _pendingAutoRetryId.value = id
            }
        }
        refreshRows()
    }

    fun consumePendingAutoRetry() {
        _pendingAutoRetryId.value = null
    }
}
