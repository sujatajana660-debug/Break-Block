package com.example.model

import androidx.compose.ui.graphics.Color

enum class BlockType(
    val id: Int,
    val primaryColor: Color,
    val lightColor: Color,
    val darkColor: Color,
    val displayName: String
) {
    EMPTY(0, Color.Transparent, Color.Transparent, Color.Transparent, "Empty"),
    RED(1, Color(0xFFEF4444), Color(0xFFFCA5A5), Color(0xFFB91C1C), "Red"),
    ORANGE(2, Color(0xFFF97316), Color(0xFFFDBA74), Color(0xFFC2410C), "Orange"),
    YELLOW(3, Color(0xFFEAB308), Color(0xFFFDE047), Color(0xFFA16207), "Yellow"),
    GREEN(4, Color(0xFF22C55E), Color(0xFF86EFAC), Color(0xFF15803D), "Green"),
    CYAN(5, Color(0xFF06B6D4), Color(0xFF67E8F9), Color(0xFF0E7490), "Cyan"),
    BLUE(6, Color(0xFF3B82F6), Color(0xFF93C5FD), Color(0xFF1D4ED8), "Blue"),
    PURPLE(7, Color(0xFF8B5CF6), Color(0xFFC4B5FD), Color(0xFF6D28D9), "Purple"),
    PINK(8, Color(0xFFEC4899), Color(0xFFF472B6), Color(0xFFBE185D), "Pink"),
    STONE(9, Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFF334155), "Stone Block");

    companion object {
        fun fromId(id: Int): BlockType {
            val baseId = id % 100 // Handle special gem flags
            return entries.firstOrNull { it.id == baseId } ?: EMPTY
        }

        // Random playable color
        fun randomPlayable(): BlockType {
            val playables = listOf(RED, ORANGE, YELLOW, GREEN, CYAN, BLUE, PURPLE, PINK)
            return playables.random()
        }
    }
}

// Special overlay tokens for Adventure Mode
const val SPECIAL_NONE = 0
const val SPECIAL_GEM_PINK = 1
const val SPECIAL_STAR_YELLOW = 2
const val SPECIAL_EMERALD_GREEN = 3

data class CellData(
    val blockType: BlockType = BlockType.EMPTY,
    val specialItem: Int = SPECIAL_NONE,
    val isClearing: Boolean = false,
    val isGhost: Boolean = false
)
