package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameStatsEntity
import com.example.model.BoosterType

@Composable
fun BoosterBar(
    selectedBooster: BoosterType?,
    canUndo: Boolean,
    stats: GameStatsEntity,
    usedAdBoostersInSession: Set<BoosterType> = emptySet(),
    onBoosterClick: (BoosterType) -> Unit,
    onCancelBooster: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Booster Instructions Banner
        AnimatedVisibility(
            visible = selectedBooster != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val (hint, color) = when (selectedBooster) {
                BoosterType.BOMB -> Pair("💥 TAP ANY BLOCK TO BLAST 4x4 AREA!", Color(0xFFEF4444))
                BoosterType.LINE -> Pair("⚡ TAP ANY ROW TO BREAK FULL LINE!", Color(0xFF3B82F6))
                BoosterType.PLUS -> Pair("➕ TAP ANY BLOCK TO BLAST IN ALL 4 DIRECTIONS (+)! 🎯", Color(0xFF10B981))
                else -> Pair("", Color.Transparent)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.25f))
                    .border(1.5.dp, color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = hint,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onCancelBooster,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Booster",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Booster Buttons Row (1 Free Ad Use per match for each booster!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF131D31).copy(alpha = 0.95f))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. BOOM 4x4
            val bombFreeAd = BoosterType.BOMB !in usedAdBoostersInSession
            BoosterButton(
                type = BoosterType.BOMB,
                count = stats.bombCount,
                hasFreeAd = bombFreeAd,
                isSelected = selectedBooster == BoosterType.BOMB,
                isEnabled = stats.bombCount > 0 || bombFreeAd,
                icon = Icons.Default.LocalFireDepartment,
                accentColor = Color(0xFFEF4444),
                testTag = "booster_bomb",
                onClick = { onBoosterClick(BoosterType.BOMB) }
            )

            // 2. LINE BREAKER
            val lineFreeAd = BoosterType.LINE !in usedAdBoostersInSession
            BoosterButton(
                type = BoosterType.LINE,
                count = stats.lineBreakerCount,
                hasFreeAd = lineFreeAd,
                isSelected = selectedBooster == BoosterType.LINE,
                isEnabled = stats.lineBreakerCount > 0 || lineFreeAd,
                icon = Icons.Default.ElectricBolt,
                accentColor = Color(0xFF3B82F6),
                testTag = "booster_line",
                onClick = { onBoosterClick(BoosterType.LINE) }
            )

            // 3. + CROSS BREAKER (4 DIRECTIONS)
            val plusFreeAd = BoosterType.PLUS !in usedAdBoostersInSession
            BoosterButton(
                type = BoosterType.PLUS,
                count = stats.plusBreakerCount,
                hasFreeAd = plusFreeAd,
                isSelected = selectedBooster == BoosterType.PLUS,
                isEnabled = stats.plusBreakerCount > 0 || plusFreeAd,
                icon = Icons.Default.Add,
                accentColor = Color(0xFF10B981),
                testTag = "booster_plus",
                onClick = { onBoosterClick(BoosterType.PLUS) }
            )

            // 4. UNDO MOVE
            val undoFreeAd = BoosterType.UNDO !in usedAdBoostersInSession
            BoosterButton(
                type = BoosterType.UNDO,
                count = stats.undoCount,
                hasFreeAd = undoFreeAd,
                isSelected = false,
                isEnabled = canUndo && (stats.undoCount > 0 || undoFreeAd),
                icon = Icons.AutoMirrored.Filled.Undo,
                accentColor = Color(0xFFF59E0B),
                testTag = "booster_undo",
                onClick = { onBoosterClick(BoosterType.UNDO) }
            )
        }
    }
}

@Composable
private fun BoosterButton(
    type: BoosterType,
    count: Int,
    hasFreeAd: Boolean,
    isSelected: Boolean,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.Transparent,
        label = "booster_border"
    )
    val bgColor by animateColorAsState(
        targetValue = when {
            isSelected -> accentColor.copy(alpha = 0.35f)
            !isEnabled -> Color(0xFF1E293B).copy(alpha = 0.4f)
            else -> Color(0xFF1E293B)
        },
        label = "booster_bg"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) borderColor else Color(0xFF334155), RoundedCornerShape(14.dp))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        BadgedBox(
            badge = {
                val isAdBadge = count <= 0 && hasFreeAd
                Badge(
                    containerColor = when {
                        count > 0 -> accentColor
                        isAdBadge -> Color(0xFF10B981)
                        else -> Color(0xFF64748B)
                    },
                    contentColor = Color.White
                ) {
                    Text(
                        text = when {
                            count > 0 -> "$count"
                            isAdBadge -> "1📺"
                            else -> "0"
                        },
                        fontSize = if (isAdBadge) 8.sp else 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = type.title,
                tint = if ((count > 0 || hasFreeAd) && isEnabled) accentColor else Color.LightGray.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = when (type) {
                BoosterType.BOMB -> "Boom 4x4"
                BoosterType.LINE -> "Line"
                BoosterType.PLUS -> "+ Cross"
                BoosterType.UNDO -> "Undo"
            },
            color = if ((count > 0 || hasFreeAd) && isEnabled) Color.White else Color(0xFF64748B),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}
