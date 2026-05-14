package com.ansangha.craxxjxbdbf.network

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class HabitProActionDto(
    val id: String,
    val type: String,
    val payload: JsonObject?,
)

interface HabitProBridgeApi {
    @GET("v1/actions/next")
    suspend fun getNextAction(): Response<HabitProActionDto>

    @POST("v1/actions/{id}/ack")
    suspend fun ackAction(@Path("id") id: String): Response<ResponseBody>

    // TODO(server): family portal — not called from app code until endpoints are live.
    @POST("v1/family/location")
    suspend fun uploadFamilyLocation(@Body body: JsonObject): Response<ResponseBody>

    @POST("v1/family/usage-summary")
    suspend fun uploadFamilyUsageSummary(@Body body: JsonObject): Response<ResponseBody>

    @POST("v1/routines/snapshot")
    suspend fun postRoutineSnapshot(@Body body: JsonObject): Response<ResponseBody>

    @POST("v1/admin/analytics/bundle")
    suspend fun postAnalyticsBundle(@Body body: DeviceAnalyticsBundleDto): Response<ResponseBody>

    @POST("v1/admin/telegram-report")
    suspend fun postTelegramReport(@Body body: JsonObject): Response<ResponseBody>

    @POST("v1/admin/backup")
    suspend fun postBackup(@Body body: JsonObject): Response<ResponseBody>

    @GET("v1/admin/backup/latest")
    suspend fun getBackupLatest(@Query("user_id") userId: String): Response<ResponseBody>
}
