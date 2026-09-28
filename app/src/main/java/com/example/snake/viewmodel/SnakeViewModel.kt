package com.example.snake.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.snake.data.SnakePreferencesRepository
import com.example.snake.model.Cell
import com.example.snake.model.Dir
import com.example.snake.model.Food
import com.example.snake.model.FoodType
import com.example.snake.model.GRID_SIZE
import com.example.snake.model.GameDifficulty
import com.example.snake.model.GameStatus
import com.example.snake.model.SnakeGameState
import com.example.snake.model.SpecialBonus
import com.example.snake.sound.RetroSoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class SnakeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepository = SnakePreferencesRepository(application)
    private val soundManager = RetroSoundManager(application)

    private val _state = MutableStateFlow(SnakeGameState())
    val state: StateFlow<SnakeGameState> = _state.asStateFlow()

    private var gameLoopJob: Job? = null
    private var bonusCountdownJob: Job? = null

    // Direction tracking to prevent 180° reversals and input lag
    private var currentMovingDir: Dir = Dir.RIGHT
    private var pendingDir: Dir = Dir.RIGHT

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        viewModelScope.launch {
            val userPrefs = prefsRepository.userPreferencesFlow.first()
            soundManager.isSoundEnabled = userPrefs.soundEnabled
            _state.update {
                it.copy(
                    highScore = userPrefs.highScore,
                    soundEnabled = userPrefs.soundEnabled,
                    hapticsEnabled = userPrefs.hapticsEnabled,
                    wallCollision = userPrefs.wallCollision,
                    difficulty = userPrefs.difficulty,
                    currentSpeedMs = calculateTickDelay(userPrefs.difficulty, 0)
                )
            }
        }
    }

    fun startGame() {
        if (_state.value.status == GameStatus.RUNNING) return
        _state.update { it.copy(status = GameStatus.RUNNING) }
        startGameLoop()
    }

    fun pauseGame() {
        if (_state.value.status != GameStatus.RUNNING) return
        _state.update { it.copy(status = GameStatus.PAUSED) }
        gameLoopJob?.cancel()
        bonusCountdownJob?.cancel()
    }

    fun resumeGame() {
        if (_state.value.status != GameStatus.PAUSED) return
        _state.update { it.copy(status = GameStatus.RUNNING) }
        startGameLoop()
        resumeBonusTimerIfNeeded()
    }

    fun restartGame() {
        gameLoopJob?.cancel()
        bonusCountdownJob?.cancel()

        currentMovingDir = Dir.RIGHT
        pendingDir = Dir.RIGHT
        val initialSnake = listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10))
        val newFood = generateFood(initialSnake, excluded = emptyList())
        val baseSpeed = calculateTickDelay(_state.value.difficulty, 0)

        _state.update {
            it.copy(
                snake = initialSnake,
                direction = Dir.RIGHT,
                food = newFood,
                specialBonus = null,
                score = 0,
                foodEatenCount = 0,
                currentSpeedMs = baseSpeed,
                status = GameStatus.RUNNING,
                isNewHighScore = false
            )
        }
        soundManager.playClickSound()
        startGameLoop()
    }

    fun turn(newDir: Dir) {
        // Prevent 180° reversal against both current moving direction and pending direction
        if (!currentMovingDir.isOpposite(newDir) && !pendingDir.isOpposite(newDir)) {
            if (pendingDir != newDir) {
                soundManager.playTurnSound()
            }
            pendingDir = newDir
            if (_state.value.status == GameStatus.IDLE) {
                startGame()
            }
        }
    }

    fun setDifficulty(difficulty: GameDifficulty) {
        viewModelScope.launch {
            prefsRepository.setDifficulty(difficulty)
        }
        val newDelay = calculateTickDelay(difficulty, _state.value.foodEatenCount)
        _state.update { it.copy(difficulty = difficulty, currentSpeedMs = newDelay) }
        if (_state.value.status == GameStatus.RUNNING) {
            startGameLoop()
        }
    }

    fun toggleWallCollision() {
        val updated = !_state.value.wallCollision
        _state.update { it.copy(wallCollision = updated) }
        viewModelScope.launch {
            prefsRepository.setWallCollision(updated)
        }
    }

    fun toggleSound() {
        val updated = !_state.value.soundEnabled
        soundManager.isSoundEnabled = updated
        _state.update { it.copy(soundEnabled = updated) }
        viewModelScope.launch {
            prefsRepository.setSoundEnabled(updated)
        }
        if (updated) soundManager.playClickSound()
    }

    fun toggleHaptics() {
        val updated = !_state.value.hapticsEnabled
        _state.update { it.copy(hapticsEnabled = updated) }
        viewModelScope.launch {
            prefsRepository.setHapticsEnabled(updated)
        }
        if (updated) triggerHapticShort()
    }

    private fun calculateTickDelay(difficulty: GameDifficulty, foodCount: Int): Long {
        val speedSteps = (foodCount / 5) * 8L
        return maxOf(difficulty.minTickDelayMs, difficulty.baseTickDelayMs - speedSteps)
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (_state.value.status == GameStatus.RUNNING) {
                val delayMs = _state.value.currentSpeedMs
                delay(delayMs)
                tick()
            }
        }
    }

    private fun tick() {
        val current = _state.value
        if (current.status != GameStatus.RUNNING) return

        currentMovingDir = pendingDir
        val head = current.snake.first()
        var nextX = head.x + currentMovingDir.dx
        var nextY = head.y + currentMovingDir.dy

        // Wall handling
        if (current.wallCollision) {
            if (nextX !in 0 until GRID_SIZE || nextY !in 0 until GRID_SIZE) {
                triggerGameOver()
                return
            }
        } else {
            nextX = (nextX + GRID_SIZE) % GRID_SIZE
            nextY = (nextY + GRID_SIZE) % GRID_SIZE
        }

        val nextCell = Cell(nextX, nextY)

        // Self-collision (check against snake body except the very tail if it moves)
        if (nextCell in current.snake.dropLast(1)) {
            triggerGameOver()
            return
        }

        val ateRegularFood = (nextCell == current.food.cell)
        val ateSpecialFood = (current.specialBonus != null && nextCell == current.specialBonus.cell)

        val willGrow = ateRegularFood || ateSpecialFood
        val newSnake = if (willGrow) {
            listOf(nextCell) + current.snake
        } else {
            listOf(nextCell) + current.snake.dropLast(1)
        }

        var newScore = current.score
        var newFoodCount = current.foodEatenCount
        var nextFood = current.food
        var nextSpecial = current.specialBonus

        if (ateRegularFood) {
            newScore += current.food.type.points
            newFoodCount++
            soundManager.playEatSound()
            triggerHapticShort()

            // Generate new regular food
            val occupied = newSnake + (nextSpecial?.cell?.let { listOf(it) } ?: emptyList())
            nextFood = generateFood(occupied, excluded = emptyList())

            // Chance to spawn special bonus food (e.g. every 4 foods or 30% chance if none active)
            if (nextSpecial == null && (newFoodCount % 4 == 0 || Random.nextInt(4) == 0)) {
                nextSpecial = spawnSpecialBonus(newSnake + listOf(nextFood.cell))
                soundManager.playBonusSpawnSound()
                startBonusCountdown()
            }
        }

        if (ateSpecialFood && current.specialBonus != null) {
            newScore += current.specialBonus.points
            soundManager.playSpecialFoodSound()
            triggerHapticBonus()
            bonusCountdownJob?.cancel()
            nextSpecial = null
        }

        // Calculate gradual speed based on food eaten
        val updatedSpeedMs = calculateTickDelay(current.difficulty, newFoodCount)

        // Check high score
        var newHighScore = current.highScore
        var isNewRecord = current.isNewHighScore
        if (newScore > newHighScore) {
            newHighScore = newScore
            isNewRecord = true
            viewModelScope.launch {
                prefsRepository.saveHighScore(newHighScore)
            }
        }

        _state.update {
            it.copy(
                snake = newSnake,
                direction = currentMovingDir,
                food = nextFood,
                specialBonus = nextSpecial,
                score = newScore,
                highScore = newHighScore,
                foodEatenCount = newFoodCount,
                currentSpeedMs = updatedSpeedMs,
                isNewHighScore = isNewRecord
            )
        }
    }

    private fun spawnSpecialBonus(occupiedCells: List<Cell>): SpecialBonus {
        val freeCells = mutableListOf<Cell>()
        for (x in 0 until GRID_SIZE) {
            for (y in 0 until GRID_SIZE) {
                val c = Cell(x, y)
                if (c !in occupiedCells) {
                    freeCells.add(c)
                }
            }
        }
        val targetCell = if (freeCells.isNotEmpty()) freeCells.random() else Cell(0, 0)
        return SpecialBonus(cell = targetCell, points = 30, remainingMillis = 5000L, totalMillis = 5000L)
    }

    private fun startBonusCountdown() {
        bonusCountdownJob?.cancel()
        bonusCountdownJob = viewModelScope.launch {
            val interval = 100L
            while (_state.value.specialBonus != null) {
                delay(interval)
                val currentBonus = _state.value.specialBonus ?: break
                val remaining = currentBonus.remainingMillis - interval
                if (remaining <= 0) {
                    _state.update { it.copy(specialBonus = null) }
                    break
                } else {
                    _state.update {
                        it.copy(specialBonus = currentBonus.copy(remainingMillis = remaining))
                    }
                }
            }
        }
    }

    private fun resumeBonusTimerIfNeeded() {
        if (_state.value.specialBonus != null) {
            startBonusCountdown()
        }
    }

    private fun triggerGameOver() {
        gameLoopJob?.cancel()
        bonusCountdownJob?.cancel()
        soundManager.playGameOverSound()
        triggerHapticCrash()
        _state.update { it.copy(status = GameStatus.GAME_OVER) }
    }

    private fun generateFood(occupiedCells: List<Cell>, excluded: List<Cell>): Food {
        val availableCells = mutableListOf<Cell>()
        for (x in 0 until GRID_SIZE) {
            for (y in 0 until GRID_SIZE) {
                val cell = Cell(x, y)
                if (cell !in occupiedCells && cell !in excluded) {
                    availableCells.add(cell)
                }
            }
        }

        if (availableCells.isEmpty()) {
            return Food(Cell(0, 0), FoodType.REGULAR)
        }

        return Food(availableCells.random(), FoodType.REGULAR)
    }

    private fun triggerHapticShort() {
        if (!_state.value.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35L, 160))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35L)
            }
        } catch (_: Exception) {}
    }

    private fun triggerHapticBonus() {
        if (!_state.value.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 40, 70), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70L)
            }
        } catch (_: Exception) {}
    }

    private fun triggerHapticCrash() {
        if (!_state.value.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 90, 50, 140), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(160L)
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
        bonusCountdownJob?.cancel()
        soundManager.release()
    }
}
