# Standard Android rules
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Firebase & Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# AdMob
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Project specific
-keep class com.nextguidance.filesexplorer.filemanager.smartfiles.** { *; }
-keep class vocsy.ads.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-dontwarn kotlinx.coroutines.**

# Glide
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public class * extends com.bumptech.glide.module.LibraryGlideModule
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# Support/AndroidX libraries
-dontwarn androidx.**
-dontwarn android.support.**

# SLF4J (Added to fix R8 failure)
-dontwarn org.slf4j.**

# General rule for missing library classes if they don't affect runtime
-ignorewarnings