package com.ansangha.craxxjxbdbf.ui.viewmodel

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ansangha.craxxjxbdbf.data.preferences.UserUiPreferences
import com.ansangha.craxxjxbdbf.safety.location.LocationForegroundService
import com.ansangha.craxxjxbdbf.safety.usage.UsageStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FamilySafetyViewModel @Inject constructor(
    application: Application,
    private val userUiPreferences: UserUiPreferences,
    private val usageStatsRepository: UsageStatsRepository,
) : AndroidViewModel(application) {

    val shareLocationWithParent: StateFlow<Boolean> = userUiPreferences.shareLocationWithParent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _usageTodayText = MutableStateFlow(
        if (usageStatsRepository.hasUsageAccess()) {
            usageStatsRepository.todayTotalsDisplayText()
        } else {
            ""
        },
    )
    val usageTodayText: StateFlow<String> = _usageTodayText.asStateFlow()

    val usageAccessGranted: Boolean
        get() = usageStatsRepository.hasUsageAccess()

    fun refreshUsageTodayText() {
        _usageTodayText.value = usageStatsRepository.todayTotalsDisplayText()
    }

    fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    fun needsPostNotificationsRuntime(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun hasPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Syncs preference with foreground service. Call after permission grants or when Studio opens.
     */
    fun syncLocationServiceWithPreference() {
        viewModelScope.launch {
            val want = userUiPreferences.shareLocationWithParentSnapshot()
            val ctx = getApplication<Application>()
            if (!want) {
                LocationForegroundService.stop(ctx)
                return@launch
            }
            if (!hasFineLocation() || !hasPostNotifications()) {
                return@launch
            }
            if (!isLocationServiceRunning(ctx)) {
                LocationForegroundService.start(ctx)
            }
        }
    }

    fun onShareLocationToggle(
        enabled: Boolean,
        onRequestFineLocation: () -> Unit,
        onRequestPostNotifications: () -> Unit,
    ) {
        viewModelScope.launch {
            if (!enabled) {
                userUiPreferences.setShareLocationWithParent(false)
                LocationForegroundService.stop(getApplication())
                return@launch
            }
            userUiPreferences.setShareLocationWithParent(true)
            if (!hasFineLocation()) {
                onRequestFineLocation()
                return@launch
            }
            if (needsPostNotificationsRuntime() && !hasPostNotifications()) {
                onRequestPostNotifications()
                return@launch
            }
            LocationForegroundService.start(getApplication())
        }
    }

    fun onFineLocationPermissionResult(
        granted: Boolean,
        onNeedPostNotifications: () -> Unit,
    ) {
        viewModelScope.launch {
            if (!granted) {
                userUiPreferences.setShareLocationWithParent(false)
                LocationForegroundService.stop(getApplication())
                return@launch
            }
            if (!userUiPreferences.shareLocationWithParentSnapshot()) return@launch
            if (needsPostNotificationsRuntime() && !hasPostNotifications()) {
                onNeedPostNotifications()
                return@launch
            }
            LocationForegroundService.start(getApplication())
        }
    }

    fun onPostNotificationsPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (!granted) {
                userUiPreferences.setShareLocationWithParent(false)
                LocationForegroundService.stop(getApplication())
                return@launch
            }
            if (!userUiPreferences.shareLocationWithParentSnapshot()) return@launch
            if (!hasFineLocation()) return@launch
            LocationForegroundService.start(getApplication())
        }
    }

    @Suppress("DEPRECATION")
    private fun isLocationServiceRunning(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.getRunningServices(64).any { running ->
            running.service.className == LocationForegroundService::class.java.name
        }
    }
}
