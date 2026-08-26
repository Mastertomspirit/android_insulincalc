# ==============================================================================
# ProGuard & R8 Optimization and Obfuscation Rules
# Project: Insulin Calculator (network.spiritscorp.insulincalc)
# ==============================================================================

# Preserve line number information for readable stack traces in crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve generic signatures and runtime annotations for reflection and serialization
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# ==============================================================================
# Retrofit, OkHttp & Okio
# ==============================================================================
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ==============================================================================
# Moshi (JSON Serialization / Deserialization)
# ==============================================================================
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-dontwarn com.squareup.moshi.**

# ==============================================================================
# Room Database & SQLite
# ==============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ==============================================================================
# Domain Data Models, DTOs & AI Service Contracts
# ==============================================================================
-keep class network.spiritscorp.data.** { *; }
-keep class network.spiritscorp.ai.** { *; }
