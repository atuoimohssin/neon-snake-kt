package com.example.snake.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.window.Dialog
import com.example.snake.model.LevelConfig
import com.example.snake.model.LevelRepository

@Composable
fun LevelSelectorDialog(
    currentLevelId: Int,
    unlockedLevel: Int,
    levelStars: Map<Int, Int>,
    onSelectLevel: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
                .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT LEVEL",
                        color = Color(0xFFF8FAFC),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_level_selector")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                // Grid of 10 Levels
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(LevelRepository.levels) { level ->
                        LevelCard(
                            level = level,
                            isCurrent = level.id == currentLevelId,
                            isUnlocked = level.id <= unlockedLevel,
                            stars = levelStars[level.id] ?: 0,
                            onClick = {
                                if (level.id <= unlockedLevel) {
                                    onSelectLevel(level.id)
                                    onDismiss()
                                }
                            }
                        )
                    }
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("done_level_selector"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text(
                        text = "CLOSE",
                        color = Color(0xFF0B132B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(
    level: LevelConfig,
    isCurrent: Boolean,
    isUnlocked: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCurrent -> Color(0xFF00F5D4)
        isUnlocked -> Color(0xFF334155)
        else -> Color(0xFF1E293B)
    }

    val bgColor = when {
        isCurrent -> Color(0x3300F5D4)
        isUnlocked -> Color(0xFF1E293B)
        else -> Color(0xFF0B1120)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = isUnlocked, onClick = onClick)
            .padding(12.dp)
            .testTag("level_card_${level.id}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LVL ${level.id}",
                    color = if (isUnlocked) Color(0xFFF1F5F9) else Color(0xFF64748B),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                if (!isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                } else if (isCurrent) {
                    Text(
                        text = "PLAYING",
                        color = Color(0xFF00F5D4),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Text(
                text = "${level.targetFood} food • ${level.tickMs}ms",
                color = if (isUnlocked) Color(0xFF94A3B8) else Color(0xFF475569),
                fontSize = 11.sp
            )

            // Star Rating (1..3)
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                for (s in 1..3) {
                    val filled = isUnlocked && s <= stars
                    Icon(
                        imageVector = if (filled) Icons.Default.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = if (filled) Color(0xFFFFD700) else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
