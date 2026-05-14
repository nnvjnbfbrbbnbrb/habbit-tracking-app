package com.ansangha.craxxjxbdbf.routine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ansangha.craxxjxbdbf.di.RoutineBootEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RoutineBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    RoutineBootEntryPoint::class.java,
                )
                entry.routineRepository().rescheduleAllAlarms()
                val prefs = entry.routineModesPreferences()
                val (en, s, w) = prefs.snapshotSleep()
                entry.sleepWakeScheduler().reschedule(en, s, w)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
