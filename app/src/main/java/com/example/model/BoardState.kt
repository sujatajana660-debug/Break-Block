package com.example.model

const val BOARD_SIZE = 8

data class ClearResult(
    val clearedRows: List<Int>,
    val clearedCols: List<Int>,
    val collectedGems: Int,
    val collectedStars: Int,
    val collectedEmeralds: Int,
    val totalLines: Int,
    val pointsEarned: Int
)

data class BoardState(
    val grid: List<List<CellData>> = List(BOARD_SIZE) {
        List(BOARD_SIZE) { CellData() }
    }
) {
    fun getCell(row: Int, col: Int): CellData {
        if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE) {
            return grid[row][col]
        }
        return CellData()
    }

    fun isOccupied(row: Int, col: Int): Boolean {
        return getCell(row, col).blockType != BlockType.EMPTY
    }

    val occupancyRatio: Float
        get() {
            var occupied = 0
            for (r in 0 until BOARD_SIZE) {
                for (c in 0 until BOARD_SIZE) {
                    if (grid[r][c].blockType != BlockType.EMPTY) occupied++
                }
            }
            return occupied.toFloat() / (BOARD_SIZE * BOARD_SIZE)
        }

    fun canPlacePiece(piece: PolyominoPiece, originRow: Int, originCol: Int): Boolean {
        for (cell in piece.cells) {
            val r = originRow + cell.row
            val c = originCol + cell.col
            if (r !in 0 until BOARD_SIZE || c !in 0 until BOARD_SIZE) return false
            if (grid[r][c].blockType != BlockType.EMPTY) return false
        }
        return true
    }

    fun hasAnyLegalPlacement(piece: PolyominoPiece): Boolean {
        for (r in 0..BOARD_SIZE - piece.height) {
            for (c in 0..BOARD_SIZE - piece.width) {
                if (canPlacePiece(piece, r, c)) return true
            }
        }
        return false
    }

    // Returns a copy of the board with the piece placed
    fun placePiece(piece: PolyominoPiece, originRow: Int, originCol: Int): BoardState {
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        for (cell in piece.cells) {
            val r = originRow + cell.row
            val c = originCol + cell.col
            if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE) {
                val existing = newGrid[r][c]
                val special = piece.specialItems[cell] ?: existing.specialItem
                newGrid[r][c] = CellData(
                    blockType = piece.blockType,
                    specialItem = special
                )
            }
        }
        return BoardState(newGrid.map { it.toList() })
    }

    fun setSpecialItem(row: Int, col: Int, specialItem: Int): BoardState {
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        if (row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE) {
            newGrid[row][col] = newGrid[row][col].copy(specialItem = specialItem)
        }
        return BoardState(newGrid.map { it.toList() })
    }

    // Checks lines to clear and returns lines and collected target items
    fun detectClears(): ClearResult {
        val fullRows = mutableListOf<Int>()
        val fullCols = mutableListOf<Int>()

        for (r in 0 until BOARD_SIZE) {
            val isFull = (0 until BOARD_SIZE).all { c -> grid[r][c].blockType != BlockType.EMPTY }
            if (isFull) fullRows.add(r)
        }

        for (c in 0 until BOARD_SIZE) {
            val isFull = (0 until BOARD_SIZE).all { r -> grid[r][c].blockType != BlockType.EMPTY }
            if (isFull) fullCols.add(c)
        }

        var gems = 0
        var stars = 0
        var emeralds = 0

        // Count special targets inside cleared cells
        val clearedCoordinates = mutableSetOf<Pair<Int, Int>>()
        for (r in fullRows) {
            for (c in 0 until BOARD_SIZE) clearedCoordinates.add(Pair(r, c))
        }
        for (c in fullCols) {
            for (r in 0 until BOARD_SIZE) clearedCoordinates.add(Pair(r, c))
        }

        for ((r, c) in clearedCoordinates) {
            when (grid[r][c].specialItem) {
                SPECIAL_GEM_PINK -> gems++
                SPECIAL_STAR_YELLOW -> stars++
                SPECIAL_EMERALD_GREEN -> emeralds++
            }
        }

        val totalLines = fullRows.size + fullCols.size
        val points = when (totalLines) {
            0 -> 0
            1 -> 10
            2 -> 30
            3 -> 60
            4 -> 100
            5 -> 160
            6 -> 240
            else -> totalLines * 50
        }

        return ClearResult(
            clearedRows = fullRows,
            clearedCols = fullCols,
            collectedGems = gems,
            collectedStars = stars,
            collectedEmeralds = emeralds,
            totalLines = totalLines,
            pointsEarned = points
        )
    }

    // Execute clear and return new board
    fun executeClear(result: ClearResult): BoardState {
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        for (r in result.clearedRows) {
            for (c in 0 until BOARD_SIZE) {
                newGrid[r][c] = CellData(blockType = BlockType.EMPTY, specialItem = SPECIAL_NONE)
            }
        }
        for (c in result.clearedCols) {
            for (r in 0 until BOARD_SIZE) {
                newGrid[r][c] = CellData(blockType = BlockType.EMPTY, specialItem = SPECIAL_NONE)
            }
        }
        return BoardState(newGrid.map { it.toList() })
    }

    // Set preview ghost cells
    fun withGhost(piece: PolyominoPiece?, originRow: Int, originCol: Int): BoardState {
        if (piece == null || !canPlacePiece(piece, originRow, originCol)) return this
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        for (cell in piece.cells) {
            val r = originRow + cell.row
            val c = originCol + cell.col
            if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE) {
                newGrid[r][c] = newGrid[r][c].copy(
                    blockType = piece.blockType,
                    isGhost = true
                )
            }
        }
        return BoardState(newGrid.map { it.toList() })
    }

    // BOOSTER 1: Boom 4x4 Block Breaker
    fun executeBomb(centerRow: Int, centerCol: Int): Pair<BoardState, ClearResult> {
        val startR = (centerRow - 1).coerceIn(0, BOARD_SIZE - 4)
        val startC = (centerCol - 1).coerceIn(0, BOARD_SIZE - 4)
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        var gems = 0
        var stars = 0
        var emeralds = 0
        var clearedCount = 0

        for (r in startR until startR + 4) {
            for (c in startC until startC + 4) {
                val cell = newGrid[r][c]
                if (cell.blockType != BlockType.EMPTY) {
                    clearedCount++
                    when (cell.specialItem) {
                        SPECIAL_GEM_PINK -> gems++
                        SPECIAL_STAR_YELLOW -> stars++
                        SPECIAL_EMERALD_GREEN -> emeralds++
                    }
                    newGrid[r][c] = CellData(blockType = BlockType.EMPTY, specialItem = SPECIAL_NONE)
                }
            }
        }

        val result = ClearResult(
            clearedRows = emptyList(),
            clearedCols = emptyList(),
            collectedGems = gems,
            collectedStars = stars,
            collectedEmeralds = emeralds,
            totalLines = (clearedCount / 4).coerceAtLeast(1),
            pointsEarned = clearedCount * 15
        )
        return Pair(BoardState(newGrid.map { it.toList() }), result)
    }

    // BOOSTER 2: Line Breaker (clears entire row)
    fun executeLineBreaker(targetRow: Int): Pair<BoardState, ClearResult> {
        val validRow = targetRow.coerceIn(0, BOARD_SIZE - 1)
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        var gems = 0
        var stars = 0
        var emeralds = 0
        var clearedCount = 0

        for (c in 0 until BOARD_SIZE) {
            val cell = newGrid[validRow][c]
            if (cell.blockType != BlockType.EMPTY) {
                clearedCount++
                when (cell.specialItem) {
                    SPECIAL_GEM_PINK -> gems++
                    SPECIAL_STAR_YELLOW -> stars++
                    SPECIAL_EMERALD_GREEN -> emeralds++
                }
                newGrid[validRow][c] = CellData(blockType = BlockType.EMPTY, specialItem = SPECIAL_NONE)
            }
        }

        val result = ClearResult(
            clearedRows = listOf(validRow),
            clearedCols = emptyList(),
            collectedGems = gems,
            collectedStars = stars,
            collectedEmeralds = emeralds,
            totalLines = 1,
            pointsEarned = maxOf(clearedCount * 12, 40)
        )
        return Pair(BoardState(newGrid.map { it.toList() }), result)
    }

    // BOOSTER 3: + Cross Breaker (Breaks blocks in all 4 directions: full row AND full column)
    fun executePlusBreaker(centerRow: Int, centerCol: Int): Pair<BoardState, ClearResult> {
        val validRow = centerRow.coerceIn(0, BOARD_SIZE - 1)
        val validCol = centerCol.coerceIn(0, BOARD_SIZE - 1)
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        var gems = 0
        var stars = 0
        var emeralds = 0
        var clearedCount = 0

        // Clear row
        for (c in 0 until BOARD_SIZE) {
            val cell = newGrid[validRow][c]
            if (cell.blockType != BlockType.EMPTY) {
                clearedCount++
                when (cell.specialItem) {
                    SPECIAL_GEM_PINK -> gems++
                    SPECIAL_STAR_YELLOW -> stars++
                    SPECIAL_EMERALD_GREEN -> emeralds++
                }
                newGrid[validRow][c] = CellData(blockType = BlockType.EMPTY, specialItem = SPECIAL_NONE)
            }
        }

        // Clear column
        for (r in 0 until BOARD_SIZE) {
            val cell = newGrid[r][validCol]
            if (cell.blockType != BlockType.EMPTY) {
                clearedCount++
                when (cell.specialItem) {
                    SPECIAL_GEM_PINK -> gems++
                    SPECIAL_STAR_YELLOW -> stars++
                    SPECIAL_EMERALD_GREEN -> emeralds++
                }
                newGrid[r][validCol] = CellData(blockType = BlockType.EMPTY, specialItem = SPECIAL_NONE)
            }
        }

        val result = ClearResult(
            clearedRows = listOf(validRow),
            clearedCols = listOf(validCol),
            collectedGems = gems,
            collectedStars = stars,
            collectedEmeralds = emeralds,
            totalLines = 2,
            pointsEarned = maxOf(clearedCount * 15, 80)
        )
        return Pair(BoardState(newGrid.map { it.toList() }), result)
    }

    // Revive Board: Clears the center 4x4 area to give ample room on continue
    fun executeRevive(): BoardState {
        val newGrid = grid.map { it.toMutableList() }.toMutableList()
        for (r in 2..5) {
            for (c in 2..5) {
                newGrid[r][c] = CellData(blockType = BlockType.EMPTY, specialItem = SPECIAL_NONE)
            }
        }
        return BoardState(newGrid.map { it.toList() })
    }
}
