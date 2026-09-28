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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.snake.model.GameStatus
import com.example.snake.viewmodel.SnakeViewModel

@Composable
fun SnakeScreen(
    viewModel: SnakeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    // Intercept back button to pause game or dismiss settings
    BackHandler(enabled = state.status == GameStatus.RUNNING || showSettings) {
        if (showSettings) {
            showSettings = false
        } else if (state.status == GameStatus.RUNNING) {
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
            // Header with title, stats, sound quick toggle, and controls
            ScoreHeader(
                state = state,
                onPauseToggle = {
                    if (state.status == GameStatus.RUNNING) {
                        viewModel.pauseGame()
                    } else if (state.status == GameStatus.PAUSED) {
                        viewModel.resumeGame()
                    } else if (state.status == GameStatus.IDLE) {
                        viewModel.startGame()
                    }
                },
                onRestart = { viewModel.restartGame() },
                onToggleSound = { viewModel.toggleSound() },
                onOpenSettings = { showSettings = true }
            )

            // Canvas Game Board with neon aesthetics & pulse animation
            GameBoard(
                state = state,
                onTurn = { dir -> viewModel.turn(dir) },
                modifier = Modifier.fillMaxWidth()
            )

            // D-Pad Arcade Controls with unambiguous Left/Right mapping
            ArcadeControls(
                onTurn = { dir -> viewModel.turn(dir) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Game Over Dialog with smooth entrance and score details
        if (state.status == GameStatus.GAME_OVER) {
            GameOverDialog(
                state = state,
                onRestart = { viewModel.restartGame() }
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
