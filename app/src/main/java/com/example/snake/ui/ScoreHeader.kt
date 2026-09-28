package com.example.snake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snake.model.GameStatus
import com.example.snake.model.SnakeGameState

@Composable
fun ScoreHeader(
    state: SnakeGameState,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onToggleSound: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Title and Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "NEON SNAKE",
                    color = Color(0xFF00F5D4),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${state.difficulty.label} • ${if (state.wallCollision) "Classic Wall" else "Wrap"} • ${state.currentSpeedMs}ms",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick sound mute/unmute
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("sound_quick_toggle")
                ) {
                    Icon(
                        imageVector = if (state.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = if (state.soundEnabled) "Mute" else "Unmute",
                        tint = if (state.soundEnabled) Color(0xFF00F5D4) else Color(0xFF64748B)
                    )
                }

                // Pause / Play toggle
                IconButton(
                    onClick = onPauseToggle,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("pause_toggle_button")
                ) {
                    Icon(
                        imageVector = if (state.status == GameStatus.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.status == GameStatus.RUNNING) "Pause" else "Play",
                        tint = Color(0xFFFFD166)
                    )
                }

                // Restart button
                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = Color(0xFF38BDF8)
                    )
                }

                // Settings button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFF1F5F9)
                    )
                }
            }
        }

        // Score Badges Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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

            ScoreCard(
                label = "LENGTH",
                value = state.snake.size.toString(),
                accentColor = Color(0xFFFF2A6D),
                modifier = Modifier.weight(0.8f)
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
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111827))
            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = label,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = value,
                color = accentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
