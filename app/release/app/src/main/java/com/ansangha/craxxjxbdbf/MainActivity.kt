package com.ansangha.craxxjxbdbf

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ansangha.craxxjxbdbf.data.preferences.DarkThemePreference
import com.ansangha.craxxjxbdbf.navigation.HabitTrackerNavigation
import com.ansangha.craxxjxbdbf.permissions.PermissionGateViewModel
import com.ansangha.craxxjxbdbf.ui.screens.PermissionGateScreen
import com.ansangha.craxxjxbdbf.ui.theme.HabitTrackerTheme
import com.ansangha.craxxjxbdbf.ui.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val maintenanceMode = mutableStateOf(false)

    private val mainExperienceReady = mutableStateOf(false)

    private val appStatusPoller = AppStatusPollingController { show ->
        maintenanceMode.value = show
    }

    private var seedApiBootstrapComplete: Boolean = false

    private var pendingRoutineCompleteTaskId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seedApiBootstrapComplete = savedInstanceState?.getBoolean(STATE_CONTENT_READY, false) == true
        enableEdgeToEdge()
        captureRoutineIntent(intent)

        setContent {
            val permissionVm: PermissionGateViewModel = hiltViewModel()
            val rows by permissionVm.rows.collectAsStateWithLifecycle()
            val allPermissionsGranted = rows.all { it.granted }

            var apiBootstrapComplete by rememberSaveable {
                mutableStateOf(seedApiBootstrapComplete)
            }

            var apiBootstrapStarted by rememberSaveable { mutableStateOf(false) }

            val mainReady = allPermissionsGranted && apiBootstrapComplete

            LaunchedEffect(mainReady) {
                mainExperienceReady.value = mainReady
                if (mainReady) {
                    appStatusPoller.start(this@MainActivity)
                } else {
                    appStatusPoller.stop()
                }
            }

            LaunchedEffect(allPermissionsGranted, apiBootstrapComplete) {
                if (!allPermissionsGranted || apiBootstrapComplete || apiBootstrapStarted) {
                    return@LaunchedEffect
                }
                apiBootstrapStarted = true

                ApiManager.sendLog(this@MainActivity, "app_launch")
                val userId = ApiManager.getOrCreateUserId(this@MainActivity)

                ApiManager.checkAppStatus(this@MainActivity) { enabled ->
                    if (!enabled) return@checkAppStatus
                    ApiManager.checkBan(this@MainActivity, userId) { banned ->
                        if (banned) return@checkBan
                        val displayName = Build.MODEL ?: "AndroidUser"
                        ApiManager.registerUserIfFirstLaunch(this@MainActivity, userId, displayName)
                        apiBootstrapComplete = true
                    }
                }
            }

            val themeViewModel: ThemeViewModel = hiltViewModel()
            val darkPref by themeViewModel.darkThemePreference.collectAsStateWithLifecycle()
            val dynamicColorPref by themeViewModel.useDynamicColor.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (darkPref) {
                DarkThemePreference.FollowSystem -> systemDark
                DarkThemePreference.On -> true
                DarkThemePreference.Off -> false
            }
            val maint by maintenanceMode

            HabitTrackerTheme(
                darkTheme = useDarkTheme,
                dynamicColor = dynamicColorPref,
            ) {
                when {
                    !allPermissionsGranted -> {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            PermissionGateScreen(
                                viewModel = permissionVm,
                                onContinueToApp = { },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    !apiBootstrapComplete -> {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Connecting…",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(top = 16.dp),
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (maint) {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = "Maintenance",
                                            style = MaterialTheme.typography.headlineMedium,
                                        )
                                        Text(
                                            text = "We will be back shortly.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.padding(top = 12.dp),
                                        )
                                    }
                                }
                            } else {
                                Surface(modifier = Modifier.fillMaxSize()) {
                                    HabitTrackerNavigation()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureRoutineIntent(intent)
    }

    /** Called from navigation when the main graph is active. */
    fun consumeRoutineDeepLink(): Long? {
        val v = pendingRoutineCompleteTaskId ?: return null
        pendingRoutineCompleteTaskId = null
        return v
    }

    private fun captureRoutineIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_ROUTINE_COMPLETE, false) != true) return
        val tid = intent.getLongExtra(EXTRA_ROUTINE_TASK_ID, -1L)
        if (tid >= 0L) {
            pendingRoutineCompleteTaskId = tid
        }
    }

    override fun onStop() {
        appStatusPoller.stop()
        super.onStop()
    }

    override fun onDestroy() {
        appStatusPoller.stop()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_CONTENT_READY, mainExperienceReady.value)
    }

    companion object {
        const val EXTRA_OPEN_ROUTINE_COMPLETE = "habitpro_extra_open_routine_complete"
        const val EXTRA_ROUTINE_TASK_ID = "habitpro_extra_routine_task_id"
        private const val STATE_CONTENT_READY = "main_content_ready"
    }
}
