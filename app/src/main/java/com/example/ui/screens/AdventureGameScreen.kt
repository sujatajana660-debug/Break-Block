package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AdventureScoreHeader
import com.example.ui.components.AdventureWinDialog
import com.example.ui.components.BoardView
import com.example.ui.components.BoosterBar
import com.example.ui.components.DraggedPieceOverlay
import com.example.ui.components.ExtraBoosterDialog
import com.example.ui.components.FullscreenInterstitialAdOverlay
import com.example.ui.components.FullscreenReviveAdOverlay
import com.example.ui.components.GameOverDialog
import com.example.ui.components.LevelGateLockDialog
import com.example.ui.components.MascotBanner
import com.example.ui.components.PauseDialog
import com.example.ui.components.PieceTrayView
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StreakRewardDialog
import com.example.ui.components.UnityAdsBanner
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ScreenType

@Composable
fun AdventureGameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var boardBounds by remember { mutableStateOf(Rect.Zero) }
    var cellSizePx by remember { mutableFloatStateOf(0f) }
    var showSettings by remember { mutableStateOf(false) }
    val level = uiState.activeLevel

    // System back button triggers pause dialog
    BackHandler {
        viewModel.pauseGame()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
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
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (level != null) {
                AdventureScoreHeader(
                    level = level,
                    score = uiState.score,
                    collectedGems = uiState.collectedGems,
                    collectedStars = uiState.collectedStars,
                    collectedEmeralds = uiState.collectedEmeralds,
                    onBackClick = { viewModel.pauseGame() },
                    onSettingsClick = {
                        viewModel.soundManager.playSundarClick()
                        showSettings = true
                    }
                )

                // Hard Level / Move Counter Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (level.isHardLevel) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "HARD LEVEL",
                                color = Color(0xFFFF8A80),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }

                    // Unlimited Moves
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF065F46).copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Moves: ∞ Unlimited",
                            color = Color(0xFF6EE7B7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Combo Action Banner (red face removed per request)
            MascotBanner(
                bannerText = uiState.celebrationBanner,
                mood = uiState.mascotMood
            )

            // 8x8 Board with Booster Tap Handler
            BoardView(
                boardState = uiState.boardState,
                dragState = uiState.dragState,
                onBoundsMeasured = { bounds, sizePx ->
                    boardBounds = bounds
                    cellSizePx = sizePx
                },
                onCellClick = { r, c ->
                    viewModel.applyBoardBooster(r, c)
                }
            )

            // Booster Bar (Boom 4x4, Line, + Cross, Undo)
            BoosterBar(
                selectedBooster = uiState.selectedBooster,
                canUndo = uiState.canUndo,
                stats = stats,
                usedAdBoostersInSession = uiState.usedAdBoostersInSession,
                onBoosterClick = { booster -> viewModel.selectBooster(booster) },
                onCancelBooster = { viewModel.cancelBooster() }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 3-Piece Tray
            PieceTrayView(
                pieces = uiState.trayPieces,
                boardState = uiState.boardState,
                dragState = uiState.dragState,
                onDragStart = { idx, piece, offset ->
                    viewModel.onDragStart(idx, piece, offset)
                },
                onDragMove = { offset ->
                    viewModel.onDragMove(offset, boardBounds, cellSizePx)
                },
                onDragEnd = {
                    viewModel.onDragEnd()
                }
            )

            // Bottom Unity Ads banner elevated
            UnityAdsBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        // Overlay for dragged piece
        if (uiState.dragState.piece != null && cellSizePx > 0f) {
            DraggedPieceOverlay(
                dragState = uiState.dragState,
                cellSizePx = cellSizePx
            )
        }

        // Pause Dialog
        if (uiState.isPaused) {
            PauseDialog(
                onResume = { viewModel.resumeGame() },
                onRestart = { level?.let { viewModel.startAdventureLevel(it.levelNumber) } },
                onQuit = { viewModel.navigateTo(ScreenType.ADVENTURE_MAP) }
            )
        }

        // Level Won Dialog
        if (uiState.isLevelWon && level != null && !uiState.showStreakRewardDialog && uiState.gateLockTargetLevel == null) {
            AdventureWinDialog(
                levelNumber = level.levelNumber,
                stars = uiState.earnedStars,
                onNextLevel = {
                    viewModel.advanceToNextAdventureLevel()
                },
                onLevelMap = {
                    viewModel.navigateTo(ScreenType.ADVENTURE_MAP)
                }
            )
        }

        // Milestone Gate Lock Dialog (every 3 levels)
        uiState.gateLockTargetLevel?.let { targetLevel ->
            LevelGateLockDialog(
                targetLevel = targetLevel,
                onWatchAdToUnlock = { viewModel.triggerGateUnlockRewardedAd() },
                onDismiss = {
                    viewModel.dismissGateLockDialog()
                    viewModel.navigateTo(ScreenType.ADVENTURE_MAP)
                }
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

        // Automatic 3-Level Streak Reward Dialog
        if (uiState.showStreakRewardDialog) {
            StreakRewardDialog(
                levelNumber = uiState.streakRewardLevelNumber,
                onWatchAdClick = { viewModel.triggerStreakRewardedAd() },
                onDismiss = { viewModel.dismissStreakRewardDialog() }
            )
        }

        // 1. FULLSCREEN INTERSTITIAL AD ON GAME OVER
        if (uiState.isShowingInterstitialAd) {
            FullscreenInterstitialAdOverlay(
                score = uiState.score,
                onAdDismissed = { viewModel.dismissInterstitialAd() }
            )
        }

        // 2. Fullscreen Rewarded Video Ad for Revive
        if (uiState.isWatchingReviveAd) {
            FullscreenReviveAdOverlay(
                title = "REVIVE MATCH",
                rewardDescription = "Watch full video ad to revive your match and clear the board center!",
                onAdCompleted = { viewModel.completeReviveAd() },
                onAdDismissed = { viewModel.dismissReviveAd() }
            )
        }

        // 3. Game Over Dialog (shown only when not showing ad and not showing interstitial)
        if (uiState.isGameOver && !uiState.isLevelWon && !uiState.isShowingInterstitialAd && !uiState.isWatchingReviveAd) {
            GameOverDialog(
                score = uiState.score,
                isNewRecord = false,
                hasUsedReviveInSession = uiState.hasUsedReviveInSession,
                onReviveWithAd = { viewModel.triggerReviveAd() },
                onRestart = {
                    level?.let { viewModel.startAdventureLevel(it.levelNumber) }
                },
                onHome = { viewModel.navigateTo(ScreenType.ADVENTURE_MAP) }
            )
        }

        // Extra Booster Rewarded Ad Prompt Dialog
        uiState.boosterRewardPrompt?.let { boosterType ->
            ExtraBoosterDialog(
                boosterType = boosterType,
                onWatchAdClick = {
                    viewModel.triggerExtraBoosterRewardedAd(boosterType)
                },
                onDismiss = { viewModel.dismissBoosterRewardPrompt() }
            )
        }

        // Fullscreen Rewarded Ad for Claiming Extra Booster
        if (uiState.isWatchingBoosterAd) {
            val target = uiState.boosterAdTarget ?: com.example.model.BoosterType.BOMB
            FullscreenReviveAdOverlay(
                title = "CLAIM ${target.title.uppercase()}",
                rewardDescription = "Watch full video ad to unlock +1 ${target.title} booster for this match!",
                onAdCompleted = { viewModel.completeBoosterRewardedAd() },
                onAdDismissed = { viewModel.dismissBoosterAd() }
            )
        }

        // Settings Dialog
        if (showSettings) {
            SettingsDialog(
                soundEnabled = stats.soundEnabled,
                hapticEnabled = stats.hapticEnabled,
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptic = { viewModel.toggleHaptics() },
                onDismiss = { showSettings = false }
            )
        }
    }
}
