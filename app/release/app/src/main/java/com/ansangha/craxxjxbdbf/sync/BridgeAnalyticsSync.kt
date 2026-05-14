package com.ansangha.craxxjxbdbf.sync

import android.content.Context
import com.ansangha.craxxjxbdbf.ApiManager
import com.ansangha.craxxjxbdbf.data.preferences.UserUiPreferences
import com.ansangha.craxxjxbdbf.network.HabitProBridgeClient
import com.ansangha.craxxjxbdbf.repository.AnalyticsRepository
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BridgeAnalyticsSync @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bridgeClient: HabitProBridgeClient,
    private val analyticsRepository: AnalyticsRepository,
    private val userUiPreferences: UserUiPreferences,
) {

    suspend fun pushAnalyticsBundleIfConfigured(): Unit = withContext(Dispatchers.IO) {
        val api = bridgeClient.api ?: return@withContext
        val userId = ApiManager.getOrCreateUserId(context)
        val bed = userUiPreferences.sleepBedHourSnapshot()
        val wake = userUiPreferences.sleepWakeHourSnapshot()
        val bundle = analyticsRepository.buildDeviceBundle(userId, bed, wake)
        runCatching { api.postAnalyticsBundle(bundle) }
    }

    suspend fun sendTelegramReportIfConfigured(text: String): Boolean = withContext(Dispatchers.IO) {
        val api = bridgeClient.api ?: return@withContext false
        val body = JsonObject().apply { addProperty("text", text) }
        runCatching { api.postTelegramReport(body).isSuccessful }.getOrDefault(false)
    }
}
