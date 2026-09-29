package com.example.snake.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.snake.data.ProgressRepository
import com.example.snake.model.Cell
import com.example.snake.model.CoinPickup
import com.example.snake.model.Dir
import com.example.snake.model.FloatingScore
import com.example.snake.model.FloorCatalog
import com.example.snake.model.FloorTheme
import com.example.snake.model.Food
import com.example.snake.model.FoodType
import com.example.snake.model.GRID_SIZE
import com.example.snake.model.GameDifficulty
import com.example.snake.model.GameState
import com.example.snake.model.GameStatus
import com.example.snake.model.LevelConfig
import com.example.snake.model.LevelCompletion
import com.example.snake.model.LevelRepository
import com.example.snake.model.SnakeGameState
import com.example.snake.model.SpecialBonus
import com.example.snake.model.UnlockType
import com.example.snake.auth.AuthManager
import com.example.snake.data.FirestoreSyncManager
import com.example.snake.data.ProgressData
import com.example.snake.sound.RetroSoundManager
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class SnakeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepository = ProgressRepository(application)
    private val soundManager = RetroSoundManager(application)
    private val authManager = AuthManager(application)
    private val firestoreSyncManager = FirestoreSyncManager(application)

    private val _state = MutableStateFlow(SnakeGameState())
    val state: StateFlow<SnakeGameState> = _state.asStateFlow()

    // Exactly one game loop job
    private var gameLoopJob: Job? = null
    private var bonusCountdownJob: Job? = null
    private var coinCountdownJob: Job? = null

    // Prevent duplicate reward saves
    private var gameOverCoinsProcessed = false
    private var levelCompletionProcessed = false

    // Single-tap guard against double taps for shop purchases
    private var isPurchasingFloor = false

    private var floatingScoreIdCounter = 0L

    // Direction tracking to prevent 180° reversals
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
            authManager.userFlow.collect { user ->
                _state.update { it.copy(currentUser = user) }
                if (user != null) {
                    syncWithCloud(user)
                } else {
                    _state.update { it.copy(isCloudSynced = false, cloudHighScore = null) }
                }
            }
        }

        viewModelScope.launch {
            val userPrefs = prefsRepository.progressDataFlow.first()
            soundManager.isSoundEnabled = userPrefs.soundEnabled

            // Auto-unlock LEVEL-type floors if eligible
            FloorCatalog.ALL_FLOORS.forEach { floor ->
                if (floor.unlockType == UnlockType.LEVEL && userPrefs.unlockedLevel >= floor.requiredLevel) {
                    if (floor.id !in userPrefs.unlockedFloorIds) {
                        prefsRepository.unlockFloor(floor.id)
                    }
                }
            }

            // Start on the highest unlocked level (capped at 10)
            val initialLevelId = userPrefs.unlockedLevel.coerceIn(1, 10)
            val levelCfg = LevelRepository.getLevel(initialLevelId)
            val initialSnake = listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10))
            val initialFood = generateFood(initialSnake, obstacles = levelCfg.obstacles, excluded = emptyList())

            updateAudioSpeedForLevel(levelCfg)

            val currentFloorTheme = FloorCatalog.getFloor(userPrefs.selectedFloorId)

            _state.update {
                it.copy(
                    gameState = GameState.Ready,
                    snake = initialSnake,
                    direction = Dir.RIGHT,
                    food = initialFood,
                    currentLevel = levelCfg,
                    obstacles = levelCfg.obstacles,
                    unlockedLevel = userPrefs.unlockedLevel,
                    levelStars = userPrefs.levelStars,
                    unlockedFloorIds = userPrefs.unlockedFloorIds,
                    selectedFloorId = userPrefs.selectedFloorId,
                    selectedFloor = currentFloorTheme,
                    reduceAnimations = userPrefs.reduceAnimations,
                    dpadScale = userPrefs.dpadScale,
                    dpadOffsetX = userPrefs.dpadOffsetX,
                    dpadOffsetY = userPrefs.dpadOffsetY,
                    pauseButtonScale = userPrefs.pauseButtonScale,
                    leftHandedControls = userPrefs.leftHandedControls,
                    highScore = userPrefs.highScore,
                    totalCoins = userPrefs.totalCoins,
                    soundEnabled = userPrefs.soundEnabled,
                    hapticsEnabled = userPrefs.hapticsEnabled,
                    wallCollision = userPrefs.wallCollision,
                    difficulty = userPrefs.difficulty,
                    currentSpeedMs = levelCfg.tickMs
                )
            }

            // Keep persistent preferences in continuous sync without resetting game session state
            prefsRepository.progressDataFlow.collect { progress ->
                soundManager.isSoundEnabled = progress.soundEnabled

                // Auto-unlock LEVEL-type floors if eligible
                FloorCatalog.ALL_FLOORS.forEach { floor ->
                    if (floor.unlockType == UnlockType.LEVEL && progress.unlockedLevel >= floor.requiredLevel) {
                        if (floor.id !in progress.unlockedFloorIds) {
                            prefsRepository.unlockFloor(floor.id)
                        }
                    }
                }

                val floorTheme = FloorCatalog.getFloor(progress.selectedFloorId)

                _state.update {
                    it.copy(
                        totalCoins = progress.totalCoins,
                        highScore = progress.highScore,
                        unlockedLevel = progress.unlockedLevel,
                        levelStars = progress.levelStars,
                        unlockedFloorIds = progress.unlockedFloorIds,
                        selectedFloorId = progress.selectedFloorId,
                        selectedFloor = floorTheme,
                        reduceAnimations = progress.reduceAnimations,
                        dpadScale = progress.dpadScale,
                        dpadOffsetX = progress.dpadOffsetX,
                        dpadOffsetY = progress.dpadOffsetY,
                        pauseButtonScale = progress.pauseButtonScale,
                        leftHandedControls = progress.leftHandedControls,
                        soundEnabled = progress.soundEnabled,
                        hapticsEnabled = progress.hapticsEnabled,
                        wallCollision = progress.wallCollision,
                        difficulty = progress.difficulty
                    )
                }

                val user = _state.value.currentUser
                if (user != null && _state.value.isCloudSynced) {
                    syncProgressToCloud(user, progress)
                }
            }
        }
    }

    private fun updateAudioSpeedForLevel(levelConfig: LevelConfig) {
        val slowest = 160f
        val fastest = 70f
        val progress = ((slowest - levelConfig.tickMs.toFloat()) / (slowest - fastest)).coerceIn(0f, 1f)
        soundManager.updateSpeed(progress)
    }

    /**
     * Resets level state cleanly into GameState.Ready.
     * Guaranteed no leftover timers or loop jobs.
     */
    fun loadLevel(levelId: Int) {
        stopGameLoop()

        gameOverCoinsProcessed = false
        levelCompletionProcessed = false

        val levelCfg = LevelRepository.getLevel(levelId.coerceIn(1, 10))
        updateAudioSpeedForLevel(levelCfg)

        currentMovingDir = Dir.RIGHT
        pendingDir = Dir.RIGHT
        val initialSnake = listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10))
        val newFood = generateFood(initialSnake, obstacles = levelCfg.obstacles, excluded = emptyList())

        _state.update {
            it.copy(
                gameState = GameState.Ready,
                snake = initialSnake,
                direction = Dir.RIGHT,
                food = newFood,
                currentLevel = levelCfg,
                obstacles = levelCfg.obstacles,
                specialBonus = null,
                coinPickup = null,
                activeFloatingScores = emptyList(),
                levelCompletion = null,
                score = 0,
                foodEatenCount = 0,
                roundCoinsCollected = 0,
                currentSpeedMs = levelCfg.tickMs,
                isNewHighScore = false
            )
        }
    }

    fun startPlaying() {
        if (_state.value.gameState == GameState.Playing) return

        // Cancel any lingering job before starting to strictly guarantee exactly one loop
        stopGameLoop()

        _state.update { it.copy(gameState = GameState.Playing) }
        soundManager.setGameRunning(true)

        gameLoopJob = viewModelScope.launch {
            while (isActive && _state.value.gameState == GameState.Playing) {
                val delayMs = _state.value.currentSpeedMs
                delay(delayMs)
                if (isActive && _state.value.gameState == GameState.Playing) {
                    tick()
                }
            }
        }

        if (_state.value.specialBonus != null) {
            startBonusCountdown()
        }
        if (_state.value.coinPickup != null) {
            startCoinCountdown()
        }
    }

    fun pauseGame() {
        if (_state.value.gameState != GameState.Playing) return
        stopGameLoop()
        _state.update { it.copy(gameState = GameState.Paused) }
    }

    fun resumeGame() {
        if (_state.value.gameState != GameState.Paused) return
        startPlaying()
    }

    fun togglePause() {
        when (_state.value.gameState) {
            GameState.Playing -> pauseGame()
            GameState.Paused -> resumeGame()
            GameState.Ready -> startPlaying()
            GameState.Lobby -> {
                loadLevel(_state.value.currentLevel.id)
                startPlaying()
            }
            else -> Unit
        }
    }

    fun pauseIfPlaying() {
        if (_state.value.gameState == GameState.Playing) {
            pauseGame()
        }
    }

    fun goToLobby() {
        stopGameLoop()
        _state.update { it.copy(gameState = GameState.Lobby) }
    }

    fun restartGame() {
        loadLevel(_state.value.currentLevel.id)
        startPlaying()
    }

    fun advanceToNextLevel() {
        val nextLevelId = _state.value.currentLevel.id + 1
        if (nextLevelId <= 10) {
            loadLevel(nextLevelId)
            startPlaying()
        } else {
            loadLevel(10)
        }
    }

    /**
     * Direction inputs are ignored unless Playing or Ready (where first input starts the game).
     */
    fun turn(newDir: Dir) {
        val currentState = _state.value.gameState
        if (currentState != GameState.Playing && currentState != GameState.Ready) {
            return
        }

        if (!currentMovingDir.isOpposite(newDir) && !pendingDir.isOpposite(newDir)) {
            pendingDir = newDir
            soundManager.playTurnSound()
            if (currentState == GameState.Ready) {
                startPlaying()
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
        bonusCountdownJob?.cancel()
        bonusCountdownJob = null
        coinCountdownJob?.cancel()
        coinCountdownJob = null
        soundManager.setGameRunning(false)
    }

    fun isFloorUnlocked(floor: FloorTheme): Boolean {
        if (floor.unlockType == UnlockType.FREE) return true
        if (floor.id in _state.value.unlockedFloorIds) return true
        if (floor.unlockType == UnlockType.LEVEL && _state.value.unlockedLevel >= floor.requiredLevel) return true
        return false
    }

    fun selectFloor(floorId: String) {
        val floor = FloorCatalog.getFloor(floorId)
        if (isFloorUnlocked(floor)) {
            _state.update {
                it.copy(
                    selectedFloorId = floorId,
                    selectedFloor = floor
                )
            }
            viewModelScope.launch {
                prefsRepository.selectFloor(floorId)
            }
            soundManager.playTurnSound()
        }
    }

    fun buyFloor(floor: FloorTheme, onResult: (success: Boolean, message: String) -> Unit) {
        if (isPurchasingFloor) return
        isPurchasingFloor = true

        viewModelScope.launch {
            try {
                if (isFloorUnlocked(floor)) {
                    selectFloor(floor.id)
                    onResult(true, "Floor equipped!")
                    return@launch
                }

                if (floor.unlockType == UnlockType.COINS) {
                    val success = prefsRepository.spendCoins(floor.price)
                    if (success) {
                        prefsRepository.unlockFloor(floor.id)
                        prefsRepository.selectFloor(floor.id)
                        _state.update {
                            it.copy(
                                unlockedFloorIds = it.unlockedFloorIds + floor.id,
                                selectedFloorId = floor.id,
                                selectedFloor = floor
                            )
                        }
                        soundManager.playBonusSound()
                        triggerHapticBonus()
                        onResult(true, "Unlocked and equipped ${floor.name}!")
                    } else {
                        soundManager.playClickSound()
                        onResult(false, "Not enough coins")
                    }
                } else if (floor.unlockType == UnlockType.LEVEL) {
                    if (_state.value.unlockedLevel >= floor.requiredLevel) {
                        prefsRepository.unlockFloor(floor.id)
                        prefsRepository.selectFloor(floor.id)
                        _state.update {
                            it.copy(
                                unlockedFloorIds = it.unlockedFloorIds + floor.id,
                                selectedFloorId = floor.id,
                                selectedFloor = floor
                            )
                        }
                        soundManager.playTurnSound()
                        onResult(true, "Unlocked and equipped ${floor.name}!")
                    } else {
                        onResult(false, "Requires Level ${floor.requiredLevel}")
                    }
                }
            } finally {
                isPurchasingFloor = false
            }
        }
    }

    fun toggleReduceAnimations() {
        val current = _state.value.reduceAnimations
        val updated = !current
        _state.update { it.copy(reduceAnimations = updated) }
        viewModelScope.launch {
            prefsRepository.setReduceAnimations(updated)
        }
    }

    fun setDifficulty(difficulty: GameDifficulty) {
        viewModelScope.launch {
            prefsRepository.setDifficulty(difficulty)
        }
        _state.update { it.copy(difficulty = difficulty) }
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

    fun saveControls(
        dpadScale: Float,
        offsetX: Float,
        offsetY: Float,
        pauseScale: Float = 1.0f,
        leftHanded: Boolean = false
    ) {
        val clampedScale = dpadScale.coerceIn(0.7f, 1.5f)
        val clampedPause = pauseScale.coerceIn(0.7f, 1.5f)
        _state.update {
            it.copy(
                dpadScale = clampedScale,
                dpadOffsetX = offsetX,
                dpadOffsetY = offsetY,
                pauseButtonScale = clampedPause,
                leftHandedControls = leftHanded,
                snackMessage = "تم حفظ تعديلات الأزرار بنجاح! ✓"
            )
        }
        viewModelScope.launch {
            prefsRepository.saveControls(clampedScale, offsetX, offsetY, clampedPause, leftHanded)
        }
        triggerHapticShort()
    }

    fun resetControls() {
        _state.update {
            it.copy(
                dpadScale = 1.0f,
                dpadOffsetX = 0f,
                dpadOffsetY = 0f,
                pauseButtonScale = 1.0f,
                leftHandedControls = false,
                snackMessage = "تمت استعادة الأبعاد الافتراضية للأزرار"
            )
        }
        viewModelScope.launch {
            prefsRepository.resetControls()
        }
        triggerHapticShort()
    }

    fun setPauseButtonScale(scale: Float) {
        val clamped = scale.coerceIn(0.7f, 1.5f)
        _state.update { it.copy(pauseButtonScale = clamped) }
        viewModelScope.launch {
            prefsRepository.setPauseButtonScale(clamped)
        }
    }

    fun setLeftHandedControls(enabled: Boolean) {
        _state.update { it.copy(leftHandedControls = enabled) }
        viewModelScope.launch {
            prefsRepository.setLeftHandedControls(enabled)
        }
    }

    private fun tick() {
        val currentState = _state.value
        if (currentState.gameState != GameState.Playing) return

        currentMovingDir = pendingDir
        val snake = currentState.snake
        val head = snake.first()
        var newHeadX = head.x + currentMovingDir.dx
        var newHeadY = head.y + currentMovingDir.dy

        if (!currentState.wallCollision) {
            if (newHeadX < 0) newHeadX = GRID_SIZE - 1
            else if (newHeadX >= GRID_SIZE) newHeadX = 0
            if (newHeadY < 0) newHeadY = GRID_SIZE - 1
            else if (newHeadY >= GRID_SIZE) newHeadY = 0
        } else {
            if (newHeadX < 0 || newHeadX >= GRID_SIZE || newHeadY < 0 || newHeadY >= GRID_SIZE) {
                triggerGameOver()
                return
            }
        }

        val newHead = Cell(newHeadX, newHeadY)

        // 1. Check self-collision (excluding the tail if not growing)
        val bodyToCheck = snake.dropLast(1)
        if (newHead in bodyToCheck) {
            triggerGameOver()
            return
        }

        // 2. Check collision with obstacles
        if (newHead in currentState.obstacles) {
            triggerGameOver()
            return
        }

        val eatsFood = (newHead == currentState.food.cell)
        val eatsSpecial = (currentState.specialBonus != null && newHead == currentState.specialBonus.cell)
        val eatsCoin = (currentState.coinPickup != null && newHead == currentState.coinPickup.cell)

        val newSnake = mutableListOf<Cell>().apply {
            add(newHead)
            addAll(snake)
            if (!eatsFood && !eatsSpecial) {
                removeAt(lastIndex)
            }
        }

        var newScore = currentState.score
        var newFoodCount = currentState.foodEatenCount
        var newRoundCoins = currentState.roundCoinsCollected
        var nextFood = currentState.food
        var nextSpecial = currentState.specialBonus
        var nextCoin = currentState.coinPickup

        if (eatsFood) {
            newScore += currentState.food.type.points
            newFoodCount += 1
            soundManager.playEatSound()
            triggerHapticShort()

            spawnFloatingScore("+${currentState.food.type.points}", newHead, Color(0xFF00F5D4))

            // 15% chance to spawn gold coin pickup if none active
            if (nextCoin == null && Random.nextFloat() < 0.15f) {
                nextCoin = spawnCoinPickup(newSnake)
                startCoinCountdown()
            }

            // Spawn special bonus every 5 normal foods if none active
            if (newFoodCount % 5 == 0 && nextSpecial == null) {
                nextSpecial = spawnSpecialBonus(newSnake)
                soundManager.playBonusSpawnSound()
                startBonusCountdown()
            }

            val excluded = listOfNotNull(nextSpecial?.cell, nextCoin?.cell)
            nextFood = generateFood(newSnake, obstacles = currentState.obstacles, excluded = excluded)
        }

        if (eatsSpecial) {
            newScore += currentState.specialBonus?.points ?: 30
            soundManager.playBonusSound()
            triggerHapticBonus()
            spawnFloatingScore("+30 BONUS", newHead, Color(0xFFFFD700))
            bonusCountdownJob?.cancel()
            nextSpecial = null
        }

        if (eatsCoin) {
            newRoundCoins += 1
            soundManager.playBonusSound()
            triggerHapticBonus()
            spawnFloatingScore("+1 COIN", newHead, Color(0xFFFFD700))
            coinCountdownJob?.cancel()
            nextCoin = null

            // Add coin pickup immediately to persistent balance
            viewModelScope.launch {
                prefsRepository.addCoins(1)
            }
        }

        var isNewRecord = currentState.isNewHighScore
        var newHighScore = currentState.highScore
        if (newScore > newHighScore) {
            newHighScore = newScore
            isNewRecord = true
            viewModelScope.launch {
                prefsRepository.saveHighScore(newHighScore)
            }
        }

        val updatedSpeedMs = currentState.currentSpeedMs

        // Check if level target food is reached
        val levelTarget = currentState.currentLevel.targetFood
        if (newFoodCount >= levelTarget) {
            _state.update {
                it.copy(
                    snake = newSnake,
                    direction = currentMovingDir,
                    food = nextFood,
                    specialBonus = nextSpecial,
                    coinPickup = nextCoin,
                    score = newScore,
                    highScore = newHighScore,
                    foodEatenCount = newFoodCount,
                    roundCoinsCollected = newRoundCoins,
                    currentSpeedMs = updatedSpeedMs,
                    isNewHighScore = isNewRecord
                )
            }
            triggerLevelComplete(newScore, newFoodCount)
            return
        }

        _state.update {
            it.copy(
                snake = newSnake,
                direction = currentMovingDir,
                food = nextFood,
                specialBonus = nextSpecial,
                coinPickup = nextCoin,
                score = newScore,
                highScore = newHighScore,
                foodEatenCount = newFoodCount,
                roundCoinsCollected = newRoundCoins,
                currentSpeedMs = updatedSpeedMs,
                isNewHighScore = isNewRecord
            )
        }
    }

    private fun triggerLevelComplete(finalScore: Int, foodsEaten: Int) {
        if (levelCompletionProcessed) return
        levelCompletionProcessed = true

        stopGameLoop()

        val currentLvl = _state.value.currentLevel
        val isLast = currentLvl.id >= 10

        val target = currentLvl.targetFood
        val stars = when {
            finalScore >= (target * 10 + 30) -> 3
            finalScore >= (target * 10 + 10) -> 2
            else -> 1
        }

        val reward = currentLvl.coinReward

        if (isLast) {
            soundManager.playVictorySound()
        } else {
            soundManager.playLevelCompleteSound()
        }
        triggerHapticBonus()

        val nextLevelToUnlock = (currentLvl.id + 1).coerceAtMost(10)

        // Save progress once only
        viewModelScope.launch {
            prefsRepository.addCoins(reward)
            prefsRepository.setUnlockedLevel(nextLevelToUnlock)
            prefsRepository.saveLevelStars(currentLvl.id, stars)
        }

        val completion = LevelCompletion(
            levelId = currentLvl.id,
            stars = stars,
            coinReward = reward,
            score = finalScore,
            isLastLevel = isLast
        )

        _state.update {
            it.copy(
                gameState = GameState.LevelComplete,
                levelCompletion = completion
            )
        }
    }

    private fun triggerGameOver() {
        stopGameLoop()

        soundManager.playGameOverSound()
        triggerHapticCrash()

        // Guard against duplicate coin saving at game over
        if (!gameOverCoinsProcessed) {
            gameOverCoinsProcessed = true
            val coinsEarned = (_state.value.score / 20) + _state.value.roundCoinsCollected
            if (coinsEarned > 0) {
                viewModelScope.launch {
                    prefsRepository.addCoins(coinsEarned)
                }
            }
        }

        _state.update {
            it.copy(gameState = GameState.GameOver)
        }
    }

    private fun spawnFloatingScore(text: String, cell: Cell, color: Color) {
        val id = ++floatingScoreIdCounter
        val item = FloatingScore(id, text, cell, color)
        _state.update {
            it.copy(activeFloatingScores = it.activeFloatingScores + item)
        }
        viewModelScope.launch {
            delay(1000)
            _state.update { state ->
                state.copy(activeFloatingScores = state.activeFloatingScores.filter { it.id != id })
            }
        }
    }

    private fun spawnSpecialBonus(occupiedCells: List<Cell>): SpecialBonus {
        val freeCells = getFreeCells(occupiedCells, _state.value.obstacles)
        val targetCell = if (freeCells.isNotEmpty()) freeCells.random() else Cell(0, 0)
        return SpecialBonus(cell = targetCell, points = 30, remainingMillis = 5000L, totalMillis = 5000L)
    }

    private fun spawnCoinPickup(occupiedCells: List<Cell>): CoinPickup {
        val freeCells = getFreeCells(occupiedCells, _state.value.obstacles)
        val targetCell = if (freeCells.isNotEmpty()) freeCells.random() else Cell(0, 0)
        return CoinPickup(cell = targetCell, remainingMillis = 6000L, totalMillis = 6000L)
    }

    private fun getFreeCells(occupiedCells: List<Cell>, obstacles: List<Cell>): List<Cell> {
        val free = ArrayList<Cell>(GRID_SIZE * GRID_SIZE)
        val occupiedSet = (occupiedCells + obstacles).toHashSet()
        for (x in 0 until GRID_SIZE) {
            for (y in 0 until GRID_SIZE) {
                val c = Cell(x, y)
                if (c !in occupiedSet) {
                    free.add(c)
                }
            }
        }
        return free
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

    private fun startCoinCountdown() {
        coinCountdownJob?.cancel()
        coinCountdownJob = viewModelScope.launch {
            val interval = 100L
            while (_state.value.coinPickup != null) {
                delay(interval)
                val currentCoin = _state.value.coinPickup ?: break
                val remaining = currentCoin.remainingMillis - interval
                if (remaining <= 0) {
                    _state.update { it.copy(coinPickup = null) }
                    break
                } else {
                    _state.update {
                        it.copy(coinPickup = currentCoin.copy(remainingMillis = remaining))
                    }
                }
            }
        }
    }

    private fun generateFood(occupiedCells: List<Cell>, obstacles: List<Cell>, excluded: List<Cell>): Food {
        val free = getFreeCells(occupiedCells + excluded, obstacles)
        if (free.isEmpty()) {
            return Food(Cell(0, 0), FoodType.REGULAR)
        }
        return Food(free.random(), FoodType.REGULAR)
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

    fun signInWithGoogle(context: Context? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isAuthLoading = true, authErrorMessage = null) }
            val result = authManager.signInWithGoogle(context)
            result.onSuccess { user ->
                _state.update {
                    it.copy(
                        currentUser = user,
                        isAuthLoading = false,
                        authErrorMessage = null,
                        snackMessage = "مرحباً ${user.displayName ?: "يا بطل"}! تم تسجيل الدخول بنجاح ✓"
                    )
                }
                syncWithCloud(user)
            }.onFailure { exception ->
                val msg = exception.localizedMessage ?: "فشل تسجيل الدخول"
                val isCancelled = msg.contains("إلغاء")
                _state.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = if (!isCancelled) msg else null,
                        snackMessage = if (isCancelled) "تم إلغاء عملية تسجيل الدخول" else null
                    )
                }
            }
        }
    }

    fun signInQuickCloud(playerName: String = "Neon Runner") {
        viewModelScope.launch {
            _state.update { it.copy(isAuthLoading = true, authErrorMessage = null) }
            val result = authManager.signInQuickCloud(playerName)
            result.onSuccess { user ->
                _state.update {
                    it.copy(
                        currentUser = user,
                        isAuthLoading = false,
                        authErrorMessage = null,
                        snackMessage = "تم إنشاء وتفعيل الحساب السحابي بنجاح! ☁✓"
                    )
                }
                syncWithCloud(user)
            }.onFailure { exception ->
                _state.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = exception.localizedMessage ?: "فشل تسجيل الدخول السحابي"
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _state.update { it.copy(isAuthLoading = true) }
            authManager.signOut()
            _state.update {
                it.copy(
                    currentUser = null,
                    isAuthLoading = false,
                    snackMessage = "Signed out"
                )
            }
        }
    }

    fun clearAuthError() {
        _state.update { it.copy(authErrorMessage = null) }
    }

    fun syncWithCloud(user: FirebaseUser? = _state.value.currentUser) {
        if (user == null) return
        viewModelScope.launch {
            _state.update { it.copy(isCloudSyncing = true) }
            val local = prefsRepository.progressDataFlow.first()
            val cloudResult = firestoreSyncManager.fetchPlayerData(user.uid)
            cloudResult.onSuccess { cloudData ->
                if (cloudData != null) {
                    val merged = firestoreSyncManager.mergeProgress(local, cloudData)
                    prefsRepository.applyMergedProgress(merged)
                    firestoreSyncManager.savePlayerData(user, merged)
                    _state.update {
                        it.copy(
                            isCloudSyncing = false,
                            isCloudSynced = true,
                            cloudHighScore = merged.highScore,
                            highScore = merged.highScore,
                            totalCoins = merged.totalCoins,
                            unlockedLevel = merged.unlockedLevel,
                            levelStars = merged.levelStars,
                            unlockedFloorIds = merged.unlockedFloorIds,
                            selectedFloorId = merged.selectedFloorId,
                            snackMessage = "تمت مزامنة نقاطك وتقدمك سحابياً بنجاح! ☁✓"
                        )
                    }
                } else {
                    firestoreSyncManager.savePlayerData(user, local)
                    _state.update {
                        it.copy(
                            isCloudSyncing = false,
                            isCloudSynced = true,
                            cloudHighScore = local.highScore,
                            snackMessage = "تم حفظ نقاطك في السحابة بنجاح! ☁✓"
                        )
                    }
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isCloudSyncing = false,
                        snackMessage = "تعذر الاتصال بالسحابة: ${e.localizedMessage ?: "خطأ في الشبكة"}"
                    )
                }
            }
        }
    }

    private fun syncProgressToCloud(user: FirebaseUser, progress: ProgressData) {
        viewModelScope.launch {
            val result = firestoreSyncManager.savePlayerData(user, progress)
            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        isCloudSynced = true,
                        cloudHighScore = maxOf(it.cloudHighScore ?: 0, progress.highScore)
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
        soundManager.release()
    }
}
