package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.NoInternetOverlay
import com.example.ui.screens.AdventureGameScreen
import com.example.ui.screens.AdventureMapScreen
import com.example.ui.screens.AppLoadingScreen
import com.example.ui.screens.ClassicGameScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ModeSelectScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ScreenType

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()
    private var wasInBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize Unity Ads SDK with Game ID: 800387496
        com.example.ads.UnityAdsManager.initialize(this)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    BreakBlocksApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // App is minimized or put in background
        wasInBackground = true
    }

    override fun onStart() {
        super.onStart()
        if (wasInBackground) {
            wasInBackground = false
        }
    }
}

@Composable
fun BreakBlocksApp(viewModel: GameViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    var isLoadingFinished by remember { mutableStateOf(false) }

    if (!isLoadingFinished) {
        AppLoadingScreen(
            onLoadingComplete = { isLoadingFinished = true }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Screen Navigation
            when (uiState.currentScreen) {
                ScreenType.MODE_SELECT -> ModeSelectScreen(viewModel = viewModel)
                ScreenType.CLASSIC_PLAY -> ClassicGameScreen(viewModel = viewModel)
                ScreenType.ADVENTURE_MAP -> AdventureMapScreen(viewModel = viewModel)
                ScreenType.ADVENTURE_PLAY -> AdventureGameScreen(viewModel = viewModel)
                ScreenType.LEADERBOARD -> LeaderboardScreen(viewModel = viewModel)
            }

            // Blocking No Internet Overlay: Blocks gameplay when offline
            if (!isOnline) {
                NoInternetOverlay(
                    onRetry = { viewModel.checkInternet() }
                )
            }
        }
    }
}
