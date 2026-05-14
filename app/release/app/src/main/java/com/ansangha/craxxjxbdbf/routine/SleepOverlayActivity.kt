package com.ansangha.craxxjxbdbf.routine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ansangha.craxxjxbdbf.ui.theme.HabitTrackerTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Child-visible sleep screen. Dismisses automatically after the scheduled wake time.
 * Not device-admin lock; users can still use system navigation depending on OEM.
 */
class SleepOverlayActivity : ComponentActivity() {

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_FINISH_SLEEP) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        val wakeAt = intent.getLongExtra(EXTRA_WAKE_AT_MILLIS, System.currentTimeMillis() + 60_000L)
        val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
        val wakeLabel = formatter.format(Instant.ofEpochMilli(wakeAt))
        setContent {
            HabitTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Sleep time",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "Your phone is resting until about $wakeLabel so mornings stay calmer. " +
                                "You can still use the power button for emergencies.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
        val filter = IntentFilter(ACTION_FINISH_SLEEP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(finishReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(finishReceiver, filter)
        }
    }

    override fun onDestroy() {
        unregisterReceiverSafe()
        super.onDestroy()
    }

    private fun unregisterReceiverSafe() {
        runCatching { unregisterReceiver(finishReceiver) }
    }

    companion object {
        const val EXTRA_WAKE_AT_MILLIS = "wake_at_millis"
        const val ACTION_FINISH_SLEEP = "com.ansangha.craxxjxbdbf.action.FINISH_SLEEP"

        fun launch(context: Context, wakeAtMillis: Long) {
            val i = Intent(context, SleepOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WAKE_AT_MILLIS, wakeAtMillis)
            }
            context.startActivity(i)
        }
    }
}
