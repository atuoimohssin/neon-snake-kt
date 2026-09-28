package com.example.snake.model

data class LevelConfig(
    val id: Int,
    val targetFood: Int,
    val tickMs: Long,
    val obstacles: List<Cell>,
    val coinReward: Int
)

object LevelRepository {

    val levels: List<LevelConfig> = listOf(
        // Level 1: Introduction, no obstacles
        LevelConfig(
            id = 1,
            targetFood = 8,
            tickMs = 160L,
            obstacles = emptyList(),
            coinReward = 5
        ),

        // Level 2: Faster, no obstacles
        LevelConfig(
            id = 2,
            targetFood = 10,
            tickMs = 148L,
            obstacles = emptyList(),
            coinReward = 8
        ),

        // Level 3: Corner blocks
        LevelConfig(
            id = 3,
            targetFood = 12,
            tickMs = 138L,
            obstacles = listOf(
                Cell(4, 4), Cell(5, 4),
                Cell(14, 4), Cell(15, 4),
                Cell(4, 15), Cell(5, 15),
                Cell(14, 15), Cell(15, 15)
            ),
            coinReward = 12
        ),

        // Level 4: Vertical barrier pillars (avoiding center row y=10)
        LevelConfig(
            id = 4,
            targetFood = 14,
            tickMs = 128L,
            obstacles = listOf(
                Cell(5, 5), Cell(5, 6), Cell(5, 7),
                Cell(5, 13), Cell(5, 14), Cell(5, 15),
                Cell(14, 5), Cell(14, 6), Cell(14, 7),
                Cell(14, 13), Cell(14, 14), Cell(14, 15)
            ),
            coinReward = 16
        ),

        // Level 5: Cross layout
        LevelConfig(
            id = 5,
            targetFood = 16,
            tickMs = 118L,
            obstacles = listOf(
                Cell(4, 7), Cell(5, 7), Cell(6, 7),
                Cell(13, 7), Cell(14, 7), Cell(15, 7),
                Cell(4, 13), Cell(5, 13), Cell(6, 13),
                Cell(13, 13), Cell(14, 13), Cell(15, 13)
            ),
            coinReward = 20
        ),

        // Level 6: L-shaped corners
        LevelConfig(
            id = 6,
            targetFood = 18,
            tickMs = 108L,
            obstacles = listOf(
                Cell(3, 3), Cell(4, 3), Cell(5, 3), Cell(3, 4), Cell(3, 5),
                Cell(16, 3), Cell(15, 3), Cell(14, 3), Cell(16, 4), Cell(16, 5),
                Cell(3, 16), Cell(4, 16), Cell(5, 16), Cell(3, 15), Cell(3, 14),
                Cell(16, 16), Cell(15, 16), Cell(14, 16), Cell(16, 15), Cell(16, 14)
            ),
            coinReward = 25
        ),

        // Level 7: Diamond & Center bumpers
        LevelConfig(
            id = 7,
            targetFood = 20,
            tickMs = 98L,
            obstacles = listOf(
                Cell(10, 4), Cell(10, 5),
                Cell(10, 15), Cell(10, 16),
                Cell(4, 10), Cell(5, 10),
                Cell(14, 10), Cell(15, 10),
                Cell(6, 6), Cell(13, 6),
                Cell(6, 14), Cell(13, 14)
            ),
            coinReward = 32
        ),

        // Level 8: Dual Corridor dividers
        LevelConfig(
            id = 8,
            targetFood = 22,
            tickMs = 88L,
            obstacles = listOf(
                Cell(4, 5), Cell(5, 5), Cell(6, 5), Cell(7, 5),
                Cell(12, 5), Cell(13, 5), Cell(14, 5), Cell(15, 5),
                Cell(4, 15), Cell(5, 15), Cell(6, 15), Cell(7, 15),
                Cell(12, 15), Cell(13, 15), Cell(14, 15), Cell(15, 15),
                Cell(2, 10), Cell(17, 10)
            ),
            coinReward = 40
        ),

        // Level 9: Double labyrinth pillars
        LevelConfig(
            id = 9,
            targetFood = 24,
            tickMs = 78L,
            obstacles = listOf(
                Cell(4, 4), Cell(4, 5), Cell(4, 6),
                Cell(4, 14), Cell(4, 15), Cell(4, 16),
                Cell(15, 4), Cell(15, 5), Cell(15, 6),
                Cell(15, 14), Cell(15, 15), Cell(15, 16),
                Cell(8, 4), Cell(11, 4),
                Cell(8, 16), Cell(11, 16),
                Cell(3, 10), Cell(16, 10)
            ),
            coinReward = 50
        ),

        // Level 10: Neon Grand Master Arena
        LevelConfig(
            id = 10,
            targetFood = 26,
            tickMs = 70L,
            obstacles = listOf(
                Cell(3, 3), Cell(3, 4), Cell(4, 3),
                Cell(16, 3), Cell(16, 4), Cell(15, 3),
                Cell(3, 16), Cell(3, 15), Cell(4, 16),
                Cell(16, 16), Cell(16, 15), Cell(15, 16),
                Cell(7, 6), Cell(12, 6),
                Cell(7, 14), Cell(12, 14),
                Cell(5, 10), Cell(14, 10),
                Cell(10, 3), Cell(10, 17)
            ),
            coinReward = 75
        )
    )

    fun getLevel(id: Int): LevelConfig {
        return levels.firstOrNull { it.id == id } ?: levels.first()
    }
}
