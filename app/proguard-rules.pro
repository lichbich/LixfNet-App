# Android & Jetpack Compose rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep data models used with Gson serialization
-keep class com.lixfnet.app.data.model.** { *; }
-keep class com.lixfnet.app.data.recommendation.** { *; }
-keep class com.lixfnet.app.data.update.** { *; }

# Gson specific rules
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes *Annotation*
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# BCrypt
-keep class org.mindrot.jbcrypt.** { *; }
