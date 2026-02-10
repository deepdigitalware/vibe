##############################
# Vibe App ProGuard/R8 rules #
##############################

# Keep Kotlin coroutines internals to avoid reflection issues
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Firebase classes used via reflection
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep WebRTC public API
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Keep AndroidX SplashScreen components
-keep class androidx.core.splashscreen.** { *; }

# Keep generated view binding and Activities
-keepclassmembers class ** {
    @android.view.View.OnClickListener <fields>;
}
-keep class **Binding { *; }
-keep class com.vibe.app.ui.** { *; }

# General: keep class names for model POJOs if any (Firestore)
-keepclassmembers class ** {
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Avoid warnings from common annotations
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**