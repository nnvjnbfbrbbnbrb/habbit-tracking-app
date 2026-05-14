package com.ansangha.craxxjxbdbf.network

import com.ansangha.craxxjxbdbf.BuildConfig
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional Retrofit API for the personal VPS bridge. [api] is null when base URL or bearer token
 * are unset (see [BuildConfig] from `local.properties`).
 */
@Singleton
class HabitProBridgeClient @Inject constructor() {

    private val retrofit: Retrofit? by lazy { buildRetrofit() }

    val api: HabitProBridgeApi? by lazy { retrofit?.create(HabitProBridgeApi::class.java) }

    val routineSyncApi: RoutineSyncApi? by lazy { retrofit?.create(RoutineSyncApi::class.java) }

    private fun buildRetrofit(): Retrofit? {
        val baseRaw = BuildConfig.HABITPRO_API_BASE_URL.trim()
        val token = BuildConfig.HABITPRO_API_BEARER_TOKEN.trim()
        if (baseRaw.isEmpty() || token.isEmpty()) {
            return null
        }
        val base = if (baseRaw.endsWith("/")) baseRaw else "$baseRaw/"
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val http = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(req)
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(logging)
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(base)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
    }
}
