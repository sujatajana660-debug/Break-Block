package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FullscreenReviveAdOverlay
import com.example.ui.components.LevelGateLockDialog
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ScreenType

@Composable
fun AdventureMapScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val maxUnlocked by viewModel.maxUnlockedLevel.collectAsStateWithLifecycle()
    val allLevelProgress by viewModel.allLevels.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progressMap = allLevelProgress.associateBy { it.levelNumber }
    val gridState = rememberLazyGridState()

    // Scroll to current playable level on entry
    LaunchedEffect(maxUnlocked) {
        val targetIndex = (maxUnlocked - 1).coerceAtLeast(0)
        gridState.scrollToItem(targetIndex)
    }

    BackHandler {
        viewModel.navigateTo(ScreenType.MODE_SELECT)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        viewModel.soundManager.playSundarClick()
                        viewModel.navigateTo(ScreenType.MODE_SELECT)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2638))
                        .testTag("adventure_map_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ADVENTURE",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "2000+ LEVELS",
                        color = Color(0xFFFFD700),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quick Play Button (respecting milestone gate lock)
                val currentPlayableLevel = minOf(maxUnlocked, stats.unlockedGateLevel)
                Button(
                    onClick = {
                        viewModel.soundManager.playSundarClick()
                        if (maxUnlocked > stats.unlockedGateLevel) {
                            viewModel.requestUnlockGate(stats.unlockedGateLevel + 1)
                        } else {
                            viewModel.startAdventureLevel(currentPlayableLevel)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (maxUnlocked > stats.unlockedGateLevel) Color(0xFFEAB308) else Color(0xFF22C55E)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("play_current_level_button")
                ) {
                    Text(
                        text = if (maxUnlocked > stats.unlockedGateLevel) "Unlock Lv ${stats.unlockedGateLevel + 1}" else "Lv $currentPlayableLevel",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Milestone Banner Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.8f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔒 Every 3 levels unlock with Rewarded Ad (Unlocked up to Lv ${stats.unlockedGateLevel})",
                    color = Color(0xFF93C5FD),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Level Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("adventure_levels_grid")
            ) {
                items(2500) { index ->
                    val levelNum = index + 1
                    val isPastGate = levelNum > stats.unlockedGateLevel
                    val isUnlocked = levelNum <= maxUnlocked && !isPastGate
                    val isGateLock = levelNum > stats.unlockedGateLevel && levelNum <= maxUnlocked
                    val progress = progressMap[levelNum]
                    val stars = progress?.stars ?: 0

                    LevelItemCard(
                        levelNumber = levelNum,
                        isUnlocked = isUnlocked,
                        isCurrent = levelNum == maxUnlocked && !isGateLock,
                        isGateLock = isGateLock,
                        stars = stars,
                        onClick = {
                            viewModel.soundManager.playSundarClick()
                            if (isGateLock) {
                                viewModel.requestUnlockGate(levelNum)
                            } else if (isUnlocked) {
                                viewModel.startAdventureLevel(levelNum)
                            }
                        }
                    )
                }
            }
        }

        // Milestone Gate Lock Dialog
        uiState.gateLockTargetLevel?.let { targetLevel ->
            LevelGateLockDialog(
                targetLevel = targetLevel,
                onWatchAdToUnlock = { viewModel.triggerGateUnlockRewardedAd() },
                onDismiss = { viewModel.dismissGateLockDialog() }
            )
        }

        // Fullscreen Rewarded Ad for Gate Unlock
        if (uiState.isWatchingGateAd) {
            val target = uiState.gateLockTargetLevel ?: 4
            FullscreenReviveAdOverlay(
                title = "UNLOCK LEVEL $target",
                rewardDescription = "Watch full video ad to unlock Levels $target to ${target + 2}!",
                onAdCompleted = { viewModel.completeGateUnlockAd() },
                onAdDismissed = { viewModel.dismissGateAd() }
            )
        }
    }
}

@Composable
fun LevelItemCard(
    levelNumber: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    isGateLock: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isGateLock -> Color(0xFF78350F) // Gold/Brown for Gate Lock
                isCurrent -> Color(0xFF2563EB)
                levelNumber > 50 && isUnlocked -> Color(0xFF7F1D1D)
                isUnlocked -> Color(0xFF1E293B)
                else -> Color(0xFF0F172A).copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent || isGateLock) 8.dp else 2.dp),
        modifier = Modifier
            .size(76.dp)
            .then(
                if (isCurrent) {
                    Modifier.border(2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                } else if (isGateLock) {
                    Modifier.border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                } else if (levelNumber > 50 && isUnlocked) {
                    Modifier.border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                } else Modifier
            )
            .clickable(enabled = isUnlocked || isGateLock) { onClick() }
            .testTag("level_card_$levelNumber")
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isGateLock) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Milestone Lock",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Lv $levelNumber",
                    color = Color(0xFFFDE68A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "AD UNLOCK",
                    color = Color(0xFF10B981),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            } else if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$levelNumber",
                    color = Color.DarkGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (levelNumber > 50) {
                        Text(text = "🔥", fontSize = 11.sp)
                    }
                    Text(
                        text = "$levelNumber",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                // Stars rating
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (s in 1..3) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (s <= stars) Color(0xFFFFD700) else Color(0xFF475569),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
