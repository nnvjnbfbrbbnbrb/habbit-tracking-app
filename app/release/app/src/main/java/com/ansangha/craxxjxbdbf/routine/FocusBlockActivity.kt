package com.ansangha.craxxjxbdbf.routine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ansangha.craxxjxbdbf.data.preferences.RoutineModesPreferences
import com.ansangha.craxxjxbdbf.ui.theme.HabitTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Transparent, honest blocking surface: explains why the phone is paused during focus.
 * Not a security boundary — users can force-stop the app or revoke usage access.
 */
@AndroidEntryPoint
class FocusBlockActivity : ComponentActivity() {

    @Inject
    lateinit var routineModesPreferences: RoutineModesPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val blocked = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()
        setContent {
            HabitTrackerTheme(
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = true,
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Focus mode",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "Only allowed apps are available during study. " +
                                "This screen appears when another app comes to the foreground.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Text(
                            text = "Detected app: $blocked",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        Button(
                            onClick = {
                                runBlocking { routineModesPreferences.setFocusMode(false) }
                                FocusMonitorService.stop(this@FocusBlockActivity)
                                finish()
                            },
                            modifier = Modifier.padding(top = 24.dp),
                        ) {
                            Text("Turn off focus mode")
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_pkg"
    }
}
