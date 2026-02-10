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

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val rgGender = view.findViewById<RadioGroup>(R.id.rgGender)
        val sbDistance = view.findViewById<SeekBar>(R.id.sbDistance)
        val tvDistanceValue = view.findViewById<TextView>(R.id.tvDistanceValue)
        val switchNotifications = view.findViewById<Switch>(R.id.switchNotifications)
        val btnBlocked = view.findViewById<LinearLayout>(R.id.btnBlocked)
        val btnLogout = view.findViewById<LinearLayout>(R.id.btnLogout)

        // Gender Selection
        rgGender.setOnCheckedChangeListener { _, checkedId ->
            val selection = when (checkedId) {
                R.id.rbMale -> "Men"
                R.id.rbFemale -> "Women"
                else -> "Everyone"
            }
            view.showSnackbar("Discovery preference set to: $selection")
        }

        // Distance Slider
        sbDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDistanceValue.text = "$progress km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                view.showSnackbar("Maximum distance set to ${seekBar?.progress} km")
            }
        })

        // Notifications
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "enabled" else "disabled"
            view.showSnackbar("Notifications $status")
        }

        // Blocked Users
        btnBlocked.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BlockedUsersFragment())
                .addToBackStack(null)
                .commit()
        }

        // Logout
        btnLogout.setOnClickListener {
            view.showSnackbar("Logging out...")
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        return view
    }
}
