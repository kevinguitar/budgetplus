# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep remote data classes
-keep class com.kevlina.budgetplus.core.data.remote.** { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable
# Optional: Keep custom exceptions.
-keep public class * extends java.lang.Exception

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# Firebase auth
# https://github.com/firebase/firebase-android-sdk/issues/2124#issuecomment-920922929
-keep public class com.google.firebase.** {*;}
-keep class com.google.android.gms.internal.** {*;}
-keepclasseswithmembers class com.google.firebase.FirebaseException

-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# Keep WorkManager / Room / App Startup generated classes and workers for androidx.startup.InitializationProvider,
# this reulted in a crash in AGP9.
-keep class androidx.work.impl.** { *; }
-keep class androidx.work.** { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.db.WorkDatabase_Impl { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keepclassmembers class * {
@androidx.room.* <fields>;
}
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.startup.** { *; }
-keep class androidx.startup.InitializationProvider { *; }
-keep class * implements androidx.startup.Initializer { *; }

# Required rules for r8
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
-dontwarn android.media.LoudnessCodecController
-dontwarn com.facebook.infer.annotation.Nullsafe
-dontwarn com.facebook.infer.annotation.Nullsafe$Mode
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
-dontwarn org.apiguardian.api.API$Status
-dontwarn org.apiguardian.api.API