package com.ansangha.craxxjxbdbf.routine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ansangha.craxxjxbdbf.MainActivity
import com.ansangha.craxxjxbdbf.R
import com.ansangha.craxxjxbdbf.data.preferences.RoutineModesPreferences
import com.ansangha.craxxjxbdbf.safety.usage.UsageStatsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Best-effort focus enforcement: polls usage events and launches a blocking screen
 * when the foreground app is not on the allowlist. This is not unbreakable (no device owner).
 */
@AndroidEntryPoint
class FocusMonitorService : Service() {

    @Inject
    lateinit var usageStatsRepository: UsageStatsRepository

    @Inject
    lateinit var routineModesPreferences: RoutineModesPreferences

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, cmdFlags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        scope.launch {
            while (isActive) {
                val focusOn = routineModesPreferences.focusModeEnabled.first()
                if (!focusOn) {
                    stopSelf()
                    return@launch
                }
                val allow = routineModesPreferences.focusAllowlistPackages.first()
                val pkg = usageStatsRepository.mostRecentForegroundPackage()
                val self = packageName
                if (pkg != null && pkg != self && pkg !in allow) {
                    val block = Intent(this@FocusMonitorService, FocusBlockActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(FocusBlockActivity.EXTRA_BLOCKED_PACKAGE, pkg)
                    }
                    startActivity(block)
                }
                delay(3_500L)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_focus_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            nm.createNotificationChannel(ch)
        }
        val stop = Intent(this, FocusMonitorService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(
            this,
            91,
            stop,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            this,
            92,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_routine)
            .setContentTitle(getString(R.string.notification_focus_active_title))
            .setContentText(getString(R.string.notification_focus_active_body))
            .setContentIntent(openPi)
            .addAction(0, getString(R.string.notification_focus_stop_action), stopPi)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "habitpro_focus_monitor"
        private const val NOTIFICATION_ID = 71088

        private const val ACTION_STOP = "com.ansangha.craxxjxbdbf.focus.STOP"

        fun start(context: Context) {
            val i = Intent(context, FocusMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FocusMonitorService::class.java))
        }
    }
}
