# Video Editor ProGuard Rules

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep VideoEngine
-keep class com.videoeditor.app.core.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
