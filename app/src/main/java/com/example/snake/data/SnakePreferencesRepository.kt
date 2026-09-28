package com.example.snake.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.snake.model.GameDifficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.snakeDataStore: DataStore<Preferences> by preferencesDataStore(name = "snake_preferences")

data class ProgressData(
    val totalCoins: Int = 0,
    val highScore: Int = 0,
    val unlockedLevel: Int = 1,
    val levelStars: Map<Int, Int> = emptyMap(),
    val unlockedFloorIds: Set<String> = setOf("classic"),
    val selectedFloorId: String = "classic",
    val reduceAnimations: Boolean = false,
    val dpadScale: Float = 1.0f,
    val dpadOffsetX: Float = 0f,
    val dpadOffsetY: Float = 0f,
    val pauseButtonScale: Float = 1.0f,
    val leftHandedControls: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val wallCollision: Boolean = true,
    val difficulty: GameDifficulty = GameDifficulty.NORMAL
)

// Export alias for compatibility
typealias UserPreferences = ProgressData

class ProgressRepository(private val context: Context) {

    private object Keys {
        val TOTAL_COINS = intPreferencesKey("total_coins")
        val HIGH_SCORE = intPreferencesKey("high_score")
        val UNLOCKED_LEVEL = intPreferencesKey("unlocked_level")
        val LEVEL_STARS = stringPreferencesKey("level_stars")
        val UNLOCKED_FLOOR_IDS = stringSetPreferencesKey("unlocked_floor_ids")
        val SELECTED_FLOOR_ID = stringPreferencesKey("selected_floor_id")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        val DPAD_SCALE = floatPreferencesKey("dpad_scale")
        val DPAD_OFFSET_X = floatPreferencesKey("dpad_offset_x")
        val DPAD_OFFSET_Y = floatPreferencesKey("dpad_offset_y")
        val PAUSE_BUTTON_SCALE = floatPreferencesKey("pause_button_scale")
        val LEFT_HANDED_CONTROLS = booleanPreferencesKey("left_handed_controls")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val WALL_COLLISION = booleanPreferencesKey("wall_collision")
        val DIFFICULTY = stringPreferencesKey("difficulty")
    }

