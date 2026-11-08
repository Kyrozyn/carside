# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve line numbers so Play Console / deobfuscation can recover readable stacks.
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*

# MapLibre SDK Native & JNI bindings (Essential for AGP release builds with R8 minify)
-keep class org.maplibre.android.** { *; }
-keep interface org.maplibre.android.** { *; }
-dontwarn org.maplibre.android.**

# Google Play Services Maps & Location
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**

# Osmdroid (jika digunakan)
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Model and domain data classes
-keep class me.kyrozyn.carside.domain.model.** { *; }
-keep class me.kyrozyn.carside.data.** { *; }
