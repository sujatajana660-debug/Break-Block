package com.example.model

data class LevelTarget(
    val pinkGemsRequired: Int = 0,
    val yellowStarsRequired: Int = 0,
    val emeraldsRequired: Int = 0
) {
    val totalRequired: Int get() = pinkGemsRequired + yellowStarsRequired + emeraldsRequired
}

data class AdventureLevel(
    val levelNumber: Int,
    val name: String,
    val target: LevelTarget,
    val initialBoard: BoardState,
    val isHardLevel: Boolean = false,
    val moveLimit: Int = 35
)

object AdventureCatalog {
    fun getLevel(levelNumber: Int): AdventureLevel {
        val safeNum = levelNumber.coerceAtLeast(1)
        val isHard = safeNum > 50
        val baseName = when (safeNum % 10) {
            1 -> "Sunflower Valley"
            2 -> "Golden Trophy"
            3 -> "Rubber Duck"
            4 -> "Sailboat Haven"
            5 -> "Lion's Pride"
            6 -> "Castle Keep"
            7 -> "Royal Crown"
            8 -> "Galaxy Rocket"
            9 -> "Lucky Clover"
            else -> "Mystery Mosaic"
        }

        val hardPrefix = when {
            safeNum > 2000 -> "[CHAMPION] "
            safeNum > 1500 -> "[LEGEND] "
            safeNum > 1000 -> "[MASTER] "
            safeNum > 500 -> "[ELITE] "
            safeNum > 100 -> "[EXPERT] "
            safeNum > 50 -> "[HARD] "
            else -> ""
        }

        // Balanced and fair target requirements: guaranteed to be fun and beatable
        val gemsNeeded: Int
        val starsNeeded: Int
        val emeraldsNeeded: Int

        when {
            safeNum <= 5 -> {
                // Levels 1-5: 6-10 diamonds, 12-18 stars
                starsNeeded = 14 + ((safeNum - 1) * 2)
                gemsNeeded = 6 + safeNum
                emeraldsNeeded = if (safeNum >= 3) 3 else 0
            }
            safeNum <= 20 -> {
                // Levels 6-20
                starsNeeded = 20 + (safeNum - 5)
                gemsNeeded = 10 + (safeNum % 4)
                emeraldsNeeded = 4 + (safeNum % 3)
            }
            safeNum <= 50 -> {
                // Levels 21-50
                starsNeeded = 30 + ((safeNum - 20) / 3)
                gemsNeeded = 12 + (safeNum % 5)
                emeraldsNeeded = 6 + (safeNum % 4)
            }
            else -> {
                // Higher levels (50+)
                starsNeeded = (35 + ((safeNum - 50) / 10)).coerceAtMost(55)
                gemsNeeded = 15 + (safeNum % 6)
                emeraldsNeeded = 8 + (safeNum % 5)
            }
        }

        // Unlimited moves in adventure mode (-1 represents unlimited)
        val moveLimit = -1

        val target = LevelTarget(
            pinkGemsRequired = gemsNeeded,
            yellowStarsRequired = starsNeeded,
            emeraldsRequired = emeraldsNeeded
        )

        val board = createMosaicForLevel(safeNum, target, isHard)

        return AdventureLevel(
            levelNumber = safeNum,
            name = "$hardPrefix$baseName #$safeNum",
            target = target,
            initialBoard = board,
            isHardLevel = isHard,
            moveLimit = moveLimit
        )
    }

