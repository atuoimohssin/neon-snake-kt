package com.example.snake.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.snake.model.SnakeGameState
import kotlin.math.abs
import kotlin.math.roundToInt

private val GuideLineDashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
private val GuideLineColor = Color(0x6600F5D4)
private val SnappedGuideLineColor = Color(0xFF00F5D4)

@Composable
fun CustomizeControlsDialog(
    state: SnakeGameState,
    onSave: (dpadScale: Float, offsetX: Float, offsetY: Float, pauseButtonScale: Float, leftHanded: Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    // Local draft states - will only be persisted to DataStore upon tapping "Save"
    var draftScale by remember(state.dpadScale) { mutableFloatStateOf(state.dpadScale.coerceIn(0.7f, 1.5f)) }
    var draftOffsetX by remember(state.dpadOffsetX) { mutableFloatStateOf(state.dpadOffsetX) }
    var draftOffsetY by remember(state.dpadOffsetY) { mutableFloatStateOf(state.dpadOffsetY) }
    var draftLeftHanded by remember(state.leftHandedControls) { mutableStateOf(state.leftHandedControls) }
    var isDragging by remember { mutableStateOf(false) }

    // Consistent LTR coordinate system for absolute screen drag behavior
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Dialog(
            onDismissRequest = {
                onSave(draftScale, draftOffsetX, draftOffsetY, 1.0f, draftLeftHanded)
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            BackHandler {
                onSave(draftScale, draftOffsetX, draftOffsetY, 1.0f, draftLeftHanded)
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .testTag("customize_controls_screen"),
                color = Color(0xFF070B14)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF00F5D4),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "تخصيص الأزرار",
                                color = Color(0xFFF8FAFC),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Top Quick Save Button
                            Button(
                                onClick = {
                                    onSave(draftScale, draftOffsetX, draftOffsetY, 1.0f, draftLeftHanded)
                                },
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("top_save_controls_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF030712),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "حفظ",
                                    color = Color(0xFF030712),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            IconButton(
                                onClick = {
                                    onSave(draftScale, draftOffsetX, draftOffsetY, 1.0f, draftLeftHanded)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("close_customize_controls")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Save and Close",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Instruction Hint
                    Text(
                        text = "اسحب الأزرار لتغيير موضعها، أو استخدم الشريط لتغيير الحجم",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Live Settings Toolbar (Scale Slider & Left-handed switch)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Slider Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "D-Pad Size: ${(draftScale * 100).roundToInt()}%",
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when {
                                        draftScale < 0.9f -> "Compact"
                                        draftScale > 1.2f -> "Large"
                                        else -> "Normal"
                                    },
                                    color = Color(0xFF00F5D4),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Slider(
                            value = draftScale,
                            onValueChange = { draftScale = it.coerceIn(0.7f, 1.5f) },
                            valueRange = 0.7f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00F5D4),
                                activeTrackColor = Color(0xFF0D9488),
                                inactiveTrackColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .testTag("dpad_scale_slider")
                        )

                        // Left-handed layout toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Left-handed layout",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = draftLeftHanded,
                                onCheckedChange = { draftLeftHanded = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF00F5D4),
                                    checkedTrackColor = Color(0xFF0D9488)
                                ),
                                modifier = Modifier
                                    .size(width = 46.dp, height = 28.dp)
                                    .testTag("left_handed_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Real Game Screen Layout Preview (HUD Placeholder + Board Placeholder + Interactive D-Pad)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF030712))
                            .border(1.5.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    ) {
                        // Mini HUD Preview
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NEON SNAKE PREVIEW",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "❚❚",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Real Game Board Placeholder (Aspect ratio 1:1, non-interactive visual boundary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.1f, fill = false)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0B1329))
                                .border(1.5.dp, Color(0xFF00F5D4).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = Color(0xFF00F5D4).copy(alpha = 0.3f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "GAME BOARD AREA",
                                    color = Color(0xFF00F5D4).copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "(Controls will never overlap this boundary)",
                                    color = Color(0xFF475569),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Interactive Control Area below the game board
                        val density = LocalDensity.current
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val containerWidthDp = maxWidth
                            val containerHeightDp = maxHeight

                            // Dimensions of the D-pad (base ~140dp)
                            val baseDpadDim = 140f
                            val scaledWidth = baseDpadDim * draftScale
                            val scaledHeight = baseDpadDim * draftScale

                            // Clamp bounds so D-pad always stays completely inside the control area,
                            // never overlaps the game board above, and never leaves the screen.
                            val maxBoundXDp = ((containerWidthDp.value - scaledWidth) / 2f).coerceAtLeast(0f)
                            val maxBoundYDp = ((containerHeightDp.value - scaledHeight) / 2f).coerceAtLeast(0f)

                            val isSnappedToCenter = abs(draftOffsetX) < 0.5f

                            // Guide Lines Canvas visible during drag
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (isDragging) {
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f

                                    // Vertical Center Guide
                                    drawLine(
                                        color = if (isSnappedToCenter) SnappedGuideLineColor else GuideLineColor,
                                        start = Offset(centerX, 0f),
                                        end = Offset(centerX, size.height),
                                        strokeWidth = if (isSnappedToCenter) 2.5f else 1.5f,
                                        pathEffect = GuideLineDashEffect
                                    )

                                    // Horizontal Center Guide
                                    drawLine(
                                        color = GuideLineColor,
                                        start = Offset(0f, centerY),
                                        end = Offset(size.width, centerY),
                                        strokeWidth = 1.5f,
                                        pathEffect = GuideLineDashEffect
                                    )
                                }
                            }

                            // The Real Arcade D-Pad with Live Preview and Drag Gesture Handler
                            ArcadeControls(
                                onTurn = {},
                                scale = draftScale,
                                offsetX = draftOffsetX,
                                offsetY = draftOffsetY,
                                leftHanded = draftLeftHanded,
                                enabled = false,
                                modifier = Modifier
                                    .testTag("customizable_dpad")
                                    .pointerInput(density, maxBoundXDp, maxBoundYDp) {
                                        detectDragGestures(
                                            onDragStart = { isDragging = true },
                                            onDragEnd = { isDragging = false },
                                            onDragCancel = { isDragging = false },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val densityVal = density.density
                                                val deltaXDp = dragAmount.x / densityVal
                                                val deltaYDp = dragAmount.y / densityVal

                                                // RTL-proof: uses absolute screen offsets
                                                val effectiveSign = if (draftLeftHanded) -1f else 1f
                                                val unClampedX = draftOffsetX + (deltaXDp * effectiveSign)
                                                val unClampedY = draftOffsetY + deltaYDp

                                                // Snap to horizontal center if within 14dp
                                                val snappedX = if (abs(unClampedX) < 14f) 0f else unClampedX

                                                draftOffsetX = snappedX.coerceIn(-maxBoundXDp, maxBoundXDp)
                                                draftOffsetY = unClampedY.coerceIn(-maxBoundYDp, maxBoundYDp)
                                            }
                                        )
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons Row: Reset & Save Changes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reset to defaults
                        OutlinedButton(
                            onClick = {
                                draftScale = 1.0f
                                draftOffsetX = 0f
                                draftOffsetY = 0f
                                draftLeftHanded = false
                                onReset()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("reset_controls_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF475569))
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إعادة ضبط",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Wide Glowing Save Button
                        Button(
                            onClick = {
                                onSave(draftScale, draftOffsetX, draftOffsetY, 1.0f, draftLeftHanded)
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(46.dp)
                                .testTag("save_controls_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = Color(0xFF030712),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حفظ التعديلات (Save)",
                                color = Color(0xFF030712),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
