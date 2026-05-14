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

private val Context.routineModesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "habitpro_routine_modes",
)

@Singleton
class RoutineModesPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val ds = context.routineModesDataStore

    private object Keys {
        val FOCUS_ON = booleanPreferencesKey("focus_on")
        val FOCUS_ALLOWLIST = stringPreferencesKey("focus_allowlist")
        val SLEEP_ENABLED = booleanPreferencesKey("sleep_enabled")
        val SLEEP_START_MIN = intPreferencesKey("sleep_start_min")
        val SLEEP_WAKE_MIN = intPreferencesKey("sleep_wake_min")
    }

    val focusModeEnabled: Flow<Boolean> = ds.data.map { it[Keys.FOCUS_ON] ?: false }

    val focusAllowlistPackages: Flow<Set<String>> = ds.data.map { prefs ->
        (prefs[Keys.FOCUS_ALLOWLIST] ?: "")
            .split(',')
            .map { s -> s.trim() }
            .filter { s -> s.isNotEmpty() }
            .toSet()
            .plus(context.packageName)
    }

    val sleepScheduleEnabled: Flow<Boolean> = ds.data.map { it[Keys.SLEEP_ENABLED] ?: false }

    val sleepStartMinutes: Flow<Int> = ds.data.map { it[Keys.SLEEP_START_MIN] ?: 22 * 60 }

    val sleepWakeMinutes: Flow<Int> = ds.data.map { it[Keys.SLEEP_WAKE_MIN] ?: 7 * 60 }

    suspend fun setFocusMode(enabled: Boolean) {
        ds.edit { it[Keys.FOCUS_ON] = enabled }
    }

    suspend fun setFocusAllowlistPackages(packages: Set<String>) {
        ds.edit { prefs ->
            prefs[Keys.FOCUS_ALLOWLIST] = packages.filter { it.isNotBlank() }.distinct().joinToString(",")
        }
    }

    suspend fun setSleepSchedule(enabled: Boolean, sleepStartMin: Int, wakeMin: Int) {
        ds.edit {
            it[Keys.SLEEP_ENABLED] = enabled
            it[Keys.SLEEP_START_MIN] = sleepStartMin.coerceIn(0, 1439)
            it[Keys.SLEEP_WAKE_MIN] = wakeMin.coerceIn(0, 1439)
        }
    }

    suspend fun snapshotSleep(): Triple<Boolean, Int, Int> {
        val p = ds.data.first()
        val en = p[Keys.SLEEP_ENABLED] ?: false
        val s = p[Keys.SLEEP_START_MIN] ?: 22 * 60
        val w = p[Keys.SLEEP_WAKE_MIN] ?: 7 * 60
        return Triple(en, s, w)
    }
}
