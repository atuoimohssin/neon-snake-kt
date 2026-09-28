package com.example.snake.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class UnlockType {
    FREE,
    COINS,
    LEVEL
}

enum class FloorAnimationType {
    NONE,
    FLOW,
    PULSE,
    AURORA
}

data class FloorTheme(
    val id: String,
    val name: String,
    val unlockType: UnlockType,
    val price: Int = 0,
    val requiredLevel: Int = 1,
    val animated: Boolean = false,
    val animationType: FloorAnimationType = FloorAnimationType.NONE,
    val bgColors: List<Color>,
    val gridLineColor: Color,
    val glowColor: Color,
    val borderColor: Color,
    val description: String = ""
) {
    val brush: Brush = Brush.verticalGradient(bgColors)
}

object FloorCatalog {
    val CLASSIC = FloorTheme(
        id = "classic",
        name = "Classic Dark",
        unlockType = UnlockType.FREE,
        price = 0,
        requiredLevel = 1,
        animated = false,
        animationType = FloorAnimationType.NONE,
        bgColors = listOf(Color(0xFF0F172A), Color(0xFF030712)),
        gridLineColor = Color(0x1A38BDF8),
        glowColor = Color(0xFF00F5D4),
        borderColor = Color(0xFF1E293B),
        description = "Standard arcade cyber floor"
    )

    val MIDNIGHT = FloorTheme(
        id = "midnight",
        name = "Midnight Void",
        unlockType = UnlockType.COINS,
        price = 30,
        requiredLevel = 1,
        animated = false,
        animationType = FloorAnimationType.NONE,
        bgColors = listOf(Color(0xFF1E1035), Color(0xFF0A0512)),
        gridLineColor = Color(0x28A855F7),
        glowColor = Color(0xFFC084FC),
        borderColor = Color(0xFF3B1D66),
        description = "Deep amethyst void atmosphere"
    )

    val OCEAN = FloorTheme(
        id = "ocean",
        name = "Ocean Abyss",
        unlockType = UnlockType.COINS,
        price = 60,
        requiredLevel = 1,
        animated = false,
        animationType = FloorAnimationType.NONE,
        bgColors = listOf(Color(0xFF052B3B), Color(0xFF02131D)),
        gridLineColor = Color(0x2806B6D4),
        glowColor = Color(0xFF22D3EE),
        borderColor = Color(0xFF0E4A63),
        description = "Bioluminescent deep ocean grid"
    )

    val SUNSET = FloorTheme(
        id = "sunset",
        name = "Sunset Neon",
        unlockType = UnlockType.COINS,
        price = 90,
        requiredLevel = 1,
        animated = false,
        animationType = FloorAnimationType.NONE,
        bgColors = listOf(Color(0xFF3B1124), Color(0xFF18060E)),
        gridLineColor = Color(0x30F43F5E),
        glowColor = Color(0xFFFB7185),
        borderColor = Color(0xFF631D38),
        description = "Warm synthwave dusk horizon"
    )

    val MATRIX = FloorTheme(
        id = "matrix",
        name = "Matrix Terminal",
        unlockType = UnlockType.LEVEL,
        price = 0,
        requiredLevel = 4,
        animated = false,
        animationType = FloorAnimationType.NONE,
        bgColors = listOf(Color(0xFF05220D), Color(0xFF010B04)),
        gridLineColor = Color(0x3322C55E),
        glowColor = Color(0xFF4ADE80),
        borderColor = Color(0xFF14532D),
        description = "Static green mainframe grid"
    )

    val FLOW = FloorTheme(
        id = "flow",
        name = "Cyber Flow",
        unlockType = UnlockType.COINS,
        price = 150,
        requiredLevel = 1,
        animated = true,
        animationType = FloorAnimationType.FLOW,
        bgColors = listOf(Color(0xFF0C1938), Color(0xFF040A1A)),
        gridLineColor = Color(0x4038BDF8),
        glowColor = Color(0xFF60A5FA),
        borderColor = Color(0xFF1E3A8A),
        description = "Grid lines moving steadily forward"
    )

    val PULSE = FloorTheme(
        id = "pulse",
        name = "Neon Pulse",
        unlockType = UnlockType.LEVEL,
        price = 0,
        requiredLevel = 7,
        animated = true,
        animationType = FloorAnimationType.PULSE,
        bgColors = listOf(Color(0xFF280B33), Color(0xFF0E0314)),
        gridLineColor = Color(0x40EC4899),
        glowColor = Color(0xFFF472B6),
        borderColor = Color(0xFF581C87),
        description = "Breathing rhythm across grid lines"
    )

    val AURORA = FloorTheme(
        id = "aurora",
        name = "Aurora Borealis",
        unlockType = UnlockType.LEVEL,
        price = 0,
        requiredLevel = 10,
        animated = true,
        animationType = FloorAnimationType.AURORA,
        bgColors = listOf(Color(0xFF0B2B28), Color(0xFF1E1035), Color(0xFF071120)),
        gridLineColor = Color(0x3834D399),
        glowColor = Color(0xFFA78BFA),
        borderColor = Color(0xFF312E81),
        description = "Shifting northern lights gradient"
    )

    val ALL_FLOORS: List<FloorTheme> = listOf(
        CLASSIC,
        MIDNIGHT,
        OCEAN,
        SUNSET,
        MATRIX,
        FLOW,
        PULSE,
        AURORA
    )

    fun getFloor(id: String): FloorTheme {
        return ALL_FLOORS.find { it.id == id } ?: CLASSIC
    }
}
