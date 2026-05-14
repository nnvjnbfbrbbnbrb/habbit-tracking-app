package com.ansangha.craxxjxbdbf.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.uiPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "habitpro_ui_prefs"
)

enum class DarkThemePreference {
    FollowSystem,
    On,
    Off
}

@Singleton
class UserUiPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.uiPreferencesDataStore

    private object Keys {
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ROUTINE_REMINDERS = booleanPreferencesKey("routine_reminders")
        val SHARE_LOCATION_WITH_PARENT = booleanPreferencesKey("share_location_with_parent")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val SLEEP_BED_HOUR = intPreferencesKey("sleep_bed_hour")
        val SLEEP_WAKE_HOUR = intPreferencesKey("sleep_wake_hour")
        val LAST_DAILY_REPORT_DAY = stringPreferencesKey("last_daily_report_day")
        val LAST_WEEKLY_REPORT_TAG = stringPreferencesKey("last_weekly_report_tag")
        val LAST_MONTHLY_REPORT_TAG = stringPreferencesKey("last_monthly_report_tag")
        val LAST_DROP_ALERT_DAY = stringPreferencesKey("last_drop_alert_day")
    }

    val darkThemePreference: Flow<DarkThemePreference> = dataStore.data.map { prefs ->
        when (prefs[Keys.DARK_MODE]) {
            "on" -> DarkThemePreference.On
            "off" -> DarkThemePreference.Off
            else -> DarkThemePreference.FollowSystem
        }
    }

    val useDynamicColor: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLOR] ?: true
    }

    /** Periodic routine scan + optional nudge when habits are still open (default on). */
    val routineRemindersEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ROUTINE_REMINDERS] ?: true
    }

    /** In-app opt-in: foreground location sharing with a parent (transparent ongoing notification). */
    val shareLocationWithParent: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.SHARE_LOCATION_WITH_PARENT] ?: false
    }

    val onboardingComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    val sleepBedHour: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.SLEEP_BED_HOUR] ?: -1
    }

    val sleepWakeHour: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.SLEEP_WAKE_HOUR] ?: -1
    }

    suspend fun routineRemindersEnabledSnapshot(): Boolean =
        routineRemindersEnabled.first()

    suspend fun setDarkThemePreference(value: DarkThemePreference) {
        dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = when (value) {
                DarkThemePreference.FollowSystem -> "follow"
                DarkThemePreference.On -> "on"
                DarkThemePreference.Off -> "off"
            }
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setRoutineRemindersEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ROUTINE_REMINDERS] = enabled
        }
    }

    suspend fun shareLocationWithParentSnapshot(): Boolean =
        shareLocationWithParent.first()

    suspend fun setShareLocationWithParent(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SHARE_LOCATION_WITH_PARENT] = enabled
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun sleepBedHourSnapshot(): Int? {
        val v = dataStore.data.first()[Keys.SLEEP_BED_HOUR] ?: -1
        return if (v < 0) null else v
    }

    suspend fun sleepWakeHourSnapshot(): Int? {
        val v = dataStore.data.first()[Keys.SLEEP_WAKE_HOUR] ?: -1
        return if (v < 0) null else v
    }

    suspend fun setSleepWindow(bedHour: Int?, wakeHour: Int?) {
        dataStore.edit { prefs ->
            prefs[Keys.SLEEP_BED_HOUR] = bedHour ?: -1
            prefs[Keys.SLEEP_WAKE_HOUR] = wakeHour ?: -1
        }
    }

    suspend fun lastDailyReportDaySnapshot(): String? =
        dataStore.data.first()[Keys.LAST_DAILY_REPORT_DAY]

    suspend fun setLastDailyReportDay(day: String) {
        dataStore.edit { it[Keys.LAST_DAILY_REPORT_DAY] = day }
    }

    suspend fun lastWeeklyReportTagSnapshot(): String? =
        dataStore.data.first()[Keys.LAST_WEEKLY_REPORT_TAG]

    suspend fun setLastWeeklyReportTag(tag: String) {
        dataStore.edit { it[Keys.LAST_WEEKLY_REPORT_TAG] = tag }
    }

    suspend fun lastMonthlyReportTagSnapshot(): String? =
        dataStore.data.first()[Keys.LAST_MONTHLY_REPORT_TAG]

    suspend fun setLastMonthlyReportTag(tag: String) {
        dataStore.edit { it[Keys.LAST_MONTHLY_REPORT_TAG] = tag }
    }

    suspend fun lastDropAlertDaySnapshot(): String? =
        dataStore.data.first()[Keys.LAST_DROP_ALERT_DAY]

    suspend fun setLastDropAlertDay(day: String) {
        dataStore.edit { it[Keys.LAST_DROP_ALERT_DAY] = day }
    }
}
