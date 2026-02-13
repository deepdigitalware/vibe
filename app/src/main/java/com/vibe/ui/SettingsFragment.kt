package com.vibe.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.vibe.R
import com.vibe.utils.showSnackbar
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val rgGender = view.findViewById<RadioGroup>(R.id.rgGender)
        val sbDistance = view.findViewById<SeekBar>(R.id.sbDistance)
        val tvDistanceValue = view.findViewById<TextView>(R.id.tvDistanceValue)
        val switchNotifications = view.findViewById<Switch>(R.id.switchNotifications)
        val btnBlocked = view.findViewById<LinearLayout>(R.id.btnBlocked)
        val btnLogout = view.findViewById<LinearLayout>(R.id.btnLogout)
        val btnShareApp = view.findViewById<LinearLayout>(R.id.btnShareApp)
        val btnGetInTouch = view.findViewById<LinearLayout>(R.id.btnGetInTouch)

        // Load Preferences
        val prefs = requireContext().getSharedPreferences("vibe_prefs", android.content.Context.MODE_PRIVATE)
        val savedShowMe = prefs.getString("pref_show_me", "Everyone")
        
        when (savedShowMe) {
            "Men" -> rgGender.check(R.id.rbMale)
            "Women" -> rgGender.check(R.id.rbFemale)
            else -> rgGender.check(R.id.rbBoth)
        }

        // Gender Selection
        rgGender.setOnCheckedChangeListener { _, checkedId ->
            val selection = when (checkedId) {
                R.id.rbMale -> "Men"
                R.id.rbFemale -> "Women"
                else -> "Everyone"
            }
            prefs.edit().putString("pref_show_me", selection).apply()
            // view.showSnackbar("Discovery preference set to: $selection")
        }

        // Distance Slider
        sbDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDistanceValue.text = "$progress km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // view.showSnackbar("Maximum distance set to ${seekBar?.progress} km")
            }
        })

        // Notifications
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "enabled" else "disabled"
            // view.showSnackbar("Notifications $status")
        }

        // Blocked Users
        btnBlocked.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BlockedUsersFragment())
                .addToBackStack(null)
                .commit()
        }
        
        // Share App
        btnShareApp.setOnClickListener {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Hey! Check out Vibe, the coolest new dating app! Download it now.")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Vibe via")
            startActivity(shareIntent)
        }
        
        // Get In Touch
        btnGetInTouch.setOnClickListener {
            val url = "https://wa.me/917003614633"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(url)
            startActivity(intent)
        }

        // Logout
        btnLogout.setOnClickListener {
            view.showSnackbar("Logging out...")
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Load Banners
        val adViewSmart = view.findViewById<AdView>(R.id.adViewSmart)
        val adViewLarge = view.findViewById<AdView>(R.id.adViewLarge)
        val bannerRequest = AdRequest.Builder().build()
        adViewSmart.loadAd(bannerRequest)
        adViewLarge.loadAd(bannerRequest)

        return view
    }
}
