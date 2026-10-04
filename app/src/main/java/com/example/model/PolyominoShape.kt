package com.example.model

import java.util.UUID

data class ShapeCell(val row: Int, val col: Int)

data class PolyominoPiece(
    val id: String = UUID.randomUUID().toString(),
    val cells: List<ShapeCell>,
    val blockType: BlockType,
    val width: Int,
    val height: Int,
    val specialItems: Map<ShapeCell, Int> = emptyMap()
) {
    val cellCount: Int get() = cells.size
}

object PolyominoCatalog {
    // 1-Block
    val DOT = listOf(ShapeCell(0, 0))

    // 2-Blocks
    val BAR_2_H = listOf(ShapeCell(0, 0), ShapeCell(0, 1))
    val BAR_2_V = listOf(ShapeCell(0, 0), ShapeCell(1, 0))

    // 3-Blocks
    val BAR_3_H = listOf(ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(0, 2))
    val BAR_3_V = listOf(ShapeCell(0, 0), ShapeCell(1, 0), ShapeCell(2, 0))
    val CORNER_3_TL = listOf(ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(1, 0))
    val CORNER_3_TR = listOf(ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(1, 1))
    val CORNER_3_BL = listOf(ShapeCell(0, 0), ShapeCell(1, 0), ShapeCell(1, 1))
    val CORNER_3_BR = listOf(ShapeCell(0, 1), ShapeCell(1, 0), ShapeCell(1, 1))

    // 4-Blocks
    val SQUARE_2X2 = listOf(ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(1, 0), ShapeCell(1, 1))
    val BAR_4_H = listOf(ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(0, 2), ShapeCell(0, 3))
    val BAR_4_V = listOf(ShapeCell(0, 0), ShapeCell(1, 0), ShapeCell(2, 0), ShapeCell(3, 0))
    val T_SHAPE_DOWN = listOf(ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(0, 2), ShapeCell(1, 1))
    val T_SHAPE_UP = listOf(ShapeCell(0, 1), ShapeCell(1, 0), ShapeCell(1, 1), ShapeCell(1, 2))
    val T_SHAPE_RIGHT = listOf(ShapeCell(0, 0), ShapeCell(1, 0), ShapeCell(2, 0), ShapeCell(1, 1))
    val T_SHAPE_LEFT = listOf(ShapeCell(0, 1), ShapeCell(1, 1), ShapeCell(2, 1), ShapeCell(1, 0))
    val L_SHAPE_1 = listOf(ShapeCell(0, 0), ShapeCell(1, 0), ShapeCell(2, 0), ShapeCell(2, 1))
    val L_SHAPE_2 = listOf(ShapeCell(0, 1), ShapeCell(1, 1), ShapeCell(2, 1), ShapeCell(2, 0))
    val L_SHAPE_3 = listOf(ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(0, 2), ShapeCell(1, 0))
    val L_SHAPE_4 = listOf(ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(0, 2), ShapeCell(1, 2))
    val Z_SHAPE_H = listOf(ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(1, 1), ShapeCell(1, 2))
    val S_SHAPE_H = listOf(ShapeCell(0, 1), ShapeCell(0, 2), ShapeCell(1, 0), ShapeCell(1, 1))

    // 5-Blocks (Screenshot 2: Long 1x5 cyan bar, big 3x3 L corner, 3x3 square)
    val BAR_5_H = listOf(ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(0, 2), ShapeCell(0, 3), ShapeCell(0, 4))
    val BAR_5_V = listOf(ShapeCell(0, 0), ShapeCell(1, 0), ShapeCell(2, 0), ShapeCell(3, 0), ShapeCell(4, 0))
    val BIG_CORNER_5_TL = listOf(ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(0, 2), ShapeCell(1, 0), ShapeCell(2, 0))
    val BIG_CORNER_5_BR = listOf(ShapeCell(2, 0), ShapeCell(2, 1), ShapeCell(2, 2), ShapeCell(0, 2), ShapeCell(1, 2))
    val BIG_SQUARE_3X3 = listOf(
        ShapeCell(0, 0), ShapeCell(0, 1), ShapeCell(0, 2),
        ShapeCell(1, 0), ShapeCell(1, 1), ShapeCell(1, 2),
        ShapeCell(2, 0), ShapeCell(2, 1), ShapeCell(2, 2)
    )

