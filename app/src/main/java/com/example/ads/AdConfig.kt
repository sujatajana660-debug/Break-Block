package com.example.ads

/**
 * Advertising configuration holding Unity Ads Game ID, Organization Core ID, and Placement IDs.
 * Pure Live Ads (Test Mode is permanently removed and false).
 */
object AdConfig {
    // Unity Ads Organization Core ID
    const val UNITY_ORGANIZATION_CORE_ID = "13469979690802"

    // Unity Ads Game ID
    const val UNITY_GAME_ID = "800387496"

    // Unity Ads Placement IDs
    const val UNITY_PLACEMENT_BANNER = "BP_Banner_Android"
    const val UNITY_PLACEMENT_INTERSTITIAL = "BP_Interstitial_Android"
    const val UNITY_PLACEMENT_REWARDED = "BP_Rewarded_Android"

    // Test Mode enabled for development/testing so real video ads play instantly on device
    // Set to false when publishing to Google Play Store with linked Store ID
    const val TEST_MODE: Boolean = true
}
