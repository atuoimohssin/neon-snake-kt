package com.example.snake.model

import androidx.compose.ui.graphics.Color

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

enum class GameStatus {
    IDLE,
    RUNNING,
    PAUSED,
    GAME_OVER
}

data class SnakeGameState(
    val snake: List<Cell> = listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10)),
    val direction: Dir = Dir.RIGHT,
    val food: Food = Food(Cell(15, 10)),
    val specialBonus: SpecialBonus? = null,
    val score: Int = 0,
    val highScore: Int = 0,
    val foodEatenCount: Int = 0,
    val currentSpeedMs: Long = 150L,
    val status: GameStatus = GameStatus.IDLE,
    val difficulty: GameDifficulty = GameDifficulty.NORMAL,
    val wallCollision: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val isNewHighScore: Boolean = false
)
