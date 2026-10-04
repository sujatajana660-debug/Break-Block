package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object UnityAdsManager {
    private const val TAG = "UnityAdsManager"

    // Real Placement IDs configured in User's Unity Dashboard (Game ID: 800387496)
    val placementRewarded: String = AdConfig.UNITY_PLACEMENT_REWARDED       // BP_Rewarded_Android
    val placementInterstitial: String = AdConfig.UNITY_PLACEMENT_INTERSTITIAL // BP_Interstitial_Android
    val placementBanner: String = AdConfig.UNITY_PLACEMENT_BANNER           // BP_Banner_Android

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _initializationStatus = MutableStateFlow("Initializing...")
    val initializationStatus: StateFlow<String> = _initializationStatus.asStateFlow()

    private val _rewardedAdLoaded = MutableStateFlow(false)
    val rewardedAdLoaded: StateFlow<Boolean> = _rewardedAdLoaded.asStateFlow()

    private val _interstitialAdLoaded = MutableStateFlow(false)
    val interstitialAdLoaded: StateFlow<Boolean> = _interstitialAdLoaded.asStateFlow()

    private val _lastAdError = MutableStateFlow<String?>(null)
    val lastAdError: StateFlow<String?> = _lastAdError.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isPreloading = false

    fun initialize(context: Context) {
        if (UnityAds.isInitialized) {
            _isInitialized.value = true
            _initializationStatus.value = "Ready (Live Game ID: ${AdConfig.UNITY_GAME_ID})"
            return
        }

        try {
            // Apply GDPR and Privacy Consent Metadata for Live Ad delivery
            val gdprMetaData = MetaData(context.applicationContext)
            gdprMetaData.set("gdpr.consent", true)
            gdprMetaData.commit()

            val ccpaMetaData = MetaData(context.applicationContext)
            ccpaMetaData.set("privacy.consent", true)
            ccpaMetaData.commit()

            Log.d(TAG, "Initializing Unity Ads in LIVE MODE with Game ID: ${AdConfig.UNITY_GAME_ID}")
            _initializationStatus.value = "Connecting to Unity Ads Live Network..."

            UnityAds.initialize(
                context.applicationContext,
                AdConfig.UNITY_GAME_ID,
                AdConfig.TEST_MODE, // strictly false
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        Log.d(TAG, "Unity Ads successfully initialized (TestMode: ${AdConfig.TEST_MODE})!")
                        _isInitialized.value = true
                        _initializationStatus.value = "Ready (Game ID: ${AdConfig.UNITY_GAME_ID})"
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

    fun preloadAds() {
        if (isPreloading || !UnityAds.isInitialized) return
        isPreloading = true
        preloadInterstitial()
        preloadRewarded()
        coroutineScope.launch {
            delay(5000)
            isPreloading = false
        }
    }

    fun preloadInterstitial() {
        if (!UnityAds.isInitialized) return
        try {
            Log.d(TAG, "Requesting load for Interstitial: $placementInterstitial")
            UnityAds.load(
                placementInterstitial,
                object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(loadedPlacementId: String) {
                        Log.d(TAG, "Interstitial loaded successfully: $loadedPlacementId")
                        _interstitialAdLoaded.value = true
                        _lastAdError.value = null
                    }

                    override fun onUnityAdsFailedToLoad(failedPlacementId: String, error: UnityAdsLoadError, message: String) {
                        Log.w(TAG, "Interstitial load status: $failedPlacementId, $error: $message")
                        _interstitialAdLoaded.value = false
                        _lastAdError.value = "Interstitial: $error ($message)"
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading interstitial ad", e)
        }
    }

    fun preloadRewarded() {
        if (!UnityAds.isInitialized) return
        try {
            Log.d(TAG, "Requesting load for Rewarded: $placementRewarded")
            UnityAds.load(
                placementRewarded,
                object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(loadedPlacementId: String) {
                        Log.d(TAG, "Rewarded ad loaded successfully: $loadedPlacementId")
                        _rewardedAdLoaded.value = true
                        _lastAdError.value = null
                    }

                    override fun onUnityAdsFailedToLoad(failedPlacementId: String, error: UnityAdsLoadError, message: String) {
                        Log.w(TAG, "Rewarded load status: $failedPlacementId, $error: $message")
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
        coroutineScope.launch {
            var waited = 0
            while (!UnityAds.isInitialized && waited < 20) {
                delay(100)
                waited++
            }

            if (!UnityAds.isInitialized) {
                Log.w(TAG, "Unity Ads not initialized. Calling fallback dismissal.")
                onAdDismissed()
                return@launch
            }

            val showListener = object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(placementId: String, error: UnityAdsShowError, message: String) {
                    Log.e(TAG, "Interstitial show failed: $placementId, $error, $message")
                    _lastAdError.value = "Show Interstitial Failed: $error"
                    _interstitialAdLoaded.value = false
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
                    _interstitialAdLoaded.value = false
                    onAdDismissed()
                }
            }

            if (_interstitialAdLoaded.value) {
                UnityAds.show(activity, placementInterstitial, UnityAdsShowOptions(), showListener)
            } else {
                UnityAds.load(placementInterstitial, object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placementId: String) {
                        UnityAds.show(activity, placementId, UnityAdsShowOptions(), showListener)
                    }

                    override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAdsLoadError, message: String) {
                        Log.w(TAG, "Interstitial on-the-fly load failed: $error ($message)")
                        onAdDismissed()
                    }
                })
            }
        }
    }

    /**
     * Show Real Unity Rewarded Ad.
     * When user cuts/skips/completes the ad, it AUTOMATICALLY claims the reward!
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardGranted: () -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        coroutineScope.launch {
            var waited = 0
            while (!UnityAds.isInitialized && waited < 20) {
                delay(100)
                waited++
            }

            if (!UnityAds.isInitialized) {
                val err = "Unity Ads not connected"
                Log.w(TAG, err)
                onAdFailed(err)
                return@launch
            }

            val showListener = object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(placementId: String, error: UnityAdsShowError, message: String) {
                    val err = "Rewarded Ad failed: $error ($message)"
                    Log.e(TAG, err)
                    _lastAdError.value = err
                    _rewardedAdLoaded.value = false
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
                    _rewardedAdLoaded.value = false
                    onRewardGranted()
                }
            }

            if (_rewardedAdLoaded.value) {
                UnityAds.show(activity, placementRewarded, UnityAdsShowOptions(), showListener)
            } else {
                UnityAds.load(placementRewarded, object : IUnityAdsLoadListener {
                    override fun onUnityAdsAdLoaded(placementId: String) {
                        _rewardedAdLoaded.value = true
                        UnityAds.show(activity, placementId, UnityAdsShowOptions(), showListener)
                    }

                    override fun onUnityAdsFailedToLoad(placementId: String, error: UnityAdsLoadError, message: String) {
                        val err = "Rewarded ad load error: $error ($message)"
                        Log.w(TAG, err)
                        _lastAdError.value = err
                        onAdFailed(err)
                    }
                })
            }
        }
    }
}
