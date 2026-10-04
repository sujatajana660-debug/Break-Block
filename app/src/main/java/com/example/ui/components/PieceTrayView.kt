package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.BoardState
import com.example.model.PolyominoPiece
import com.example.viewmodel.DragState
import kotlin.math.roundToInt

@Composable
fun PieceTrayView(
    pieces: List<PolyominoPiece?>,
    boardState: BoardState,
    dragState: DragState,
    onDragStart: (Int, PolyominoPiece, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        pieces.forEachIndexed { index, piece ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (piece != null) {
                    val isBeingDragged = dragState.pieceIndex == index
                    val canFit = boardState.hasAnyLegalPlacement(piece)
                    PieceThumbnail(
                        piece = piece,
                        alpha = if (isBeingDragged) 0.15f else if (!canFit) 0.45f else 1f,
                        onDragStart = { startOffset ->
                            onDragStart(index, piece, startOffset)
                        },
                        onDragMove = onDragMove,
                        onDragEnd = onDragEnd,
                        testTag = "tray_piece_$index"
                    )
                }
            }
        }
    }
}

@Composable
fun PieceThumbnail(
    piece: PolyominoPiece,
    alpha: Float,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var globalBounds by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = modifier
            .size(90.dp)
            .testTag(testTag)
            .onGloballyPositioned { coords ->
                globalBounds = coords.boundsInRoot()
            }
            .pointerInput(piece.id) {
                detectDragGestures(
                    onDragStart = { touchOffset ->
                        val rootStart = Offset(
                            globalBounds.left + touchOffset.x,
                            globalBounds.top + touchOffset.y
                        )
                        onDragStart(rootStart)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onDragMove(change.position + globalBounds.topLeft)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxDimension = maxOf(piece.width, piece.height, 3)
            val miniCellSize = (size.minDimension * 0.85f) / maxDimension
            val blockPadding = miniCellSize * 0.08f
            val blockSize = miniCellSize - (blockPadding * 2)

            val pieceWidthPx = piece.width * miniCellSize
            val pieceHeightPx = piece.height * miniCellSize
            val startX = (size.width - pieceWidthPx) / 2
            val startY = (size.height - pieceHeightPx) / 2

            for (cell in piece.cells) {
                val left = startX + (cell.col * miniCellSize) + blockPadding
                val top = startY + (cell.row * miniCellSize) + blockPadding
                drawCandyBlock(
                    type = piece.blockType,
                    topLeft = Offset(left, top),
                    size = blockSize,
                    specialItem = piece.specialItems[cell] ?: 0,
                    alpha = alpha
                )
            }
        }
    }
}

// Dragged piece hovering under player's finger
@Composable
fun DraggedPieceOverlay(
    dragState: DragState,
    cellSizePx: Float
) {
    val piece = dragState.piece ?: return
    val density = LocalDensity.current

    // Shift piece upward by 48dp so player finger doesn't block the piece
    val visualYOffsetPx = with(density) { 48.dp.toPx() }
    val pieceWidthPx = piece.width * cellSizePx
    val pieceHeightPx = piece.height * cellSizePx

    val topLeftX = dragState.currentPosition.x - (pieceWidthPx / 2)
    val topLeftY = dragState.currentPosition.y - visualYOffsetPx - (pieceHeightPx / 2)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier
                .offset { IntOffset(topLeftX.roundToInt(), topLeftY.roundToInt()) }
                .size(
                    width = with(density) { pieceWidthPx.toDp() },
                    height = with(density) { pieceHeightPx.toDp() }
                )
        ) {
            val blockPadding = cellSizePx * 0.05f
            val blockSize = cellSizePx - (blockPadding * 2)

            for (cell in piece.cells) {
                val left = (cell.col * cellSizePx) + blockPadding
                val top = (cell.row * cellSizePx) + blockPadding
                drawCandyBlock(
                    type = piece.blockType,
                    topLeft = Offset(left, top),
                    size = blockSize,
                    specialItem = piece.specialItems[cell] ?: 0,
                    alpha = 0.95f
                )
            }
        }
    }
}
