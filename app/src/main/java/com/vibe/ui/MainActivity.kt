package com.vibe.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.vibe.R
import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.vibe.utils.showSnackbar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkWelcomeBonus()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        
        // Load Ads
        try {
            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
            // findViewById<com.google.android.gms.ads.AdView>(R.id.adViewTop).loadAd(adRequest)
            // findViewById<com.google.android.gms.ads.AdView>(R.id.adViewBottom).loadAd(adRequest)
            
            // Hide AdViews to prevent layout spacing issues
            findViewById<com.google.android.gms.ads.AdView>(R.id.adViewTop).visibility = android.view.View.GONE
            findViewById<com.google.android.gms.ads.AdView>(R.id.adViewBottom).visibility = android.view.View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(DiscoverFragment())
            // findViewById<android.view.View>(android.R.id.content).showSnackbar("Vibe Updated: v1.1")
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_discover -> {
                    loadFragment(DiscoverFragment())
                    true
                }
                R.id.nav_matches -> {
                    loadFragment(FavouriteFragment())
                    true
                }
                R.id.nav_messages -> {
                    loadFragment(ChatsFragment()) // "Messages" tab uses ChatsFragment
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun setBottomNavVisibility(visible: Boolean) {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun checkWelcomeBonus() {
        val prefs = getSharedPreferences("vibe_prefs", Context.MODE_PRIVATE)
        val isBonusClaimed = prefs.getBoolean("is_bonus_claimed", false)

        if (!isBonusClaimed) {
            val dialogView = layoutInflater.inflate(R.layout.dialog_welcome_bonus, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()
                
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialogView.findViewById<android.widget.Button>(R.id.btnClaimBonus).setOnClickListener {
                val currentBalance = prefs.getFloat("wallet_balance", 0f)
                prefs.edit()
                    .putFloat("wallet_balance", currentBalance + 10f)
                    .putBoolean("is_bonus_claimed", true)
                    .apply()
                
                findViewById<android.view.View>(android.R.id.content).showSnackbar("Rs 10 added to your wallet! Enjoy!")
                dialog.dismiss()
            }
            
            dialog.show()
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
