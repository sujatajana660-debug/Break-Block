# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Unity Ads SDK ProGuard Keep Rules
-keep class com.unity3d.ads.** { *; }
-keep interface com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-keep interface com.unity3d.services.** { *; }
-dontwarn com.unity3d.services.**
-dontwarn com.unity3d.ads.**

# Keep models and entities
-keep class com.example.model.** { *; }
-keep class com.example.data.** { *; }
-keep class com.example.ads.** { *; }
