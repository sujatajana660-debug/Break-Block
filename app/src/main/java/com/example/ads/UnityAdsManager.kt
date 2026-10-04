package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAds.UnityAdsInitializationError
import com.unity3d.ads.UnityAds.UnityAdsLoadError
import com.unity3d.ads.UnityAds.UnityAdsShowCompletionState
import com.unity3d.ads.UnityAds.UnityAdsShowError
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.ads.metadata.MetaData
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UnityAdsManager {
    private const val TAG = "UnityAdsManager"

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _initializationStatus = MutableStateFlow("Initializing...")
    val initializationStatus: StateFlow<String> = _initializationStatus.asStateFlow()

    private val _rewardedAdLoaded = MutableStateFlow(false)
    val rewardedAdLoaded: StateFlow<Boolean> = _rewardedAdLoaded.asStateFlow()

    private val _interstitialAdLoaded = MutableStateFlow(false)
    val interstitialAdLoaded: StateFlow<Boolean> = _interstitialAdLoaded.asStateFlow()

    private val _bannerAdLoaded = MutableStateFlow(false)
    val bannerAdLoaded: StateFlow<Boolean> = _bannerAdLoaded.asStateFlow()

    private val _lastAdError = MutableStateFlow<String?>(null)
    val lastAdError: StateFlow<String?> = _lastAdError.asStateFlow()

    fun initialize(context: Context) {
        if (UnityAds.isInitialized) {
            _isInitialized.value = true
            _initializationStatus.value = "Initialized"
            preloadAds()
            return
        }

        try {
            // Apply GDPR and Privacy Consent Metadata
            val gdprMetaData = MetaData(context.applicationContext)
            gdprMetaData.set("gdpr.consent", true)
            gdprMetaData.commit()

            val ccpaMetaData = MetaData(context.applicationContext)
            ccpaMetaData.set("privacy.consent", true)
            ccpaMetaData.commit()

            Log.d(TAG, "Initializing Unity Ads with Game ID: ${AdConfig.UNITY_GAME_ID}, TestMode: ${AdConfig.testMode}")
            _initializationStatus.value = "Connecting to Unity Ads (${if (AdConfig.testMode) "Test Mode" else "Live Mode"})..."

            UnityAds.initialize(
                context.applicationContext,
                AdConfig.UNITY_GAME_ID,
                AdConfig.testMode,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        Log.d(TAG, "Unity Ads successfully initialized!")
                        _isInitialized.value = true
                        _initializationStatus.value = "Ready (Live Game ID: ${AdConfig.UNITY_GAME_ID})"
                        preloadAds()
                    }

                    override fun onInitializationFailed(error: UnityAdsInitializationError, message: String) {
                        val errMsg = "Init Failed: $error - $message"
                        Log.e(TAG, errMsg)
                        _isInitialized.value = false
                        _initializationStatus.value = errMsg
                        _lastAdError.value = errMsg
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Unity Ads init", e)
            _initializationStatus.value = "Error: ${e.localizedMessage}"
            _lastAdError.value = e.localizedMessage
        }
    }

    fun setTestMode(enabled: Boolean, context: Context) {
        AdConfig.testMode = enabled
        // Re-initialize with new mode
        initialize(context)
    }

    fun preloadAds() {
        preloadInterstitial()
        preloadRewarded()
    }

    private fun preloadInterstitial() {
        try {
            UnityAds.load(
                AdConfig.UNITY_PLACEMENT_INTERSTITIAL,
                object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placementId: String) {
                        Log.d(TAG, "Interstitial loaded: $placementId")
                        _interstitialAdLoaded.value = true
                    }

                    override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAdsLoadError, message: String) {
                        Log.w(TAG, "Interstitial failed to load: $placementId, $error, $message")
                        _interstitialAdLoaded.value = false
                        _lastAdError.value = "Interstitial: $error ($message)"
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading interstitial ad", e)
        }
    }

    private fun preloadRewarded() {
        try {
            UnityAds.load(
                AdConfig.UNITY_PLACEMENT_REWARDED,
                object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placementId: String) {
                        Log.d(TAG, "Rewarded ad loaded: $placementId")
                        _rewardedAdLoaded.value = true
                    }

                    override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAdsLoadError, message: String) {
                        Log.w(TAG, "Rewarded ad failed to load: $placementId, $error, $message")
                        _rewardedAdLoaded.value = false
                        _lastAdError.value = "Rewarded: $error ($message)"
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading rewarded ad", e)
        }
    }

    /**
     * Show Real Unity Interstitial Ad on Game Over.
     */
    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit
    ) {
        if (!UnityAds.isInitialized) {
            Log.w(TAG, "Unity Ads not initialized. Calling fallback dismissal.")
            onAdDismissed()
            return
        }

        UnityAds.show(
            activity,
            AdConfig.UNITY_PLACEMENT_INTERSTITIAL,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(placementId: String, error: UnityAdsShowError, message: String) {
                    Log.e(TAG, "Interstitial show failed: $placementId, $error, $message")
                    _lastAdError.value = "Show Interstitial Failed: $error"
                    onAdDismissed()
                }

                override fun onUnityAdsShowStart(placementId: String) {
                    Log.d(TAG, "Interstitial started: $placementId")
                }

                override fun onUnityAdsShowClick(placementId: String) {
                    Log.d(TAG, "Interstitial clicked: $placementId")
                }

                override fun onUnityAdsShowComplete(placementId: String, state: UnityAdsShowCompletionState) {
                    Log.d(TAG, "Interstitial complete: $placementId, state: $state")
                    // Preload next interstitial
                    preloadInterstitial()
                    onAdDismissed()
                }
            }
        )
    }

    /**
     * Show Real Unity Rewarded Ad.
     * Per user instruction:
     * When user cuts/skips/completes the ad, it AUTOMATICALLY claims the reward!
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardGranted: () -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        if (!UnityAds.isInitialized) {
            val err = "Unity Ads not initialized yet"
            Log.w(TAG, err)
            onAdFailed(err)
            return
        }

        UnityAds.show(
            activity,
            AdConfig.UNITY_PLACEMENT_REWARDED,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(placementId: String, error: UnityAdsShowError, message: String) {
                    val err = "Rewarded Ad failed: $error ($message)"
                    Log.e(TAG, err)
                    _lastAdError.value = err
                    onAdFailed(err)
                }

                override fun onUnityAdsShowStart(placementId: String) {
                    Log.d(TAG, "Rewarded ad started: $placementId")
                }

                override fun onUnityAdsShowClick(placementId: String) {
                    Log.d(TAG, "Rewarded ad clicked: $placementId")
                }

                override fun onUnityAdsShowComplete(placementId: String, state: UnityAdsShowCompletionState) {
                    Log.d(TAG, "Rewarded ad complete: $placementId, state: $state")
                    // Auto-claim reward whether completed or skipped by user!
                    onRewardGranted()
                    // Preload next rewarded ad
                    preloadRewarded()
                }
            }
        )
    }
}

/**
 * Jetpack Compose wrapper for Unity Banner Ad.
 */
