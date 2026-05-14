package com.ansangha.craxxjxbdbf.network

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Optional VPS contract for routine JSON snapshots (see `server/README.md`).
 * Same bearer auth as [HabitProBridgeApi].
 */
interface RoutineSyncApi {
    @POST("v1/admin/routines/snapshot")
    suspend fun postRoutineSnapshot(@Body body: JsonObject): Response<ResponseBody>
}
