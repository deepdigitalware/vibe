package com.vibe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.R
import com.vibe.utils.showSnackbar

class ProfileDetailFragment : Fragment() {

    private var name: String = "Unknown"
    private var age: Int = 20
    private var distance: String = "Unknown"
    private var bio: String = ""

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_AGE = "age"
        private const val ARG_DISTANCE = "distance"
        private const val ARG_BIO = "bio"

        fun newInstance(name: String, age: Int, distance: String, bio: String) =
            ProfileDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, name)
                    putInt(ARG_AGE, age)
                    putString(ARG_DISTANCE, distance)
                    putString(ARG_BIO, bio)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            name = it.getString(ARG_NAME, "Unknown")
            age = it.getInt(ARG_AGE, 20)
            distance = it.getString(ARG_DISTANCE, "Unknown")
            bio = it.getString(ARG_BIO, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile_detail, container, false)

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvDetail = view.findViewById<TextView>(R.id.tvDetail)
        val tvAbout = view.findViewById<TextView>(R.id.tvAbout)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val btnVideoCall = view.findViewById<Button>(R.id.btnVideoCall)
        val btnChat = view.findViewById<Button>(R.id.btnChat)
        val rvPhotos = view.findViewById<RecyclerView>(R.id.rvPhotos)

        tvName.text = name
        tvDetail.text = "$age • $distance away"
        tvAbout.text = bio.ifEmpty { "No bio available." }

        // Setup Photo Adapter
        val dummyPhotos = listOf(
            android.R.drawable.star_big_on,
            android.R.drawable.star_big_off,
            android.R.drawable.btn_star_big_on,
            android.R.drawable.btn_star_big_off
        )
        val photoAdapter = PhotoAdapter(dummyPhotos)
        rvPhotos.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvPhotos.adapter = photoAdapter

        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnVideoCall.setOnClickListener {
            // Start video call logic (deduct money)
            val intent = CallActivity.newIntent(requireContext(), role = "caller", roomId = "Room_${name.replace(" ", "_")}")
            startActivity(intent)
        }

        btnChat.setOnClickListener {
            view.showSnackbar("Opening chat with $name...")
            
            // Navigate to ChatDetailFragment
            // In a real app, we'd pass the user ID/Name to the fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ChatDetailFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}
