package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdventureLevel

@Composable
fun ClassicScoreHeader(
    score: Int,
    highScore: Int,
    comboCount: Int,
    onPauseClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back / Pause Button (In-game exit/pause)
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2638).copy(alpha = 0.8f))
                    .testTag("in_game_pause_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back / Pause",
                    tint = Color.White
                )
            }

            // High score badge with crown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E2638).copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("high_score_badge")
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = "High Score",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$highScore",
                    color = Color(0xFFFFD700),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2638).copy(alpha = 0.8f))
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Giant Current Score
        Text(
            text = "$score",
            color = Color.White,
            fontSize = 54.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.testTag("current_score_text")
        )

        // Combo Tag if active
        if (comboCount > 1) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF9800))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("combo_badge")
            ) {
                Text(
                    text = "COMBO $comboCount!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun AdventureScoreHeader(
    level: AdventureLevel,
    score: Int,
    collectedGems: Int,
    collectedStars: Int,
    collectedEmeralds: Int,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E2638).copy(alpha = 0.8f))
                    .clickable { onBackClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("adventure_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Level ${level.levelNumber}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2638).copy(alpha = 0.8f))
                    .testTag("adventure_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Target Collectibles Bar
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF151D2F))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("level_objectives_bar")
        ) {
            if (level.target.pinkGemsRequired > 0) {
                TargetCounter(
                    iconColor = Color(0xFFFF4081),
                    label = "💎",
                    current = collectedGems,
                    required = level.target.pinkGemsRequired
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            if (level.target.yellowStarsRequired > 0) {
                TargetCounter(
                    iconColor = Color(0xFFFFD700),
                    label = "⭐",
                    current = collectedStars,
                    required = level.target.yellowStarsRequired
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            if (level.target.emeraldsRequired > 0) {
                TargetCounter(
                    iconColor = Color(0xFF10B981),
                    label = "🟢",
                    current = collectedEmeralds,
                    required = level.target.emeraldsRequired
                )
            }
        }
    }
}

@Composable
fun TargetCounter(
    iconColor: Color,
    label: String,
    current: Int,
    required: Int
) {
    val isCompleted = current >= required
    val displayCurrent = current.coerceAtMost(required)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCompleted) Color(0x334ADE80) else Color(0x25FFFFFF))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = label, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isCompleted) "$required/$required ✓" else "$displayCurrent/$required",
            color = if (isCompleted) Color(0xFF4ADE80) else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}
