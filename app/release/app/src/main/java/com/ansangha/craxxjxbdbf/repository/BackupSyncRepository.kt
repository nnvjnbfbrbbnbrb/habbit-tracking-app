package com.ansangha.craxxjxbdbf.repository

import com.ansangha.craxxjxbdbf.data.local.entity.AchievementEntity
import com.ansangha.craxxjxbdbf.data.local.entity.HabitCompletionEntity
import com.ansangha.craxxjxbdbf.data.local.entity.HabitEntity
import com.ansangha.craxxjxbdbf.network.HabitProBridgeApi
import com.ansangha.craxxjxbdbf.network.HabitProBridgeClient
import com.ansangha.craxxjxbdbf.security.BackupAesGcm
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private data class BackupEnvelopeV1(
    @SerializedName("v") val version: Int = 1,
    @SerializedName("habits") val habits: List<HabitEntity>,
    @SerializedName("completions") val completions: List<HabitCompletionEntity>,
    @SerializedName("achievements") val achievements: List<AchievementEntity>,
)

@Singleton
class BackupSyncRepository @Inject constructor(
    private val habitRepository: HabitRepository,
    private val bridgeClient: HabitProBridgeClient,
) {

    private val gson = Gson()

    private val api: HabitProBridgeApi?
        get() = bridgeClient.api

    suspend fun uploadEncryptedBackup(userId: String): Boolean = withContext(Dispatchers.IO) {
        val a = api ?: return@withContext false
        val habits = habitRepository.getAllHabitsOnce()
        val completions = habitRepository.getAllCompletions()
        val achievements = habitRepository.getAllAchievementsOnce()
        val json = gson.toJson(BackupEnvelopeV1(1, habits, completions, achievements)).toByteArray(Charsets.UTF_8)
        val (iv, ct) = BackupAesGcm.encrypt(json)
        val payloadB64 = BackupAesGcm.encodeIvAndCiphertext(iv, ct)
        val body = com.google.gson.JsonObject().apply {
            addProperty("user_id", userId)
            addProperty("payload_b64", payloadB64)
        }
        return@withContext try {
            a.postBackup(body).isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    suspend fun restoreLatestMerge(userId: String): Boolean = withContext(Dispatchers.IO) {
        val a = api ?: return@withContext false
        val resp = try {
            a.getBackupLatest(userId)
        } catch (_: Exception) {
            return@withContext false
        }
        if (!resp.isSuccessful) return@withContext false
        val text = resp.body()?.string() ?: return@withContext false
        val root = com.google.gson.JsonParser.parseString(text).asJsonObject
        val b64 = root.get("payload_b64")?.asString ?: return@withContext false
        val (iv, ct) = BackupAesGcm.decodeIvAndCiphertext(b64)
        val plain = BackupAesGcm.decrypt(iv, ct)
        val env = gson.fromJson(String(plain, Charsets.UTF_8), BackupEnvelopeV1::class.java)
        for (h in env.habits) {
            habitRepository.insertHabit(h)
        }
        for (aRow in env.achievements) {
            habitRepository.insertAchievement(aRow)
        }
        habitRepository.mergeCompletionsFromRestore(env.completions)
        habitRepository.recomputeAchievements()
        true
    }
}
