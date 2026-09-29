package com.example.snake.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class CloudPlayerData(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val highScore: Int = 0,
    val totalCoins: Int = 0,
    val unlockedLevel: Int = 1,
    val levelStars: Map<String, Long> = emptyMap(),
    val unlockedFloorIds: List<String> = listOf("classic"),
    val selectedFloorId: String = "classic",
    val updatedAt: Long = System.currentTimeMillis()
)

class FirestoreSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "FirestoreSyncManager"
        private const val COLLECTION_USERS = "users"
    }

    private val firestore: FirebaseFirestore by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseApp initialization error: ${e.message}")
        }
        FirebaseFirestore.getInstance()
    }

    suspend fun fetchPlayerData(uid: String): Result<CloudPlayerData?> {
        return try {
            val doc = firestore.collection(COLLECTION_USERS)
                .document(uid)
                .get()
                .await()

            if (!doc.exists()) {
                Result.success(null)
            } else {
                val data = doc.data ?: return Result.success(null)
                val starsRaw = (data["levelStars"] as? Map<*, *>) ?: emptyMap<Any, Any>()
                val levelStarsMap = starsRaw.entries.mapNotNull { entry ->
                    val k = entry.key?.toString() ?: return@mapNotNull null
                    val v = (entry.value as? Number)?.toLong() ?: 0L
                    k to v
                }.toMap()

                val floorListRaw = (data["unlockedFloorIds"] as? List<*>) ?: emptyList<Any>()
                val floorIds = floorListRaw.mapNotNull { it?.toString() }

                val cloudData = CloudPlayerData(
                    uid = uid,
                    displayName = data["displayName"]?.toString().orEmpty(),
                    email = data["email"]?.toString().orEmpty(),
                    highScore = (data["highScore"] as? Number)?.toInt() ?: 0,
                    totalCoins = (data["totalCoins"] as? Number)?.toInt() ?: 0,
                    unlockedLevel = (data["unlockedLevel"] as? Number)?.toInt() ?: 1,
                    levelStars = levelStarsMap,
                    unlockedFloorIds = if (floorIds.isEmpty()) listOf("classic") else floorIds,
                    selectedFloorId = data["selectedFloorId"]?.toString() ?: "classic",
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                Result.success(cloudData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching player cloud data for uid $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun savePlayerData(user: FirebaseUser, local: ProgressData): Result<Unit> {
        return try {
            val starsMap = local.levelStars.mapKeys { it.key.toString() }.mapValues { it.value.toLong() }
            val map = hashMapOf(
                "uid" to user.uid,
                "displayName" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "highScore" to local.highScore,
                "totalCoins" to local.totalCoins,
                "unlockedLevel" to local.unlockedLevel,
                "levelStars" to starsMap,
                "unlockedFloorIds" to local.unlockedFloorIds.toList(),
                "selectedFloorId" to local.selectedFloorId,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection(COLLECTION_USERS)
                .document(user.uid)
                .set(map, SetOptions.merge())
                .await()

            Log.i(TAG, "Player data saved successfully to Firestore for ${user.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving player data to Firestore for ${user.uid}: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun mergeProgress(local: ProgressData, cloud: CloudPlayerData): ProgressData {
        val mergedStars = local.levelStars.toMutableMap()
        cloud.levelStars.forEach { (keyStr, starVal) ->
            val lvl = keyStr.toIntOrNull() ?: return@forEach
            val currentStar = mergedStars[lvl] ?: 0
            if (starVal.toInt() > currentStar) {
                mergedStars[lvl] = starVal.toInt()
            }
        }

        val mergedFloors = (local.unlockedFloorIds + cloud.unlockedFloorIds).toSet()

        return local.copy(
            highScore = maxOf(local.highScore, cloud.highScore),
            totalCoins = maxOf(local.totalCoins, cloud.totalCoins),
            unlockedLevel = maxOf(local.unlockedLevel, cloud.unlockedLevel),
            levelStars = mergedStars,
            unlockedFloorIds = mergedFloors,
            selectedFloorId = if (cloud.selectedFloorId in mergedFloors) cloud.selectedFloorId else local.selectedFloorId
        )
    }
}
