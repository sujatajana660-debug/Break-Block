package com.example.ui.components

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdConfig
import com.example.ads.UnityAdsManager
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

/**
 * Real Unity Ads Banner Component.
 * Integrates directly with Unity Ads SDK (Placement: BP_Banner_Android, Game ID: 800387496).
 * Automatically initializes BannerView as soon as Unity Ads network connection is established.
 */
@Composable
fun UnityAdsBanner(
    modifier: Modifier = Modifier,
    gameId: String = AdConfig.UNITY_GAME_ID,
    placementId: String = AdConfig.UNITY_PLACEMENT_BANNER
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isAdsInitialized by UnityAdsManager.isInitialized.collectAsStateWithLifecycle()

    var isUnityBannerLoaded by remember { mutableStateOf(false) }
    var bannerErrorMessage by remember { mutableStateOf<String?>(null) }
    var bannerViewRef by remember { mutableStateOf<BannerView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            bannerViewRef?.destroy()
            bannerViewRef = null
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 18.dp)
            .height(52.dp)
            .testTag("unity_ads_banner_container"),
        contentAlignment = Alignment.Center
    ) {
        if (activity != null && isAdsInitialized) {
            // Real Unity Ads BannerView
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                factory = { ctx ->
                    val banner = BannerView(
                        activity,
                        placementId,
                        UnityBannerSize(320, 50)
                    ).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    banner.listener = object : BannerView.IListener {
                        override fun onBannerLoaded(bannerAdView: BannerView) {
                            Log.d("UnityAdsBanner", "Unity Banner loaded for $placementId")
                            isUnityBannerLoaded = true
                            bannerErrorMessage = null
                        }

                        override fun onBannerShown(bannerAdView: BannerView) {
                            Log.d("UnityAdsBanner", "Unity Banner shown for $placementId")
                            isUnityBannerLoaded = true
                        }

                        override fun onBannerFailedToLoad(bannerAdView: BannerView, errorInfo: BannerErrorInfo) {
                            val msg = errorInfo.errorMessage
                            Log.w("UnityAdsBanner", "Unity Banner failed ($placementId): $msg")
                            bannerErrorMessage = msg
                            isUnityBannerLoaded = false
                        }

                        override fun onBannerClick(bannerAdView: BannerView) {
                            Log.d("UnityAdsBanner", "Unity Banner clicked")
                        }

                        override fun onBannerLeftApplication(bannerAdView: BannerView) {
                            Log.d("UnityAdsBanner", "Unity Banner left app")
                        }
                    }
                    banner.load()
                    bannerViewRef = banner
                    banner
                },
                update = { banner ->
                    // Banner maintained in layout
                }
            )
        }

        // When Unity Banner is loading or if no fill returned from live ad network, show branded placeholder
        if (!isUnityBannerLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(48.dp)
                    .shadow(10.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF334155),
                                Color(0xFFF59E0B).copy(alpha = pulseAlpha),
                                Color(0xFF38BDF8).copy(alpha = pulseAlpha),
                                Color(0xFF334155)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFF59E0B))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AD",
                                color = Color(0xFF0F172A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Break Block 2026",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sponsored Advertisement",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF064E3B))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isUnityBannerLoaded) "SPONSORED" else "AD",
                            color = Color(0xFF34D399),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
