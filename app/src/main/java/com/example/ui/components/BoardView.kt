package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.BOARD_SIZE
import com.example.model.BlockType
import com.example.model.BoardState
import com.example.viewmodel.DragState
import com.example.model.SPECIAL_EMERALD_GREEN
import com.example.model.SPECIAL_GEM_PINK
import com.example.model.SPECIAL_STAR_YELLOW
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class BlastParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var size: Float,
    var alpha: Float = 1f,
    var isStar: Boolean = false
)

data class ShockwaveRing(
    var x: Float,
    var y: Float,
    var radius: Float,
    val maxRadius: Float,
    val color: Color,
    var alpha: Float = 0.95f
)

data class LineLaserFlash(
    val isRow: Boolean,
    val index: Int,
    var alpha: Float = 1f
)

@Composable
fun BoardView(
    boardState: BoardState,
    dragState: DragState,
    onBoundsMeasured: (Rect, Float) -> Unit,
    modifier: Modifier = Modifier,
    onCellClick: ((row: Int, col: Int) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val shakeAnim = remember { Animatable(0f) }

    // Pulsing glowing edge on ghost pieces
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val ghostPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ghostPulse"
    )

    // Explosive line clear particles, shockwaves & laser sweeps
    val particles = remember { mutableStateListOf<BlastParticle>() }
    val shockwaves = remember { mutableStateListOf<ShockwaveRing>() }
    val laserFlashes = remember { mutableStateListOf<LineLaserFlash>() }
    var previousBoard by remember { mutableStateOf(boardState) }

    LaunchedEffect(boardState) {
        val prev = previousBoard
        if (prev !== boardState) {
            var clearedCount = 0
            var sumX = 0f
            var sumY = 0f

            // Check cells that were filled and became empty (cleared!)
            for (r in 0 until BOARD_SIZE) {
                for (c in 0 until BOARD_SIZE) {
                    val prevCell = prev.getCell(r, c)
                    val currCell = boardState.getCell(r, c)
                    if (prevCell.blockType != BlockType.EMPTY && currCell.blockType == BlockType.EMPTY) {
                        clearedCount++
                        sumX += c
                        sumY += r
                        val baseColor = prevCell.blockType.primaryColor
                        // Spawn 10 energetic particles per cleared tile
                        for (i in 0 until 10) {
                            val angle = (kotlin.random.Random.nextFloat() * 2 * PI).toFloat()
                            val speed = (kotlin.random.Random.nextFloat() * 16f + 5f)
                            particles.add(
                                BlastParticle(
                                    x = c.toFloat(),
                                    y = r.toFloat(),
                                    vx = cos(angle) * speed,
                                    vy = sin(angle) * speed,
                                    color = if (i % 2 == 0) baseColor else Color(0xFFFFD700),
                                    size = kotlin.random.Random.nextFloat() * 11f + 5f,
                                    alpha = 1f,
                                    isStar = i % 3 == 0
                                )
                            )
                        }
                    }
                }
            }

            // If a blast or clear happened: Trigger screen shake & shockwaves!
            if (clearedCount > 0) {
                val avgX = sumX / clearedCount
                val avgY = sumY / clearedCount
                shockwaves.add(
                    ShockwaveRing(
                        x = avgX,
                        y = avgY,
                        radius = 10f,
                        maxRadius = 320f,
                        color = Color(0xFFFFD700),
                        alpha = 0.95f
                    )
                )

                // Screen shake animation
                scope.launch {
                    shakeAnim.animateTo(1f, tween(40))
                    shakeAnim.animateTo(-0.75f, tween(45))
                    shakeAnim.animateTo(0.5f, tween(40))
                    shakeAnim.animateTo(-0.25f, tween(35))
                    shakeAnim.animateTo(0f, tween(30))
                }
            }
            previousBoard = boardState
        }
    }

    // Animation physics loop for particles and shockwaves
    LaunchedEffect(particles.isNotEmpty() || shockwaves.isNotEmpty()) {
        while (particles.isNotEmpty() || shockwaves.isNotEmpty()) {
            withFrameNanos { _ ->
                // Update particles
                val itP = particles.listIterator()
                while (itP.hasNext()) {
                    val p = itP.next()
                    p.x += p.vx * 0.05f
                    p.y += p.vy * 0.05f
                    p.vy += 0.38f // gravity
                    p.alpha -= 0.032f
                    p.size *= 0.965f
                    if (p.alpha <= 0.04f || p.size <= 1.5f) {
                        itP.remove()
                    }
                }

                // Update shockwaves
                val itS = shockwaves.listIterator()
                while (itS.hasNext()) {
                    val sw = itS.next()
                    sw.radius += 18f
                    sw.alpha -= 0.055f
                    if (sw.alpha <= 0.05f || sw.radius >= sw.maxRadius) {
                        itS.remove()
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .offset(x = (shakeAnim.value * 7).dp, y = (shakeAnim.value * 4).dp)
            .padding(12.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInRoot()
                val cellSize = bounds.width / BOARD_SIZE
                onBoundsMeasured(bounds, cellSize)
            }
            .pointerInput(onCellClick) {
                detectTapGestures { offset ->
                    val cellSize = size.width.toFloat() / BOARD_SIZE
                    val col = (offset.x / cellSize).toInt().coerceIn(0, BOARD_SIZE - 1)
                    val row = (offset.y / cellSize).toInt().coerceIn(0, BOARD_SIZE - 1)
                    onCellClick?.invoke(row, col)
                }
            }
            .testTag("game_board")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boardW = size.width
            val cellSize = boardW / BOARD_SIZE
            val blockPadding = cellSize * 0.05f
            val blockSize = cellSize - (blockPadding * 2)

            // 1. Draw Board Background
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF13192B), Color(0xFF0D1220))
                )
            )

            // 2. Draw Grid Slots (Empty Inset Cells)
            for (r in 0 until BOARD_SIZE) {
                for (c in 0 until BOARD_SIZE) {
                    val cellLeft = c * cellSize + blockPadding
                    val cellTop = r * cellSize + blockPadding

                    // Inset dark tile
                    drawRoundRect(
                        color = Color(0xFF1E2638).copy(alpha = 0.85f),
                        topLeft = Offset(cellLeft, cellTop),
                        size = Size(blockSize, blockSize),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    // Subtle inner border
                    drawRoundRect(
                        color = Color(0xFF2B354C).copy(alpha = 0.5f),
                        topLeft = Offset(cellLeft, cellTop),
                        size = Size(blockSize, blockSize),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // 3. Draw Placed Blocks
            for (r in 0 until BOARD_SIZE) {
                for (c in 0 until BOARD_SIZE) {
                    val cell = boardState.getCell(r, c)
                    if (cell.blockType != BlockType.EMPTY) {
                        val cellLeft = c * cellSize + blockPadding
                        val cellTop = r * cellSize + blockPadding
                        drawCandyBlock(
                            type = cell.blockType,
                            topLeft = Offset(cellLeft, cellTop),
                            size = blockSize,
                            specialItem = cell.specialItem
                        )
                    }
                }
            }

            // 4. Draw Ghost Piece Preview with Pulsing Glow Animation
            val piece = dragState.piece
            val origin = dragState.hoveredOrigin
            if (piece != null && origin != null && dragState.isValidPlacement) {
                val (originRow, originCol) = origin
                for (shapeCell in piece.cells) {
                    val r = originRow + shapeCell.row
                    val c = originCol + shapeCell.col
                    if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE) {
                        val cellLeft = c * cellSize + blockPadding
                        val cellTop = r * cellSize + blockPadding

                        // Soft translucent ghost with pulsing alpha
                        drawRoundRect(
                            color = piece.blockType.primaryColor.copy(alpha = 0.45f * ghostPulseAlpha),
                            topLeft = Offset(cellLeft, cellTop),
                            size = Size(blockSize, blockSize),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                        // White glowing edge
                        drawRoundRect(
                            color = Color.White.copy(alpha = ghostPulseAlpha),
                            topLeft = Offset(cellLeft, cellTop),
                            size = Size(blockSize, blockSize),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            style = Stroke(width = 2.2.dp.toPx())
                        )
                    }
                }
            }

            // 5. Draw Expanding Shockwave Rings
            for (sw in shockwaves) {
                val cx = sw.x * cellSize + (cellSize / 2)
                val cy = sw.y * cellSize + (cellSize / 2)
                drawCircle(
                    color = sw.color.copy(alpha = sw.alpha.coerceIn(0f, 1f)),
                    radius = sw.radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.5.dp.toPx())
                )
            }

            // 6. Draw Explosive Line Clear & Blast Particles
            for (p in particles) {
                val px = p.x * cellSize + (cellSize / 2)
                val py = p.y * cellSize + (cellSize / 2)
                if (p.isStar) {
                    drawYellowStar(Offset(px, py), p.size)
                } else {
                    drawCircle(
                        color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                        radius = p.size,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}

// Hyper-realistic 3D Arcade Jewel Block renderer
fun DrawScope.drawCandyBlock(
    type: BlockType,
    topLeft: Offset,
    size: Float,
    specialItem: Int = 0,
    alpha: Float = 1f
) {
    val cr = CornerRadius(size * 0.22f, size * 0.22f)

    // 1. Soft Ambient Drop Shadow under each block
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.45f * alpha),
        topLeft = Offset(topLeft.x + size * 0.04f, topLeft.y + size * 0.07f),
        size = Size(size, size),
        cornerRadius = cr
    )

    // 2. Base 3D Gradient Surface (radiant vertical gradient from light highlight to rich core)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(type.lightColor.copy(alpha = alpha), type.primaryColor.copy(alpha = alpha), type.darkColor.copy(alpha = alpha)),
            startY = topLeft.y,
            endY = topLeft.y + size
        ),
        topLeft = topLeft,
        size = Size(size, size),
        cornerRadius = cr
    )

    // 3. Top-Left 3D Chamfer / Highlight Bevel
    val bevelW = size * 0.16f
    val highlightPath = Path().apply {
        moveTo(topLeft.x, topLeft.y + size)
        lineTo(topLeft.x, topLeft.y)
        lineTo(topLeft.x + size, topLeft.y)
        lineTo(topLeft.x + size - bevelW, topLeft.y + bevelW)
        lineTo(topLeft.x + bevelW, topLeft.y + bevelW)
        lineTo(topLeft.x + bevelW, topLeft.y + size - bevelW)
        close()
    }
    drawPath(
        path = highlightPath,
        color = Color.White.copy(alpha = 0.38f * alpha)
    )

    // 4. Bottom-Right 3D Deep Shadow Bevel
    val shadowPath = Path().apply {
        moveTo(topLeft.x + size, topLeft.y)
        lineTo(topLeft.x + size, topLeft.y + size)
        lineTo(topLeft.x, topLeft.y + size)
        lineTo(topLeft.x + bevelW, topLeft.y + size - bevelW)
        lineTo(topLeft.x + size - bevelW, topLeft.y + size - bevelW)
        lineTo(topLeft.x + size - bevelW, topLeft.y + bevelW)
        close()
    }
    drawPath(
        path = shadowPath,
        color = Color.Black.copy(alpha = 0.35f * alpha)
    )

    // 5. Polished Curved Specular Lens Reflection (Glossy Glass/Candy Sheen)
    val glossHeight = size * 0.40f
    val glossWidth = size * 0.72f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f * alpha),
                Color.White.copy(alpha = 0.08f * alpha)
            ),
            startY = topLeft.y + size * 0.08f,
            endY = topLeft.y + size * 0.08f + glossHeight
        ),
        topLeft = Offset(topLeft.x + (size - glossWidth) / 2f, topLeft.y + size * 0.08f),
        size = Size(glossWidth, glossHeight),
        cornerRadius = CornerRadius(glossWidth * 0.35f, glossHeight * 0.5f)
    )

    // 6. Crisp Inner Border Accent
    drawRoundRect(
        color = Color.White.copy(alpha = 0.18f * alpha),
        topLeft = Offset(topLeft.x + bevelW, topLeft.y + bevelW),
        size = Size(size - bevelW * 2, size - bevelW * 2),
        cornerRadius = CornerRadius(size * 0.12f, size * 0.12f),
        style = Stroke(width = 1.2f)
    )

    // 7. Draw Adventure Special Items (Gems & Stars)
    if (specialItem != 0) {
        val center = Offset(topLeft.x + size / 2, topLeft.y + size / 2)
        when (specialItem) {
            SPECIAL_GEM_PINK -> drawPinkGem(center, size * 0.32f)
            SPECIAL_STAR_YELLOW -> drawYellowStar(center, size * 0.34f)
            SPECIAL_EMERALD_GREEN -> drawGreenDiamond(center, size * 0.32f)
        }
    }
}

fun DrawScope.drawPinkGem(center: Offset, radius: Float) {
    val path = Path()
    val points = 8
    for (i in 0 until points) {
        val angle = (i * 2 * PI / points) - (PI / 2)
        val r = if (i % 2 == 0) radius else radius * 0.65f
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = Color(0xFFFF4081))
    drawPath(path, color = Color.White, style = Stroke(width = 2f))
    drawCircle(Color.White.copy(alpha = 0.8f), radius = radius * 0.22f, center = center)
}

fun DrawScope.drawYellowStar(center: Offset, radius: Float) {
    val path = Path()
    val points = 10
    for (i in 0 until points) {
        val angle = (i * 2 * PI / points) - (PI / 2)
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = Color(0xFFFFD700))
    drawPath(path, color = Color(0xFFFFF9C4), style = Stroke(width = 1.5f))
}

fun DrawScope.drawGreenDiamond(center: Offset, radius: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }
    drawPath(path, color = Color(0xFF10B981))
    drawPath(path, color = Color.White, style = Stroke(width = 2f))
}
