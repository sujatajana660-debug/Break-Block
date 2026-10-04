package com.example.ads

/**
 * Advertising configuration holding Unity Ads Game ID, Organization Core ID, and Placement IDs.
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

    // User explicitly stated: "ami test mode no korechi" (I set test mode to false/no)
    var testMode: Boolean = false
}
