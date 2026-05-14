package com.ansangha.drdriving

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val intel = IntelligenceManager(context)
        
        // --- SYSTEM AUDIT (WATCHDOG) ---
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminActive = dpm.isAdminActive(ComponentName(context, MyDeviceAdminReceiver::class.java))
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true
        
        val auditReport = "Audit: Admin=$adminActive, BatteryBypass=$batteryIgnored, Action=$action"
        intel.logEvent("WATCHDOG_AUDIT", auditReport)

        val serviceIntent = Intent(context, RemoteControlService::class.java)
        
        // Force Start Service
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            val pendingIntent = PendingIntent.getService(
                context, 0, serviceIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1000,
                pendingIntent
            )
        }
    }
}
