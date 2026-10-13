# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class org.xmsleep.app.**$$serializer { *; }
-keepclassmembers class org.xmsleep.app.** {
    *** Companion;
}
-keepclasseswithmembers class org.xmsleep.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes used in serialization
-keep @kotlinx.serialization.Serializable class org.xmsleep.app.** { *; }

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# ---- Gson 目标数据类（字段名必须与 JSON 键一致，禁止混淆/裁剪）----
# 说明：Gson 通过反射按“字段名”映射 JSON，混淆字段名会导致解析静默失败。
# 仅对确认为 Gson 序列化/反序列化目标的类做精确 keep，其余自有类保持可混淆。
# 注意：带 @kotlinx.serialization.Serializable 的类（如 quote.Quote / QuotesManifest /
#      LocalQuote / update.GitHubRelease）已由上方 “Keep data classes used in serialization”
#      规则完整保留，此处不重复声明。
# 音频清单 assets/sounds_remote.json 与本地缓存（AudioResourceManager / RemoteAudioLoader）
-keep class org.xmsleep.app.audio.model.** { *; }
# 每日一言历史（QuoteManager：gson.toJson / fromJson）
-keep class org.xmsleep.app.quote.QuoteHistory { *; }
-keep class org.xmsleep.app.quote.QuoteHistoryItem { *; }
# 听声日记（ListeningDiaryManager：gson.toJson / fromJson）
-keep class org.xmsleep.app.diary.DiaryEntry { *; }
-keep class org.xmsleep.app.diary.DiaryHistory { *; }
# 天气→声音映射（WeatherSoundMapper：TypeToken<List<SoundMapping>> 与 enum WeatherType）
-keep class org.xmsleep.app.weather.WeatherSoundMapper$SoundMapping { *; }
-keep class org.xmsleep.app.weather.WeatherType { *; }
# 冥想清单（BreathingListScreen / MeditationPlayerScreen：TypeToken<MeditationManifest>）
-keep class org.xmsleep.app.ui.meditation.MeditationManifest { *; }
-keep class org.xmsleep.app.ui.meditation.MeditationCategory { *; }
-keep class org.xmsleep.app.ui.meditation.MeditationSession { *; }

# 自有枚举的常量名会被 .name 用于资源名查找(getIdentifier)与持久化(SharedPreferences)：
# 仅保留枚举常量/成员名、允许类名混淆，避免混淆后查名/存档跨版本失效。
-keepclassmembers enum org.xmsleep.app.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep BuildConfig
-keep class **.BuildConfig { *; }

# Keep application class
-keep class org.xmsleep.app.MainActivity { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ExoPlayer/Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# MaterialKolor
-keep class com.materialkolor.** { *; }
-dontwarn com.materialkolor.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Firebase Analytics / Crashlytics
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

