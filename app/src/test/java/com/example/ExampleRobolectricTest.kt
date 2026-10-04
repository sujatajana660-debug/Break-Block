package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.GameStatsEntity
import com.example.model.AdventureCatalog
import com.example.model.BOARD_SIZE
import com.example.model.BlockType
import com.example.model.BoardState
import com.example.model.PolyominoCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Break Blocks", appName)
    }

    @Test
    fun `board state piece placement and line clear`() {
        var board = BoardState()
        val bar4 = PolyominoCatalog.createPiece(PolyominoCatalog.BAR_4_H, BlockType.CYAN)
        // Place two 1x4 bars to fill row 0
        assertTrue(board.canPlacePiece(bar4, 0, 0))
        board = board.placePiece(bar4, 0, 0)
        assertTrue(board.canPlacePiece(bar4, 0, 4))
        board = board.placePiece(bar4, 0, 4)

        // Detect clears
        val clearResult = board.detectClears()
        assertEquals(1, clearResult.clearedRows.size)
        assertEquals(0, clearResult.clearedRows[0])
        assertEquals(1, clearResult.totalLines)

        // Execute clear
        val boardCleared = board.executeClear(clearResult)
        for (c in 0 until BOARD_SIZE) {
            assertEquals(BlockType.EMPTY, boardCleared.getCell(0, c).blockType)
        }
    }

    @Test
    fun `booster bomb 4x4 clear test`() {
        var board = BoardState()
        val bar4 = PolyominoCatalog.createPiece(PolyominoCatalog.BAR_4_H, BlockType.RED)
        board = board.placePiece(bar4, 2, 2)
        assertEquals(BlockType.RED, board.getCell(2, 2).blockType)

        // Blast 4x4 area centered around (2, 2)
        val (clearedBoard, result) = board.executeBomb(2, 2)
        assertEquals(BlockType.EMPTY, clearedBoard.getCell(2, 2).blockType)
        assertTrue(result.pointsEarned > 0)
    }

    @Test
    fun `booster line and plus breaker test`() {
        var board = BoardState()
        val bar4 = PolyominoCatalog.createPiece(PolyominoCatalog.BAR_4_H, BlockType.YELLOW)
        board = board.placePiece(bar4, 3, 0)

        // Line breaker on row 3
        val (afterLine, _) = board.executeLineBreaker(3)
        assertEquals(BlockType.EMPTY, afterLine.getCell(3, 0).blockType)

        // Plus breaker on (4, 4)
        val (afterPlus, plusRes) = board.executePlusBreaker(4, 4)
        assertEquals(2, plusRes.totalLines)
    }

    @Test
    fun `hard level generation after level 50`() {
        val level50 = AdventureCatalog.getLevel(50)
        assertFalse(level50.isHardLevel)
        val level51 = AdventureCatalog.getLevel(51)
        assertTrue(level51.isHardLevel)
        assertTrue(level51.name.contains("[HARD]"))

        // Check stone obstacles present in level 51+
        var stoneFound = false
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (level51.initialBoard.getCell(r, c).blockType == BlockType.STONE) {
                    stoneFound = true
                    break
                }
            }
        }
        assertTrue(stoneFound)
    }

    @Test
    fun `default stats has 5 of each booster and guest profile`() {
        val defaultStats = GameStatsEntity()
        assertEquals(5, defaultStats.bombCount)
        assertEquals(5, defaultStats.lineBreakerCount)
        assertEquals(5, defaultStats.plusBreakerCount)
        assertEquals(5, defaultStats.undoCount)
        assertTrue(defaultStats.guestId.startsWith("GST-"))
        assertEquals("Guest Player", defaultStats.playerName)
        assertEquals(3, defaultStats.unlockedGateLevel)
    }

    @Test
    fun `board executeRevive clears center area`() {
        var board = BoardState()
        for (r in 2..5) {
            for (c in 2..5) {
                board = board.placePiece(
                    PolyominoCatalog.createPiece(PolyominoCatalog.DOT, BlockType.PURPLE),
                    r,
                    c
                )
            }
        }
        val revived = board.executeRevive()
        for (r in 2..5) {
            for (c in 2..5) {
                assertEquals(BlockType.EMPTY, revived.getCell(r, c).blockType)
            }
        }
    }

    @Test
    fun `launch MainActivity test`() {
        val scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        assertNotNull(scenario)
    }
}
