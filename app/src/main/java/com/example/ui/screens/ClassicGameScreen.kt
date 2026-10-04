package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BoardView
import com.example.ui.components.BoosterBar
import com.example.ui.components.ClassicScoreHeader
import com.example.ui.components.DraggedPieceOverlay
import com.example.ui.components.ExtraBoosterDialog
import com.example.ui.components.FullscreenInterstitialAdOverlay
import com.example.ui.components.FullscreenReviveAdOverlay
import com.example.ui.components.GameOverDialog
import com.example.ui.components.MascotBanner
import com.example.ui.components.PauseDialog
import com.example.ui.components.PieceTrayView
import com.example.ui.components.SettingsDialog
import com.example.ui.components.UnityAdsBanner
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ScreenType

@Composable
fun ClassicGameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var boardBounds by remember { mutableStateOf(Rect.Zero) }
    var cellSizePx by remember { mutableFloatStateOf(0f) }
    var showSettings by remember { mutableStateOf(false) }

    // System back button pauses the game
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
            // Header with Back/Pause button and Settings
            ClassicScoreHeader(
                score = uiState.score,
                highScore = stats.classicHighScore,
                comboCount = uiState.comboCount,
                onPauseClick = { viewModel.pauseGame() },
                onSettingsClick = {
                    viewModel.soundManager.playSundarClick()
                    showSettings = true
                }
            )

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

            // Unity Ads Banner at bottom elevated above navigation bar
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
                onRestart = { viewModel.startClassicGame() },
                onQuit = { viewModel.quitGameToMenu() }
            )
        }

        // 1. FULLSCREEN INTERSTITIAL AD ON GAME OVER
        if (uiState.isShowingInterstitialAd) {
            FullscreenInterstitialAdOverlay(
                score = uiState.score,
                onAdDismissed = { viewModel.dismissInterstitialAd() }
            )
        }

        // 2. Fullscreen Rewarded Video Ad for Revive (active when clicking revive in game over dialog)
        if (uiState.isWatchingReviveAd) {
            FullscreenReviveAdOverlay(
                title = "REVIVE MATCH",
                rewardDescription = "Watch full video ad to revive your match and clear the board center!",
                onAdCompleted = { viewModel.completeReviveAd() },
                onAdDismissed = { viewModel.dismissReviveAd() }
            )
        }

        // 3. Game Over Dialog (shown when not watching ad and not showing interstitial)
        if (uiState.isGameOver && !uiState.isShowingInterstitialAd && !uiState.isWatchingReviveAd) {
            GameOverDialog(
                score = uiState.score,
                isNewRecord = uiState.isNewRecord,
                hasUsedReviveInSession = uiState.hasUsedReviveInSession,
                onReviveWithAd = { viewModel.triggerReviveAd() },
                onRestart = { viewModel.startClassicGame() },
                onHome = { viewModel.navigateTo(ScreenType.MODE_SELECT) }
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