@Composable
fun UnityBannerAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var loadError by remember { mutableStateOf<String?>(null) }
    var isLoaded by remember { mutableStateOf(false) }

    if (activity != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                factory = { ctx ->
                    val bannerView = BannerView(
                        activity,
                        AdConfig.UNITY_PLACEMENT_BANNER,
                        UnityBannerSize(320, 50)
                    )
                    bannerView.listener = object : BannerView.IListener {
                        override fun onBannerLoaded(bannerAdView: BannerView) {
                            Log.d("UnityBannerAd", "Banner loaded successfully")
                            isLoaded = true
                            loadError = null
                        }

                        override fun onBannerShown(bannerAdView: BannerView) {
                            Log.d("UnityBannerAd", "Banner shown on screen")
                            isLoaded = true
                        }

                        override fun onBannerFailedToLoad(bannerAdView: BannerView, errorInfo: BannerErrorInfo) {
                            val msg = "Banner Error: ${errorInfo.errorMessage}"
                            Log.w("UnityBannerAd", msg)
                            loadError = errorInfo.errorMessage
                        }

                        override fun onBannerClick(bannerAdView: BannerView) {
                            Log.d("UnityBannerAd", "Banner clicked")
                        }

                        override fun onBannerLeftApplication(bannerAdView: BannerView) {
                            Log.d("UnityBannerAd", "Banner left app")
                        }
                    }
                    bannerView.load()
                    bannerView
                }
            )

            // Diagnostic status banner if live ads returned no-fill on mobile
            if (!isLoaded && loadError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unity Banner (${AdConfig.UNITY_PLACEMENT_BANNER}): $loadError",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
