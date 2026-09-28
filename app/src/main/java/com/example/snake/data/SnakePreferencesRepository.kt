package com.example.snake.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.snake.model.GameDifficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.snakeDataStore: DataStore<Preferences> by preferencesDataStore(name = "snake_preferences")

data class UserPreferences(
    val highScore: Int,
    val soundEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val wallCollision: Boolean,
    val difficulty: GameDifficulty
)

class SnakePreferencesRepository(private val context: Context) {

    private object Keys {
        val HIGH_SCORE = intPreferencesKey("high_score")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val WALL_COLLISION = booleanPreferencesKey("wall_collision")
        val DIFFICULTY = stringPreferencesKey("difficulty")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.snakeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val highScore = preferences[Keys.HIGH_SCORE] ?: 0
            val sound = preferences[Keys.SOUND_ENABLED] ?: true
            val haptics = preferences[Keys.HAPTICS_ENABLED] ?: true
            val wall = preferences[Keys.WALL_COLLISION] ?: true
            val diffName = preferences[Keys.DIFFICULTY] ?: GameDifficulty.NORMAL.name
            val difficulty = try {
                GameDifficulty.valueOf(diffName)
            } catch (_: Exception) {
                GameDifficulty.NORMAL
            }

            UserPreferences(
                highScore = highScore,
                soundEnabled = sound,
                hapticsEnabled = haptics,
                wallCollision = wall,
                difficulty = difficulty
            )
        }

    suspend fun saveHighScore(score: Int) {
        context.snakeDataStore.edit { preferences ->
            val current = preferences[Keys.HIGH_SCORE] ?: 0
            if (score > current) {
                preferences[Keys.HIGH_SCORE] = score
            }
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
