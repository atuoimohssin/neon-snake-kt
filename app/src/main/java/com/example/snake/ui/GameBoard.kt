package com.example.snake.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snake.model.Cell
import com.example.snake.model.Dir
import com.example.snake.model.GRID_SIZE
import com.example.snake.model.GameStatus
import com.example.snake.model.SnakeGameState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameBoard(
    state: SnakeGameState,
    onTurn: (Dir) -> Unit,
    modifier: Modifier = Modifier
) {
    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 32f

    // 1. Camera Sway based on Snake Head position
    val head = state.snake.firstOrNull() ?: Cell(10, 10)
    val targetSwayY = ((head.x - (GRID_SIZE / 2f)) / (GRID_SIZE / 2f)) * 4.5f
    val targetSwayX = 43f + ((head.y - (GRID_SIZE / 2f)) / (GRID_SIZE / 2f)) * 3.5f

    val cameraSwayX by animateFloatAsState(
        targetValue = targetSwayX,
        animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
        label = "sway_x"
    )
    val cameraSwayY by animateFloatAsState(
        targetValue = targetSwayY,
        animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
        label = "sway_y"
    )

    // 2. Pulse animation for 3D glowing food
    val infiniteTransition = rememberInfiniteTransition(label = "pseudo3d_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val density = LocalDensity.current.density

    // Pre-allocated reusable Path for bonus star to guarantee 60 FPS (zero allocation in drawScope)
    val reusableStarPath = remember { Path() }

    // Outer Screen-Space Container: Gestures are registered here in pure 2D coordinates!
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .testTag("game_board")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        accumulatedDragX = 0f
                        accumulatedDragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragX += dragAmount.x
                        accumulatedDragY += dragAmount.y

                        if (abs(accumulatedDragX) > dragThreshold || abs(accumulatedDragY) > dragThreshold) {
                            if (abs(accumulatedDragX) > abs(accumulatedDragY)) {
                                if (accumulatedDragX > 0) onTurn(Dir.RIGHT) else onTurn(Dir.LEFT)
                            } else {
                                if (accumulatedDragY > 0) onTurn(Dir.DOWN) else onTurn(Dir.UP)
                            }
                            accumulatedDragX = 0f
                            accumulatedDragY = 0f
                        }
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Tilted 3D Floor Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = cameraSwayX
                    rotationY = cameraSwayY
                    cameraDistance = 14f * density
                    transformOrigin = TransformOrigin(0.5f, 0.72f)
                }
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF030712)
                        )
                    )
                )
                .border(2.dp, Color(0xFF1E293B), RoundedCornerShape(22.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boardWidth = size.width
                val boardHeight = size.height
                val cellSize = boardWidth / GRID_SIZE
                val cubeHeight = cellSize * 0.32f

                // 1. Grid Lines with perspective grid styling
                val gridColor = Color(0x1A38BDF8)
                for (i in 1 until GRID_SIZE) {
                    val x = i * cellSize
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, boardHeight),
                        strokeWidth = 1f
                    )
                    val y = i * cellSize
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(boardWidth, y),
                        strokeWidth = 1f
                    )
                }

                // 2. Render Food (3D Sphere with shadow, gradient, and specular highlight)
                val foodCell = state.food.cell
                val foodCenterX = foodCell.x * cellSize + cellSize / 2f
                val foodCenterY = foodCell.y * cellSize + cellSize / 2f
                val baseRadius = cellSize / 2.3f
                val currentRadius = baseRadius * pulseScale
                val floatOffset = 5f * pulseScale

                // Floor Drop Shadow under food
                drawOval(
                    color = Color(0x55000000),
                    topLeft = Offset(foodCenterX - currentRadius * 0.85f, foodCenterY + 2f),
                    size = Size(currentRadius * 1.7f, currentRadius * 0.8f)
                )

                // Soft Outer Neon Glow
                drawCircle(
                    color = Color(0xFFFF2A6D).copy(alpha = glowAlpha * 0.65f),
                    radius = currentRadius * 1.55f,
                    center = Offset(foodCenterX, foodCenterY - floatOffset)
                )

                // 3D Sphere Body with Radial Light Gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFB3C6),
                            Color(0xFFFF2A6D),
                            Color(0xFFCC0044),
                            Color(0xFF660022)
                        ),
                        center = Offset(
                            foodCenterX - currentRadius * 0.35f,
                            foodCenterY - floatOffset - currentRadius * 0.35f
                        ),
                        radius = currentRadius * 1.15f
                    ),
                    radius = currentRadius,
                    center = Offset(foodCenterX, foodCenterY - floatOffset)
                )

                // Specular Light Spot
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = currentRadius * 0.22f,
                    center = Offset(
                        foodCenterX - currentRadius * 0.35f,
                        foodCenterY - floatOffset - currentRadius * 0.35f
                    )
                )

                // Little glowing neon leaf
                drawCircle(
                    color = Color(0xFF00F5D4),
                    radius = currentRadius * 0.28f,
                    center = Offset(foodCenterX + currentRadius * 0.35f, foodCenterY - floatOffset - currentRadius * 0.6f)
                )

                // 3. Render Special Bonus (3D Golden Star with rotating countdown aura)
                state.specialBonus?.let { bonus ->
                    val bCell = bonus.cell
                    val bCenterX = bCell.x * cellSize + cellSize / 2f
                    val bCenterY = bCell.y * cellSize + cellSize / 2f
                    val bRadius = (cellSize / 2.2f) * (pulseScale * 1.05f)
                    val bFloat = 7f * pulseScale

                    // Shadow
                    drawOval(
                        color = Color(0x66000000),
                        topLeft = Offset(bCenterX - bRadius, bCenterY + 3f),
                        size = Size(bRadius * 2f, bRadius * 0.9f)
                    )

                    // Golden Glow Aura
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = glowAlpha * 0.85f),
                        radius = bRadius * 1.7f,
                        center = Offset(bCenterX, bCenterY - bFloat)
                    )

                    // Countdown Ring
                    val progressFraction = bonus.remainingMillis.toFloat() / bonus.totalMillis
                    drawArc(
                        color = Color(0xFFFFD700),
                        startAngle = -90f,
                        sweepAngle = 360f * progressFraction,
                        useCenter = false,
                        topLeft = Offset(bCenterX - bRadius * 1.3f, bCenterY - bFloat - bRadius * 1.3f),
                        size = Size(bRadius * 2.6f, bRadius * 2.6f),
                        style = Stroke(width = 3f)
                    )

                    // 3D Star Path (zero allocations: reuse pre-allocated Path)
                    reusableStarPath.reset()
                    val points = 5
                    val outerR = bRadius
                    val innerR = bRadius * 0.46f
                    for (p in 0 until (points * 2)) {
                        val r = if (p % 2 == 0) outerR else innerR
                        val angle = (p * Math.PI / points) - (Math.PI / 2)
                        val px = bCenterX + (r * cos(angle)).toFloat()
                        val py = bCenterY - bFloat + (r * sin(angle)).toFloat()
                        if (p == 0) reusableStarPath.moveTo(px, py) else reusableStarPath.lineTo(px, py)
                    }
                    reusableStarPath.close()

                    drawPath(
                        path = reusableStarPath,
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFFFD700), Color(0xFFFF8800)),
                            center = Offset(bCenterX, bCenterY - bFloat),
                            radius = bRadius
                        )
                    )
                }

                // 4. Render Snake 3D Cubes (Tail to Head)
                val snake = state.snake
                val headCyan = Color(0xFF00F5D4)
                val tailBlue = Color(0xFF0077B6)

                // Pass 1: Floor drop shadows for all segments
                snake.forEach { cell ->
                    val cellLeft = cell.x * cellSize + 1.5f
                    val cellTop = cell.y * cellSize + 1.5f
                    val cellDim = cellSize - 3f

                    drawRoundRect(
                        color = Color(0x55000000),
                        topLeft = Offset(cellLeft + 2f, cellTop + 4f),
                        size = Size(cellDim, cellDim),
                        cornerRadius = CornerRadius(cellDim * 0.28f, cellDim * 0.28f)
                    )
                }

                // Pass 2: 3D Cubes (Side Faces + Top Faces)
                snake.reversed().forEachIndexed { indexFromTail, cell ->
                    val isHead = indexFromTail == snake.size - 1
                    val cellLeft = cell.x * cellSize + 1.5f
                    val cellTop = cell.y * cellSize + 1.5f
                    val cellDim = cellSize - 3f

                    val progress = if (snake.size > 1) {
                        indexFromTail.toFloat() / (snake.size - 1)
                    } else 1f

                    val topFaceColor = androidx.compose.ui.graphics.lerp(tailBlue, headCyan, progress)
                    // Darker shaded color for the front 3D extrusion wall
                    val frontFaceColor = topFaceColor.copy(
                        red = topFaceColor.red * 0.55f,
                        green = topFaceColor.green * 0.55f,
                        blue = topFaceColor.blue * 0.55f,
                        alpha = 0.95f
                    )

                    // A. Front Extrusion Face (Giving depth towards the bottom)
                    drawRoundRect(
                        color = frontFaceColor,
                        topLeft = Offset(cellLeft, cellTop + cellDim - cubeHeight),
                        size = Size(cellDim, cubeHeight),
                        cornerRadius = CornerRadius(cellDim * 0.2f, cellDim * 0.2f)
                    )

                    // B. Raised Top Face
                    val topFaceTop = cellTop - cubeHeight
                    drawRoundRect(
                        color = topFaceColor,
                        topLeft = Offset(cellLeft, topFaceTop),
                        size = Size(cellDim, cellDim),
                        cornerRadius = CornerRadius(cellDim * 0.32f, cellDim * 0.32f)
                    )

                    // C. Top Face Neon Highlight Border
                    drawRoundRect(
                        color = Color.White.copy(alpha = if (isHead) 0.55f else 0.28f),
                        topLeft = Offset(cellLeft + 0.8f, topFaceTop + 0.8f),
                        size = Size(cellDim - 1.6f, cellDim - 1.6f),
                        cornerRadius = CornerRadius(cellDim * 0.30f, cellDim * 0.30f),
                        style = Stroke(width = 1.2f)
                    )

                    // D. If Head: Render 3D Eyes on the Top Face
                    if (isHead) {
                        val eyeRadius = cellDim * 0.16f
                        val pupilRadius = eyeRadius * 0.55f

                        val (eye1, eye2) = when (state.direction) {
                            Dir.UP -> Pair(
                                Offset(cellLeft + cellDim * 0.3f, topFaceTop + cellDim * 0.28f),
                                Offset(cellLeft + cellDim * 0.7f, topFaceTop + cellDim * 0.28f)
                            )
                            Dir.DOWN -> Pair(
                                Offset(cellLeft + cellDim * 0.3f, topFaceTop + cellDim * 0.72f),
                                Offset(cellLeft + cellDim * 0.7f, topFaceTop + cellDim * 0.72f)
                            )
                            Dir.LEFT -> Pair(
                                Offset(cellLeft + cellDim * 0.28f, topFaceTop + cellDim * 0.3f),
                                Offset(cellLeft + cellDim * 0.28f, topFaceTop + cellDim * 0.7f)
                            )
                            Dir.RIGHT -> Pair(
                                Offset(cellLeft + cellDim * 0.72f, topFaceTop + cellDim * 0.3f),
                                Offset(cellLeft + cellDim * 0.72f, topFaceTop + cellDim * 0.7f)
                            )
                        }

                        // Eye whites
                        drawCircle(color = Color.White, radius = eyeRadius, center = eye1)
                        drawCircle(color = Color.White, radius = eyeRadius, center = eye2)
                        // Pupils
                        drawCircle(color = Color(0xFF0F172A), radius = pupilRadius, center = eye1)
                        drawCircle(color = Color(0xFF0F172A), radius = pupilRadius, center = eye2)
                    }
                }
            }
        }

        // Overlay 1: Un-obscured "Swipe or Press Arrow to Start" at the top
        AnimatedVisibility(
            visible = state.status == GameStatus.IDLE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xDD030712))
                    .border(1.dp, Color(0x8800F5D4), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFF00F5D4),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Swipe or Press Arrow to Start",
                        color = Color(0xFF00F5D4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Overlay 2: Active Bonus Timer Pill
        state.specialBonus?.let { bonus ->
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xDD78350F))
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "BONUS +${bonus.points} (${(bonus.remainingMillis / 1000f).coerceAtLeast(0f).let { String.format("%.1fs", it) }})",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Overlay 3: Paused Badge
        if (state.status == GameStatus.PAUSED) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xEE000000))
                    .border(1.5.dp, Color(0xFFFFD166), RoundedCornerShape(16.dp))
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "PAUSED",
                    color = Color(0xFFFFD166),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
