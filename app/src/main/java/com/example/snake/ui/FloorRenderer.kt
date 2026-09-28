package com.example.snake.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.snake.model.FloorAnimationType
import com.example.snake.model.FloorTheme
import com.example.snake.model.GRID_SIZE
import kotlin.math.PI
import kotlin.math.sin

/**
 * Shared Floor Renderer used for both the main GameBoard and the Shop previews.
 * Maintains solid 60 FPS with minimal or pre-computed state in the draw scope.
 */
object FloorRenderer {

    fun drawFloor(
        drawScope: DrawScope,
        floor: FloorTheme,
        animTime: Float, // 0f..1f (continuous normalized progress)
        gridCount: Int = GRID_SIZE,
        reduceAnimations: Boolean = false,
        forceStatic: Boolean = false
    ) {
        with(drawScope) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return

            val shouldAnimate = floor.animated && !reduceAnimations && !forceStatic

            // 1. Draw Background Gradient
            if (shouldAnimate && floor.animationType == FloorAnimationType.AURORA) {
                // Moving aurora gradient
                val angle = animTime * 2f * PI.toFloat()
                val startX = width * (0.5f + 0.35f * sin(angle))
                val endX = width * (0.5f - 0.35f * sin(angle))
                val auroraBrush = Brush.linearGradient(
                    colors = floor.bgColors,
                    start = Offset(startX, 0f),
                    end = Offset(endX, height)
                )
                drawRect(brush = auroraBrush, size = size)
            } else {
                drawRect(brush = floor.brush, size = size)
            }

            // 2. Draw Grid Lines
            val cellSize = width / gridCount
            val baseGridColor = floor.gridLineColor

            when {
                shouldAnimate && floor.animationType == FloorAnimationType.FLOW -> {
                    // Vertical lines static
                    for (i in 1 until gridCount) {
                        val x = i * cellSize
                        drawLine(
                            color = baseGridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f
                        )
                    }
                    // Horizontal lines flowing downwards
                    val flowOffset = (animTime * cellSize) % cellSize
                    val numHoriz = (height / cellSize).toInt() + 1
                    for (j in 0..numHoriz) {
                        val y = j * cellSize + flowOffset
                        if (y in 0f..height) {
                            drawLine(
                                color = baseGridColor,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }
                    }
                }

                shouldAnimate && floor.animationType == FloorAnimationType.PULSE -> {
                    // Breathing rhythm: opacity scales between 0.35x and 1.6x
                    val pulseFactor = 0.65f + 0.35f * sin(animTime * 2f * PI.toFloat())
                    val currentAlpha = (baseGridColor.alpha * pulseFactor).coerceIn(0.08f, 0.90f)
                    val pulseColor = baseGridColor.copy(alpha = currentAlpha)

                    for (i in 1 until gridCount) {
                        val x = i * cellSize
                        drawLine(
                            color = pulseColor,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f
                        )
                        val y = i * cellSize
                        drawLine(
                            color = pulseColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }
                }

                else -> {
                    // Standard static grid lines
                    for (i in 1 until gridCount) {
                        val x = i * cellSize
                        drawLine(
                            color = baseGridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f
                        )
                        val y = i * cellSize
                        drawLine(
                            color = baseGridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }
    }
}
