package com.example.snake.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snake.model.GameState
import com.example.snake.model.GameStatus
import com.example.snake.model.SnakeGameState

@Composable
fun ScoreHeader(
    state: SnakeGameState,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLevelSelector: () -> Unit,
    onOpenFloorShop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Brand Title & Action Icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "NEON SNAKE",
                    color = Color(0xFF00F5D4),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
                Text(
                    text = "L${state.currentLevel.id} • ${state.currentLevel.tickMs}ms",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Play toggle button (prominently placed first so it is never pushed off screen)
                val isPlaying = state.gameState == GameState.Playing
                val isPaused = state.gameState == GameState.Paused
                IconButton(
                    onClick = onPauseToggle,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isPlaying -> Color(0x33FFD166)
                                isPaused -> Color(0x3300F5D4)
                                else -> Color(0x2238BDF8)
                            }
                        )
                        .border(
                            1.5.dp,
                            when {
                                isPlaying -> Color(0xFFFFD166)
                                isPaused -> Color(0xFF00F5D4)
                                else -> Color(0xFF38BDF8)
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .testTag("pause_button")
                        .testTag("pause_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = when {
                            isPlaying -> Color(0xFFFFD166)
                            isPaused -> Color(0xFF00F5D4)
                            else -> Color(0xFF38BDF8)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Floor shop shortcut button
                IconButton(
                    onClick = onOpenFloorShop,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("open_floor_shop_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Floor Shop",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Level selector shortcut button
                IconButton(
                    onClick = onOpenLevelSelector,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("open_level_selector_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Levels",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quick sound mute/unmute
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("sound_quick_toggle")
                ) {
                    Icon(
                        imageVector = if (state.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = if (state.soundEnabled) "Mute" else "Unmute",
                        tint = if (state.soundEnabled) Color(0xFF00F5D4) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Restart button
                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Settings button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFF1F5F9),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Row 2: Level HUD Progress Bar (clickable to open level selector)
        val progress = (state.foodEatenCount.toFloat() / state.currentLevel.targetFood.toFloat()).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E293B))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                .clickable { onOpenLevelSelector() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LEVEL ${state.currentLevel.id}",
                            color = Color(0xFF00F5D4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        if (state.obstacles.isNotEmpty()) {
                            Text(
                                text = "• ${state.obstacles.size} Obstacles",
                                color = Color(0xFFF43F5E),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Goal: ${state.foodEatenCount}/${state.currentLevel.targetFood} Food",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF00F5D4),
                    trackColor = Color(0xFF0F172A),
                    strokeCap = StrokeCap.Round
                )
            }
        }

        // Row 3: Score Badges (SCORE, HIGH SCORE, COINS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScoreCard(
                label = "SCORE",
                value = state.score.toString(),
                accentColor = Color(0xFF00F5D4),
                modifier = Modifier.weight(1f)
            )

            ScoreCard(
                label = "HIGH SCORE",
                value = state.highScore.toString(),
                accentColor = Color(0xFFFFD166),
                icon = Icons.Default.EmojiEvents,
                modifier = Modifier.weight(1f)
            )

            // Coin counter chip animating when it changes
            CoinScoreCard(
                totalCoins = state.totalCoins,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ScoreCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF111827))
            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = label,
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = value,
                color = accentColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun CoinScoreCard(
    totalCoins: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF111827))
            .border(1.dp, Color(0x66FFD700), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "COINS",
                    color = Color(0xFFFFD700),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            AnimatedContent(
                targetState = totalCoins,
                transitionSpec = {
                    slideInVertically { height -> height } togetherWith
                            slideOutVertically { height -> -height }
                },
                label = "coins_counter"
            ) { coins ->
                Text(
                    text = coins.toString(),
                    color = Color(0xFFFFD700),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
