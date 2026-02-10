package com.vibe.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Use Android 12+ Splash API
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Check SharedPreferences for persistent login state
        val prefs = getSharedPreferences("vibe_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        val next = try {
            // Initialize Firebase if not already done
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            
            val user = FirebaseAuth.getInstance().currentUser
            // Consider logged in if Firebase user exists OR we have persistent flag (for Guest mode)
            if (user != null || isLoggedIn) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }
        } catch (e: Exception) {
            LoginActivity::class.java
        }

        startActivity(Intent(this, next))
        finish()
    }
}
