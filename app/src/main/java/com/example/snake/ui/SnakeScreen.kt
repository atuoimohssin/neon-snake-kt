package com.example.snake.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snake.model.GameState
import com.example.snake.viewmodel.SnakeViewModel

@Composable
fun SnakeScreen(
    viewModel: SnakeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var showLevelSelector by remember { mutableStateOf(false) }
    var showFloorShop by remember { mutableStateOf(false) }

    // Auto-pause when the app goes to the background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.pauseIfPlaying()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercept back button to dismiss dialogs or pause playing game (never navigate to lobby automatically)
    BackHandler(
        enabled = showSettings || showLevelSelector || showFloorShop || state.gameState == GameState.Playing
    ) {
        if (showFloorShop) {
            showFloorShop = false
        } else if (showLevelSelector) {
            showLevelSelector = false
        } else if (showSettings) {
            showSettings = false
        } else if (state.gameState == GameState.Playing) {
            viewModel.pauseGame()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header with Level HUD progress bar, coin counter chip, and action icons
            ScoreHeader(
                state = state,
                onPauseToggle = { viewModel.togglePause() },
                onRestart = { viewModel.restartGame() },
                onToggleSound = { viewModel.toggleSound() },
                onOpenSettings = {
                    viewModel.pauseIfPlaying()
                    showSettings = true
                },
                onOpenLevelSelector = {
                    viewModel.pauseIfPlaying()
                    showLevelSelector = true
                },
                onOpenFloorShop = {
                    viewModel.pauseIfPlaying()
                    showFloorShop = true
                }
            )

            // Canvas Game Board with 3D floor perspective, cubes, spheres, and neon obstacles
            GameBoard(
                state = state,
                onTurn = { dir -> viewModel.turn(dir) },
                onResume = { viewModel.resumeGame() },
                onStartGame = { viewModel.startPlaying() },
                modifier = Modifier.fillMaxWidth()
            )

            // D-Pad Arcade Controls with unambiguous Left/Right mapping
            ArcadeControls(
                onTurn = { dir -> viewModel.turn(dir) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Floor Themes Shop Dialog
        if (showFloorShop) {
            FloorShopDialog(
                state = state,
                isFloorUnlocked = { floor -> viewModel.isFloorUnlocked(floor) },
                onSelectFloor = { floorId -> viewModel.selectFloor(floorId) },
                onBuyFloor = { floor, onResult -> viewModel.buyFloor(floor, onResult) },
                onDismiss = { showFloorShop = false }
            )
        }

        // Level Complete Dialog
        if (state.gameState == GameState.LevelComplete && state.levelCompletion != null) {
            LevelCompleteDialog(
                completion = state.levelCompletion!!,
                onNextLevel = { viewModel.advanceToNextLevel() },
                onReplay = { viewModel.restartGame() },
                onOpenLevelSelector = {
                    showLevelSelector = true
                },
                onLobby = { viewModel.goToLobby() }
            )
        }

        // Game Over Dialog with score and coins breakdown
        if (state.gameState == GameState.GameOver) {
            GameOverDialog(
                state = state,
                onRestart = { viewModel.restartGame() },
                onLobby = { viewModel.goToLobby() }
            )
        }

        // Level Selector Grid Dialog
        if (showLevelSelector) {
            LevelSelectorDialog(
                currentLevelId = state.currentLevel.id,
                unlockedLevel = state.unlockedLevel,
                levelStars = state.levelStars,
                onSelectLevel = { levelId ->
                    viewModel.loadLevel(levelId)
                    showLevelSelector = false
                },
                onDismiss = { showLevelSelector = false }
            )
        }

        // Settings Dialog
        if (showSettings) {
            SettingsDialog(
                state = state,
                onDifficultySelected = { diff -> viewModel.setDifficulty(diff) },
                onToggleWallCollision = { viewModel.toggleWallCollision() },
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                onDismiss = { showSettings = false }
            )
        }
    }
}
