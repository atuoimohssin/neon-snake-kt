package com.example.snake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.snake.model.Dir
import kotlin.math.roundToInt

@Composable
fun ArcadeControls(
    onTurn: (Dir) -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    leftHanded: Boolean = false,
    enabled: Boolean = true
) {
    // Force Left-to-Right layout direction so Left/Right physical buttons and arrows
    // remain consistent regardless of device language (LTR/RTL)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val clampedScale = scale.coerceIn(0.7f, 1.5f)
        val effectiveOffsetX = if (leftHanded) -offsetX else offsetX

        Box(
            modifier = modifier
                .offset {
                    IntOffset(
                        x = (effectiveOffsetX * density).roundToInt(),
                        y = (offsetY * density).roundToInt()
                    )
                }
                .graphicsLayer {
                    scaleX = clampedScale
                    scaleY = clampedScale
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // UP Button
                ArcadeDirectionButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Up",
                    testTag = "dpad_up",
                    enabled = enabled,
                    onClick = { onTurn(Dir.UP) }
                )

                // Middle Row: LEFT, CENTER PIVOT, RIGHT
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArcadeDirectionButton(
                        icon = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Left",
                        testTag = "dpad_left",
                        enabled = enabled,
                        onClick = { onTurn(Dir.LEFT) }
                    )

                    // Decorative Center Pivot
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C2541))
                            .border(1.dp, Color(0xFF3A86FF), CircleShape)
                    )

                    ArcadeDirectionButton(
                        icon = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Right",
                        testTag = "dpad_right",
                        enabled = enabled,
                        onClick = { onTurn(Dir.RIGHT) }
                    )
                }

                // DOWN Button
                ArcadeDirectionButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Down",
                    testTag = "dpad_down",
                    enabled = enabled,
                    onClick = { onTurn(Dir.DOWN) }
                )
            }
        }
    }
}

@Composable
private fun ArcadeDirectionButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(56.dp)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .testTag(testTag)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(32.dp)
        )
    }
}