    private fun createMosaicForLevel(levelNum: Int, target: LevelTarget, isHard: Boolean): BoardState {
        val grid = List(BOARD_SIZE) { r ->
            List(BOARD_SIZE) { c ->
                CellData()
            }.toMutableList()
        }.toMutableList()

        val pattern = (levelNum - 1) % 6
        when (pattern) {
            0 -> {
                // Flower Pattern
                val flowerCoords = listOf(
                    Pair(2, 3), Pair(2, 4),
                    Pair(3, 2), Pair(3, 3), Pair(3, 4), Pair(3, 5),
                    Pair(4, 2), Pair(4, 3), Pair(4, 4), Pair(4, 5),
                    Pair(5, 3), Pair(5, 4),
                    Pair(6, 3), Pair(6, 4) // stem
                )
                for ((r, c) in flowerCoords) {
                    val color = if (r in 3..4 && c in 3..4) BlockType.YELLOW else if (r >= 6) BlockType.GREEN else BlockType.PINK
                    grid[r][c] = CellData(blockType = color)
                }
            }
            1 -> {
                // Trophy Pattern
                val trophyCoords = listOf(
                    Pair(1, 2), Pair(1, 3), Pair(1, 4), Pair(1, 5),
                    Pair(2, 1), Pair(2, 2), Pair(2, 5), Pair(2, 6),
                    Pair(3, 1), Pair(3, 2), Pair(3, 3), Pair(3, 4), Pair(3, 5), Pair(3, 6),
                    Pair(4, 2), Pair(4, 3), Pair(4, 4), Pair(4, 5),
                    Pair(5, 3), Pair(5, 4),
                    Pair(6, 2), Pair(6, 3), Pair(6, 4), Pair(6, 5)
                )
                for ((r, c) in trophyCoords) {
                    grid[r][c] = CellData(blockType = BlockType.YELLOW)
                }
            }
            2 -> {
                // Duck Pattern
                val duckCoords = listOf(
                    Pair(1, 2), Pair(1, 3),
                    Pair(2, 1), Pair(2, 2), Pair(2, 3), Pair(2, 4),
                    Pair(3, 3), Pair(3, 4),
                    Pair(4, 3), Pair(4, 4), Pair(4, 5), Pair(4, 6),
                    Pair(5, 2), Pair(5, 3), Pair(5, 4), Pair(5, 5), Pair(5, 6),
                    Pair(6, 3), Pair(6, 4), Pair(6, 5)
                )
                for ((r, c) in duckCoords) {
                    val color = if (r == 2 && c == 1) BlockType.ORANGE else BlockType.YELLOW
                    grid[r][c] = CellData(blockType = color)
                }
            }
            3 -> {
                // Sailboat Pattern
                val boatCoords = listOf(
                    Pair(1, 4),
                    Pair(2, 3), Pair(2, 4),
                    Pair(3, 2), Pair(3, 3), Pair(3, 4),
                    Pair(4, 1), Pair(4, 2), Pair(4, 3), Pair(4, 4),
                    Pair(5, 4),
                    Pair(6, 1), Pair(6, 2), Pair(6, 3), Pair(6, 4), Pair(6, 5), Pair(6, 6)
                )
                for ((r, c) in boatCoords) {
                    val color = if (r < 5) BlockType.CYAN else if (r == 5) BlockType.YELLOW else BlockType.ORANGE
                    grid[r][c] = CellData(blockType = color)
                }
            }
            4 -> {
                // Crown / Heart Pattern
                val heartCoords = listOf(
                    Pair(2, 2), Pair(2, 3), Pair(2, 5), Pair(2, 6),
                    Pair(3, 1), Pair(3, 2), Pair(3, 3), Pair(3, 4), Pair(3, 5), Pair(3, 6), Pair(3, 7),
                    Pair(4, 2), Pair(4, 3), Pair(4, 4), Pair(4, 5), Pair(4, 6),
                    Pair(5, 3), Pair(5, 4), Pair(5, 5),
                    Pair(6, 4)
                )
                for ((r, c) in heartCoords) {
                    grid[r][c] = CellData(blockType = BlockType.RED)
                }
            }
            else -> {
                // Castle / Fortress Pattern
                val castleCoords = listOf(
                    Pair(1, 1), Pair(1, 3), Pair(1, 5),
                    Pair(2, 1), Pair(2, 2), Pair(2, 3), Pair(2, 4), Pair(2, 5),
                    Pair(3, 1), Pair(3, 2), Pair(3, 3), Pair(3, 4), Pair(3, 5),
                    Pair(4, 1), Pair(4, 2), Pair(4, 3), Pair(4, 4), Pair(4, 5)
                )
                for ((r, c) in castleCoords) {
                    val color = if (r == 1) BlockType.YELLOW else if (r == 2) BlockType.BLUE else BlockType.PURPLE
                    grid[r][c] = CellData(blockType = color)
                }
            }
        }

        // Challenging Obstacles (starts from Lv 10+ and gets harder)
        if (levelNum >= 10) {
            val obstacleCoords = when (levelNum % 5) {
                0 -> listOf(Pair(0, 0), Pair(0, 7), Pair(7, 0), Pair(7, 7))
                1 -> listOf(Pair(3, 0), Pair(4, 0), Pair(3, 7), Pair(4, 7))
                2 -> listOf(Pair(0, 3), Pair(0, 4), Pair(7, 3), Pair(7, 4))
                3 -> listOf(Pair(1, 1), Pair(1, 6), Pair(6, 1), Pair(6, 6))
                else -> listOf(Pair(2, 2), Pair(2, 5), Pair(5, 2), Pair(5, 5))
            }
            for ((r, c) in obstacleCoords) {
                grid[r][c] = CellData(blockType = BlockType.STONE)
            }
        }

        // Collect all non-stone, non-empty mosaic cells
        val playableCoords = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (grid[r][c].blockType != BlockType.EMPTY && grid[r][c].blockType != BlockType.STONE) {
                    playableCoords.add(Pair(r, c))
                }
            }
        }

        // Shuffle deterministically based on level number
        val rng = kotlin.random.Random(levelNum * 9973L)
        val available = playableCoords.shuffled(rng).toMutableList()

        // Equal and generous distribution of Diamonds (Pink Gems), Stars, and Emeralds
        val totalCells = available.size
        if (totalCells > 0) {
            val hasEmeralds = target.emeraldsRequired > 0
            val emeraldShare = if (hasEmeralds) (totalCells * 0.2f).toInt().coerceAtLeast(1) else 0
            val remainingCells = totalCells - emeraldShare
            // Give Diamonds/Pink Gems 50% of the cells
            val gemShare = (remainingCells / 2).coerceAtLeast(1)
            val starShare = (remainingCells - gemShare).coerceAtLeast(1)

            // 1. Place Pink Diamonds / Gems
            val gemsToPlace = gemShare.coerceAtMost(available.size)
            for (i in 0 until gemsToPlace) {
                if (available.isNotEmpty()) {
                    val (r, c) = available.removeAt(0)
                    grid[r][c] = grid[r][c].copy(specialItem = SPECIAL_GEM_PINK)
                }
            }

            // 2. Place Yellow Stars
            val starsToPlace = starShare.coerceAtMost(available.size)
            for (i in 0 until starsToPlace) {
                if (available.isNotEmpty()) {
                    val (r, c) = available.removeAt(0)
                    grid[r][c] = grid[r][c].copy(specialItem = SPECIAL_STAR_YELLOW)
                }
            }

            // 3. Place Emeralds
            if (hasEmeralds) {
                val emeraldsToPlace = emeraldShare.coerceAtMost(available.size)
                for (i in 0 until emeraldsToPlace) {
                    if (available.isNotEmpty()) {
                        val (r, c) = available.removeAt(0)
                        grid[r][c] = grid[r][c].copy(specialItem = SPECIAL_EMERALD_GREEN)
                    }
                }
            }
        }

        return BoardState(grid.map { it.toList() })
    }
}