    private fun parseStars(raw: String?): Map<Int, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            raw.split(",").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val lvl = parts[0].trim().toIntOrNull()
                    val st = parts[1].trim().toIntOrNull()
                    if (lvl != null && st != null) lvl to st else null
                } else null
            }.toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun encodeStars(map: Map<Int, Int>): String {
        return map.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    val progressDataFlow: Flow<ProgressData> = context.snakeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val totalCoins = preferences[Keys.TOTAL_COINS] ?: 0
            val highScore = preferences[Keys.HIGH_SCORE] ?: 0
            val unlockedLevel = preferences[Keys.UNLOCKED_LEVEL] ?: 1
            val levelStars = parseStars(preferences[Keys.LEVEL_STARS])
            val unlockedFloors = preferences[Keys.UNLOCKED_FLOOR_IDS] ?: setOf("classic")
            val selectedFloor = preferences[Keys.SELECTED_FLOOR_ID] ?: "classic"
            val reduceAnims = preferences[Keys.REDUCE_ANIMATIONS] ?: false
            val dpadScale = preferences[Keys.DPAD_SCALE] ?: 1.0f
            val dpadOffsetX = preferences[Keys.DPAD_OFFSET_X] ?: 0f
            val dpadOffsetY = preferences[Keys.DPAD_OFFSET_Y] ?: 0f
            val pauseButtonScale = preferences[Keys.PAUSE_BUTTON_SCALE] ?: 1.0f
            val leftHanded = preferences[Keys.LEFT_HANDED_CONTROLS] ?: false
            val sound = preferences[Keys.SOUND_ENABLED] ?: true
            val haptics = preferences[Keys.HAPTICS_ENABLED] ?: true
            val wall = preferences[Keys.WALL_COLLISION] ?: true
            val diffName = preferences[Keys.DIFFICULTY] ?: GameDifficulty.NORMAL.name
            val difficulty = try {
                GameDifficulty.valueOf(diffName)
            } catch (_: Exception) {
                GameDifficulty.NORMAL
            }

            ProgressData(
                totalCoins = totalCoins,
                highScore = highScore,
                unlockedLevel = unlockedLevel,
                levelStars = levelStars,
                unlockedFloorIds = unlockedFloors,
                selectedFloorId = selectedFloor,
                reduceAnimations = reduceAnims,
                dpadScale = dpadScale,
                dpadOffsetX = dpadOffsetX,
                dpadOffsetY = dpadOffsetY,
                pauseButtonScale = pauseButtonScale,
                leftHandedControls = leftHanded,
                soundEnabled = sound,
                hapticsEnabled = haptics,
                wallCollision = wall,
                difficulty = difficulty
            )
        }

    val userPreferencesFlow: Flow<ProgressData> = progressDataFlow

    suspend fun addCoins(amount: Int) {
        if (amount <= 0) return
        context.snakeDataStore.edit { preferences ->
            val current = preferences[Keys.TOTAL_COINS] ?: 0
            preferences[Keys.TOTAL_COINS] = current + amount
        }
    }

    suspend fun spendCoins(amount: Int): Boolean {
        if (amount <= 0) return true
        var success = false
        context.snakeDataStore.edit { preferences ->
            val current = preferences[Keys.TOTAL_COINS] ?: 0
            if (current >= amount) {
                preferences[Keys.TOTAL_COINS] = current - amount
                success = true
            } else {
                success = false
            }
        }
        return success
    }

    suspend fun saveHighScore(score: Int) {
        context.snakeDataStore.edit { preferences ->
            val current = preferences[Keys.HIGH_SCORE] ?: 0
            if (score > current) {
                preferences[Keys.HIGH_SCORE] = score
            }
        }
    }

    suspend fun setUnlockedLevel(level: Int) {
        context.snakeDataStore.edit { preferences ->
            val current = preferences[Keys.UNLOCKED_LEVEL] ?: 1
            if (level > current) {
                preferences[Keys.UNLOCKED_LEVEL] = level
            }
        }
    }

    suspend fun saveLevelStars(levelId: Int, stars: Int) {
        context.snakeDataStore.edit { preferences ->
            val raw = preferences[Keys.LEVEL_STARS]
            val currentMap = parseStars(raw).toMutableMap()
            val bestSoFar = currentMap[levelId] ?: 0
            if (stars > bestSoFar) {
                currentMap[levelId] = stars
                preferences[Keys.LEVEL_STARS] = encodeStars(currentMap)
            }
        }
    }

    suspend fun unlockFloor(floorId: String) {
        context.snakeDataStore.edit { preferences ->
            val current = preferences[Keys.UNLOCKED_FLOOR_IDS] ?: setOf("classic")
            preferences[Keys.UNLOCKED_FLOOR_IDS] = current + floorId
        }
    }

    suspend fun selectFloor(floorId: String) {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.SELECTED_FLOOR_ID] = floorId
        }
    }

    suspend fun setReduceAnimations(reduce: Boolean) {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.REDUCE_ANIMATIONS] = reduce
        }
    }

    suspend fun setDpadScale(scale: Float) {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.DPAD_SCALE] = scale.coerceIn(0.7f, 1.5f)
        }
    }

    suspend fun setDpadOffset(offsetX: Float, offsetY: Float) {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.DPAD_OFFSET_X] = offsetX
            preferences[Keys.DPAD_OFFSET_Y] = offsetY
        }
    }

    suspend fun setPauseButtonScale(scale: Float) {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.PAUSE_BUTTON_SCALE] = scale.coerceIn(0.7f, 1.5f)
        }
    }

    suspend fun setLeftHandedControls(enabled: Boolean) {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.LEFT_HANDED_CONTROLS] = enabled
        }
    }

    suspend fun saveControls(
        dpadScale: Float,
        offsetX: Float,
        offsetY: Float,
        pauseScale: Float = 1.0f,
        leftHanded: Boolean = false
    ) {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.DPAD_SCALE] = dpadScale.coerceIn(0.7f, 1.5f)
            preferences[Keys.DPAD_OFFSET_X] = offsetX
            preferences[Keys.DPAD_OFFSET_Y] = offsetY
            preferences[Keys.PAUSE_BUTTON_SCALE] = pauseScale.coerceIn(0.7f, 1.5f)
            preferences[Keys.LEFT_HANDED_CONTROLS] = leftHanded
        }
    }

    suspend fun resetControls() {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.DPAD_SCALE] = 1.0f
            preferences[Keys.DPAD_OFFSET_X] = 0f
            preferences[Keys.DPAD_OFFSET_Y] = 0f
            preferences[Keys.PAUSE_BUTTON_SCALE] = 1.0f
            preferences[Keys.LEFT_HANDED_CONTROLS] = false
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setWallCollision(enabled: Boolean) {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.WALL_COLLISION] = enabled
        }
    }

    suspend fun setDifficulty(difficulty: GameDifficulty) {
        context.snakeDataStore.edit { preferences ->
            preferences[Keys.DIFFICULTY] = difficulty.name
        }
    }
}

// Backward-compatibility alias
typealias SnakePreferencesRepository = ProgressRepository
