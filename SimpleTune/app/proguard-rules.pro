# R8 / ProGuard rules for VibeOn

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.vibeon.music.**$$serializer { *; }
-keepclassmembers class com.vibeon.music.** { *** Companion; }
-keepclasseswithmembers class com.vibeon.music.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit-style reflection not used; but Ktor/OkHttp need keep rules.
-keepattributes Signature
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Media3
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# Room
-keep class com.vibeon.music.data.local.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }