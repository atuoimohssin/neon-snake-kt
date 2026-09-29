package com.example.snake.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseUser

const val GRID_SIZE = 20

enum class Dir(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun isOpposite(other: Dir): Boolean {
        return (dx + other.dx == 0) && (dy + other.dy == 0)
    }
}

data class Cell(val x: Int, val y: Int)

enum class GameDifficulty(val label: String, val baseTickDelayMs: Long, val minTickDelayMs: Long) {
    SLOW("Slow", 210L, 110L),
    NORMAL("Normal", 150L, 70L),
    FAST("Fast", 100L, 50L),
    TURBO("Turbo", 70L, 40L)
}

enum class FoodType(val points: Int, val color: Color) {
    REGULAR(10, Color(0xFFFF3366)),
    SPECIAL_STAR(30, Color(0xFFFFD700))
}

data class Food(
    val cell: Cell,
    val type: FoodType = FoodType.REGULAR
)

data class SpecialBonus(
    val cell: Cell,
    val points: Int = 30,
    val remainingMillis: Long = 5000L,
    val totalMillis: Long = 5000L
)

data class CoinPickup(
    val cell: Cell,
    val remainingMillis: Long = 6000L,
    val totalMillis: Long = 6000L
)

data class FloatingScore(
    val id: Long,
    val text: String,
    val cell: Cell,
    val color: Color
)

sealed interface GameState {
    data object Lobby : GameState
    data object Ready : GameState
    data object Playing : GameState
    data object Paused : GameState
    data object LevelComplete : GameState
    data object GameOver : GameState
}

enum class GameStatus {
    IDLE,
    RUNNING,
    PAUSED,
    GAME_OVER,
    LEVEL_COMPLETE
}

data class LevelCompletion(
    val levelId: Int,
    val stars: Int,
    val coinReward: Int,
    val score: Int,
    val isLastLevel: Boolean
)

data class SnakeGameState(
    val gameState: GameState = GameState.Ready,
    val snake: List<Cell> = listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10)),
    val direction: Dir = Dir.RIGHT,
    val food: Food = Food(Cell(15, 10)),
    val specialBonus: SpecialBonus? = null,
    val coinPickup: CoinPickup? = null,
    val activeFloatingScores: List<FloatingScore> = emptyList(),
    val currentLevel: LevelConfig = LevelRepository.getLevel(1),
    val obstacles: List<Cell> = emptyList(),
    val unlockedLevel: Int = 1,
    val levelStars: Map<Int, Int> = emptyMap(),
    val levelCompletion: LevelCompletion? = null,
    val unlockedFloorIds: Set<String> = setOf("classic"),
    val selectedFloorId: String = "classic",
    val selectedFloor: FloorTheme = FloorCatalog.CLASSIC,
    val reduceAnimations: Boolean = false,
    val score: Int = 0,
    val highScore: Int = 0,
    val foodEatenCount: Int = 0,
    val roundCoinsCollected: Int = 0,
    val totalCoins: Int = 0,
    val currentSpeedMs: Long = 160L,
    val dpadScale: Float = 1.0f,
    val dpadOffsetX: Float = 0f,
    val dpadOffsetY: Float = 0f,
    val pauseButtonScale: Float = 1.0f,
    val leftHandedControls: Boolean = false,
    val difficulty: GameDifficulty = GameDifficulty.NORMAL,
    val wallCollision: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val isNewHighScore: Boolean = false,
    val snackMessage: String? = null,
    val currentUser: FirebaseUser? = null,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val isCloudSyncing: Boolean = false,
    val isCloudSynced: Boolean = false,
    val cloudHighScore: Int? = null
) {
    val status: GameStatus
        get() = when (gameState) {
            GameState.Lobby, GameState.Ready -> GameStatus.IDLE
            GameState.Playing -> GameStatus.RUNNING
            GameState.Paused -> GameStatus.PAUSED
            GameState.LevelComplete -> GameStatus.LEVEL_COMPLETE
            GameState.GameOver -> GameStatus.GAME_OVER
        }

    val coinsEarnedThisRound: Int
        get() = (score / 20) + roundCoinsCollected

    val floatingScores: List<FloatingScore>
        get() = activeFloatingScores
}