    val ALL_SHAPES = listOf(
        DOT,
        BAR_2_H, BAR_2_V,
        BAR_3_H, BAR_3_V, CORNER_3_TL, CORNER_3_TR, CORNER_3_BL, CORNER_3_BR,
        SQUARE_2X2,
        BAR_4_H, BAR_4_V,
        T_SHAPE_DOWN, T_SHAPE_UP, T_SHAPE_RIGHT, T_SHAPE_LEFT,
        L_SHAPE_1, L_SHAPE_2, L_SHAPE_3, L_SHAPE_4,
        Z_SHAPE_H, S_SHAPE_H,
        BAR_5_H, BAR_5_V,
        BIG_CORNER_5_TL, BIG_CORNER_5_BR,
        BIG_SQUARE_3X3
    )

    fun createPiece(cells: List<ShapeCell>, forcedColor: BlockType? = null): PolyominoPiece {
        val maxR = cells.maxOf { it.row }
        val minR = cells.minOf { it.row }
        val maxC = cells.maxOf { it.col }
        val minC = cells.minOf { it.col }
        // Normalize coordinates to start from (0,0)
        val normalized = cells.map { ShapeCell(it.row - minR, it.col - minC) }
        val width = maxC - minC + 1
        val height = maxR - minR + 1

        val color = forcedColor ?: when (normalized.size) {
            1 -> BlockType.YELLOW
            2 -> BlockType.GREEN
            3 -> if (width == 1 || height == 1) BlockType.ORANGE else BlockType.PINK
            4 -> when {
                width == 2 && height == 2 -> BlockType.YELLOW
                width == 4 || height == 4 -> BlockType.CYAN
                width == 3 && height == 2 -> BlockType.PURPLE
                else -> BlockType.BLUE
            }
            5 -> if (width == 5 || height == 5) BlockType.CYAN else BlockType.RED
            9 -> BlockType.ORANGE
            else -> BlockType.randomPlayable()
        }

        return PolyominoPiece(
            cells = normalized,
            blockType = color,
            width = width,
            height = height
        )
    }

    // Generate batch of 3 pieces with fairness guarantee and optional adventure target item
    fun generateBatch(
        boardOccupancyRatio: Float,
        ensurePlaceableAgainst: ((PolyominoPiece) -> Boolean)? = null,
        adventureSpecialItem: Int? = null
    ): List<PolyominoPiece> {
        val pieces = mutableListOf<PolyominoPiece>()
        // Weight distribution adjusted for board fullness
        val pool = when {
            boardOccupancyRatio > 0.75f -> listOf(
                DOT, BAR_2_H, BAR_2_V, BAR_3_H, BAR_3_V,
                CORNER_3_TL, CORNER_3_TR, CORNER_3_BL, CORNER_3_BR,
                SQUARE_2X2
            )
            boardOccupancyRatio > 0.50f -> ALL_SHAPES.filter { it.size <= 5 }
            else -> ALL_SHAPES
        }

        for (i in 0 until 3) {
            val rawShape = pool.random()
            pieces.add(createPiece(rawShape))
        }

        // Guarantee at least one piece is placeable if checker is provided
        if (ensurePlaceableAgainst != null) {
            val hasPlaceable = pieces.any { ensurePlaceableAgainst(it) }
            if (!hasPlaceable) {
                // Replace first piece with an easy piece or DOT that can fit
                val safeCandidate = listOf(DOT, BAR_2_H, BAR_2_V, BAR_3_H, SQUARE_2X2)
                    .map { createPiece(it) }
                    .firstOrNull { ensurePlaceableAgainst(it) } ?: createPiece(DOT)
                pieces[0] = safeCandidate
            }
        }

        // In Adventure Mode: if player still needs stars or gems, embed the target item on one piece!
        if (adventureSpecialItem != null && adventureSpecialItem != SPECIAL_NONE && pieces.isNotEmpty()) {
            val targetIdx = (0 until pieces.size).random()
            val targetPiece = pieces[targetIdx]
            val targetCell = targetPiece.cells.random()
            pieces[targetIdx] = targetPiece.copy(
                specialItems = mapOf(targetCell to adventureSpecialItem)
            )
        }

        return pieces
    }
}
