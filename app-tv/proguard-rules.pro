# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class dev.jausc.myflix.**$$serializer { *; }
-keepclassmembers class dev.jausc.myflix.** {
    *** Companion;
}
-keepclasseswithmembers class dev.jausc.myflix.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Media3/ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Jellyfin FFmpeg decoder extension
-keep class androidx.media3.decoder.ffmpeg.** { *; }
-keep class org.jellyfin.media3.** { *; }
-dontwarn org.jellyfin.media3.**

# MPV native libraries
-keep class is.xyz.mpv.** { *; }
-keepclassmembers class is.xyz.mpv.MPVLib {
    native <methods>;
    static <methods>;
}

# Keep Application class
-keep class dev.jausc.myflix.tv.MyFlixApp { *; }

# Keep all activities
-keep class dev.jausc.myflix.tv.MainActivity { *; }

# OkHttp (used by Ktor and Coil)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
