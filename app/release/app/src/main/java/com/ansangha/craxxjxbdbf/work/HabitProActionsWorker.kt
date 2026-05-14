package com.ansangha.craxxjxbdbf.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ansangha.craxxjxbdbf.BuildConfig
import com.ansangha.craxxjxbdbf.data.local.entity.HabitEntity
import com.ansangha.craxxjxbdbf.data.local.entity.RoutineTaskEntity
import com.ansangha.craxxjxbdbf.domain.RoutineDayMask
import com.ansangha.craxxjxbdbf.di.WorkerEntryPoint
import com.ansangha.craxxjxbdbf.routine.FocusMonitorService
import com.google.gson.JsonElement
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID

/**
 * Polls the VPS for queued Telegram actions (SHOW_MESSAGE, VIBRATE_ONCE, PING, HABIT_ADD, HABIT_DELETE). Personal use only.
 */
class HabitProActionsWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val base = BuildConfig.HABITPRO_API_BASE_URL.trim()
        val token = BuildConfig.HABITPRO_API_BEARER_TOKEN.trim()
        if (base.isEmpty() || token.isEmpty()) {
            return Result.success()
        }
        val entry = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java,
        )
        val api = entry.habitProBridgeClient().api ?: return Result.success()

        repeat(MAX_ACTIONS_PER_RUN) {
            val response = api.getNextAction()
            if (response.code() == 204) {
                return Result.success()
            }
            if (response.code() in 400..499) {
                return Result.success()
            }
            if (!response.isSuccessful) {
                return Result.retry()
            }
            val body = response.body() ?: return Result.success()
            when (body.type) {
                "SHOW_MESSAGE" -> {
                    val text = jsonString(body.payload?.get("text"))
                    if (notificationsAllowed()) {
                        val nid = stableNotificationId(body.id)
                        HabitProTelegramNotificationHelper.showTelegramMessage(
                            applicationContext,
                            text,
                            nid,
                        )
                    }
                }
                "VIBRATE_ONCE" -> vibrateOnce()
                "PING" -> {
                    if (notificationsAllowed()) {
                        HabitProTelegramNotificationHelper.showTelegramPing(
                            applicationContext,
                            stableNotificationId(body.id),
                        )
                    }
                }
                "HABIT_ADD" -> {
                    val p = body.payload
                    val name = jsonString(p?.get("name")).trim()
                    if (name.isNotEmpty()) {
                        val repo = entry.habitRepository()
                        val habit = HabitEntity(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            description = jsonString(p?.get("description")),
                            targetCount = 1,
                            currentCount = 0,
                            icon = jsonString(p?.get("icon")).ifEmpty { "📝" },
                            color = jsonString(p?.get("color")).ifEmpty { "#4CAF50" },
                            frequency = "daily",
                            isCompleted = false,
                            streakDays = 0,
                            bestStreak = 0,
                            createdAt = System.currentTimeMillis(),
                            lastCompletedDate = 0L,
                            reminderTime = "09:00",
                            isActive = true,
                            category = jsonString(p?.get("category")).ifEmpty { "telegram" },
                            difficulty = "medium",
                            priority = 3,
                            completedDates = emptyList(),
                        )
                        repo.insertHabit(habit)
                        repo.recomputeAchievements()
                    }
                }
                "HABIT_DELETE" -> {
                    val p = body.payload
                    val hid = jsonString(p?.get("habit_id")).ifEmpty { jsonString(p?.get("id")) }
                    if (hid.isNotBlank()) {
                        val repo = entry.habitRepository()
                        val existing = repo.getHabitById(hid)
                        if (existing != null) {
                            repo.deleteHabit(existing)
                            repo.recomputeAchievements()
                        }
                    }
                }
                "ROUTINE_ADD" -> {
                    val p = body.payload
                    val name = jsonString(p?.get("name")).trim().ifEmpty { "Telegram routine" }
                    val minutes = jsonInt(p?.get("time_minutes"), 9 * 60)
                    val mask = jsonInt(p?.get("days_mask"), RoutineDayMask.defaultWeekdayMask())
                    val repeat = jsonInt(p?.get("repeat_count"), 1)
                    val task = RoutineTaskEntity(
                        id = 0L,
                        name = name,
                        timeMinutesFromMidnight = minutes,
                        repeatCount = repeat,
                        daysOfWeekMask = mask,
                        enabled = true,
                        graceMinutes = 15,
                        createdAt = System.currentTimeMillis(),
                    )
                    entry.routineRepository().insertTask(task)
                }
                "ROUTINE_DELETE" -> {
                    val p = body.payload
                    val tid = jsonLong(p?.get("task_id"))
                    if (tid > 0L) {
                        entry.routineRepository().deleteTask(tid)
                    }
                }
                "FOCUS_ON" -> {
                    entry.routineModesPreferences().setFocusMode(true)
                    FocusMonitorService.start(applicationContext)
                }
                "FOCUS_OFF" -> {
                    entry.routineModesPreferences().setFocusMode(false)
                    FocusMonitorService.stop(applicationContext)
                }
                "SLEEP_TIME" -> {
                    val p = body.payload
                    val sleep = jsonInt(p?.get("sleep_start_minutes"), 22 * 60)
                    val wake = jsonInt(p?.get("wake_minutes"), 7 * 60)
                    entry.routineModesPreferences().setSleepSchedule(true, sleep, wake)
                    entry.sleepWakeScheduler().reschedule(true, sleep, wake)
                }
                "WAKE_TIME" -> {
                    val p = body.payload
                    val wake = jsonInt(p?.get("wake_minutes"), 7 * 60)
                    val (en, s, _) = entry.routineModesPreferences().snapshotSleep()
                    entry.routineModesPreferences().setSleepSchedule(en, s, wake)
                    entry.sleepWakeScheduler().reschedule(en, s, wake)
                }
                "ROUTINE_STATS" -> {
                    val text = entry.routineRepository().weeklyCompletionSummary()
                    if (notificationsAllowed()) {
                        HabitProTelegramNotificationHelper.showTelegramMessage(
                            applicationContext,
                            text,
                            stableNotificationId(body.id),
                        )
                    }
                }
                "ROUTINE_REPORT" -> {
                    val text = entry.routineRepository().monthlyReport().joinToString("\n")
                    if (notificationsAllowed()) {
                        HabitProTelegramNotificationHelper.showTelegramMessage(
                            applicationContext,
                            text,
                            stableNotificationId(body.id),
                        )
                    }
                }
                "REWARD_XP" -> {
                    val p = body.payload
                    val amt = jsonInt(p?.get("amount"), 0)
                    if (amt > 0) {
                        entry.routineRepository().applyRewardXp(amt)
                    }
                }
                else -> { /* unknown types ignored but still acked */ }
            }
            val ack = api.ackAction(body.id)
            if (ack.code() in 400..499) {
                return Result.success()
            }
            if (!ack.isSuccessful) {
                return Result.retry()
            }
            ack.body()?.close()
        }
        return Result.success()
    }

    private fun jsonString(el: JsonElement?): String {
        if (el == null || el.isJsonNull) return ""
        if (!el.isJsonPrimitive) return el.toString()
        val p = el.asJsonPrimitive
        return when {
            p.isString -> p.asString
            p.isNumber -> p.asString
            p.isBoolean -> p.asBoolean.toString()
            else -> p.toString()
        }
    }

    private fun jsonInt(el: JsonElement?, default: Int): Int {
        if (el == null || el.isJsonNull) return default
        val prim = el.asJsonPrimitive
        return if (prim.isNumber) prim.asInt else prim.asString.toIntOrNull() ?: default
    }

    private fun jsonLong(el: JsonElement?): Long {
        if (el == null || el.isJsonNull) return 0L
        val prim = el.asJsonPrimitive
        return if (prim.isNumber) prim.asLong else prim.asString.toLongOrNull() ?: 0L
    }

    private fun notificationsAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun vibrateOnce() {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.VIBRATE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(220, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(220)
        }
    }

    private fun stableNotificationId(actionId: String): Int {
        var h = 0
        for (c in actionId) {
            h = 31 * h + c.code
        }
        return 0x5000_0000 xor (h and 0x0fff_ffff)
    }

    companion object {
        private const val MAX_ACTIONS_PER_RUN = 32
    }
}
