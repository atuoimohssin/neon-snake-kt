package com.example.snake.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snake.model.Cell
import com.example.snake.model.Dir
import com.example.snake.model.FloatingScore
import com.example.snake.model.GRID_SIZE
import com.example.snake.model.GameStatus
import com.example.snake.model.SnakeGameState
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.example.snake.model.GameState
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameBoard(
    state: SnakeGameState,
    onTurn: (Dir) -> Unit,
    onResume: () -> Unit = {},
    onStartGame: () -> Unit = {},
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

    // 2. Pulse animation for 3D glowing food & coins
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

    // 3. Shared Continuous Floor Animation (Flow, Pulse, Aurora)
    val floorAnimTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floor_anim_time"
    )

    // Pause animations when the game is paused
    var frozenFloorAnimTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.status) {
        if (state.status == GameStatus.PAUSED) {
            frozenFloorAnimTime = floorAnimTime
        }
    }
    val effectiveFloorAnimTime = if (state.status == GameStatus.PAUSED) frozenFloorAnimTime else floorAnimTime

    val currentFloor = state.selectedFloor
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
            .pointerInput(state.gameState) {
                // Ignore swipe input unless state is Playing or Ready (where first input starts the game)
                if (state.gameState != GameState.Playing && state.gameState != GameState.Ready) {
                    return@pointerInput
                }
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
        // Tilted 3D Floor Canvas Container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationX = cameraSwayX
                    rotationY = cameraSwayY
                    cameraDistance = 14f * density
                    transformOrigin = TransformOrigin(0.5f, 0.72f)
                }
                .clip(RoundedCornerShape(22.dp))
                .border(2.dp, currentFloor.borderColor, RoundedCornerShape(22.dp))
        ) {
            val totalBoardPx = constraints.maxWidth.toFloat()
            val singleCellPx = totalBoardPx / GRID_SIZE

            Canvas(modifier = Modifier.fillMaxSize()) {
                val boardWidth = size.width
                val boardHeight = size.height
                val cellSize = boardWidth / GRID_SIZE
                val cubeHeight = cellSize * 0.32f

                // 1. Render Dynamic Floor (Theme Background Gradient + Grid Lines + Flow/Pulse/Aurora Animation)
                FloorRenderer.drawFloor(
                    drawScope = this,
                    floor = currentFloor,
                    animTime = effectiveFloorAnimTime,
                    gridCount = GRID_SIZE,
                    reduceAnimations = state.reduceAnimations,
                    forceStatic = (state.status == GameStatus.PAUSED)
                )

                // 1.5. Render Obstacles (3D Neon Blocks with Drop Shadow)
                val obstacleShadowColor = Color(0x66000000)
                val obstacleTopColor = Color(0xFFE11D48)
                val obstacleSideColor = Color(0xFF881337)
                val obstacleBorderColor = Color(0xFFFB7185)

                state.obstacles.forEach { obstacle ->
                    val obLeft = obstacle.x * cellSize + 1.5f
                    val obTop = obstacle.y * cellSize + 1.5f
                    val obDim = cellSize - 3f

                    // Soft floor shadow
                    drawRoundRect(
                        color = obstacleShadowColor,
                        topLeft = Offset(obLeft + 1f, obTop + 3f),
                        size = Size(obDim, obDim + 2f),
                        cornerRadius = CornerRadius(obDim * 0.2f, obDim * 0.2f)
                    )

                    // Extruded side face (dark ruby)
                    drawRoundRect(
                        color = obstacleSideColor,
                        topLeft = Offset(obLeft, obTop + obDim * 0.35f - cubeHeight),
                        size = Size(obDim, obDim * 0.65f + cubeHeight),
                        cornerRadius = CornerRadius(obDim * 0.2f, obDim * 0.2f)
                    )

                    // Raised top face
                    val topFaceY = obTop - cubeHeight
                    drawRoundRect(
                        color = obstacleTopColor,
                        topLeft = Offset(obLeft, topFaceY),
                        size = Size(obDim, obDim),
                        cornerRadius = CornerRadius(obDim * 0.2f, obDim * 0.2f)
                    )

                    // Top face neon highlight border
                    drawRoundRect(
                        color = obstacleBorderColor,
                        topLeft = Offset(obLeft + 1f, topFaceY + 1f),
                        size = Size(obDim - 2f, obDim - 2f),
                        cornerRadius = CornerRadius(obDim * 0.18f, obDim * 0.18f),
                        style = Stroke(width = 1.5f)
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

                // 3. Render 3D Gold Coin Pickup (if active)
                state.coinPickup?.let { coin ->
                    val cCell = coin.cell
                    val cCenterX = cCell.x * cellSize + cellSize / 2f
                    val cCenterY = cCell.y * cellSize + cellSize / 2f
                    val cRadius = (cellSize / 2.3f) * pulseScale
                    val cFloat = 6f * pulseScale

                    // Floor Drop Shadow under coin
                    drawOval(
                        color = Color(0x66000000),
                        topLeft = Offset(cCenterX - cRadius * 0.9f, cCenterY + 3f),
                        size = Size(cRadius * 1.8f, cRadius * 0.75f)
                    )

                    // Golden Outer Glow Aura
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = glowAlpha * 0.8f),
                        radius = cRadius * 1.6f,
                        center = Offset(cCenterX, cCenterY - cFloat)
                    )

                    // Circular Countdown Ring around coin
                    val coinProgress = coin.remainingMillis.toFloat() / coin.totalMillis
                    drawArc(
                        color = Color(0xFFFFD700),
                        startAngle = -90f,
                        sweepAngle = 360f * coinProgress,
                        useCenter = false,
                        topLeft = Offset(cCenterX - cRadius * 1.25f, cCenterY - cFloat - cRadius * 1.25f),
                        size = Size(cRadius * 2.5f, cRadius * 2.5f),
                        style = Stroke(width = 2.5f)
                    )

                    // 3D Coin Outer Disc
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFF7B2),
                                Color(0xFFFFD700),
                                Color(0xFFD97706),
                                Color(0xFF78350F)
                            ),
                            center = Offset(cCenterX - cRadius * 0.25f, cCenterY - cFloat - cRadius * 0.25f),
                            radius = cRadius
                        ),
                        radius = cRadius,
                        center = Offset(cCenterX, cCenterY - cFloat)
                    )

                    // Coin Inner Embossed Ring
                    drawCircle(
                        color = Color(0xFFFFFBEB).copy(alpha = 0.8f),
                        radius = cRadius * 0.65f,
                        center = Offset(cCenterX, cCenterY - cFloat),
                        style = Stroke(width = 1.5f)
                    )

                    // Coin Center Dot / Specular reflection
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = cRadius * 0.2f,
                        center = Offset(cCenterX - cRadius * 0.28f, cCenterY - cFloat - cRadius * 0.28f)
                    )
                }

                // 4. Render Special Bonus (3D Golden Star with rotating countdown aura)
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

                // 5. Render Snake 3D Cubes (Tail to Head)
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

            // 6. Floating Score Points Animation Overlay (mapped to cell positions)
            for (floatingScore in state.activeFloatingScores) {
                val startX = (floatingScore.cell.x * singleCellPx)
                val startY = (floatingScore.cell.y * singleCellPx)
                FloatingScoreView(
                    score = floatingScore,
                    modifier = Modifier.offset { IntOffset(startX.roundToInt(), startY.roundToInt()) }
                )
            }
        }

        // Top Overlay 1: Ready Prompt
        AnimatedVisibility(
            visible = state.gameState == GameState.Ready,
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

        // Top Overlay 2: Active Bonus Timer Pill / Coin Timer Pill
        Row(
            modifier = Modifier
                .padding(top = 10.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.specialBonus?.let { bonus ->
                Box(
                    modifier = Modifier
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

            state.coinPickup?.let { coin ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xD8422006))
                        .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "COIN (${(coin.remainingMillis / 1000f).coerceAtLeast(0f).let { String.format("%.1fs", it) }})",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Overlay: Lobby Card
        if (state.gameState == GameState.Lobby) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xF00B132B))
                    .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "NEON SNAKE",
                        color = Color(0xFF00F5D4),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "LEVEL ${state.currentLevel.id} • ${state.currentLevel.targetFood} FOOD TARGET",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onStartGame,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("lobby_start_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF0B132B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "START GAME",
                            color = Color(0xFF0B132B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Overlay: Paused Overlay with Resume Button
        if (state.gameState == GameState.Paused || state.status == GameStatus.PAUSED) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFA0B132B))
                    .border(2.dp, Color(0xFFFFD166), RoundedCornerShape(20.dp))
                    .padding(horizontal = 28.dp, vertical = 20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "PAUSED",
                        color = Color(0xFFFFD166),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("resume_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF0B132B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RESUME",
                            color = Color(0xFF0B132B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingScoreView(
    score: FloatingScore,
    modifier: Modifier = Modifier
) {
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(score.id) {
        launch {
            offsetY.animateTo(
                targetValue = -34f,
                animationSpec = tween(durationMillis = 850, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            delay(350L)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .alpha(alpha.value)
    ) {
        Text(
            text = score.text,
            color = score.color,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x99000000))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
