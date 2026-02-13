package com.vibe.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.vibe.R
import com.vibe.utils.showSnackbar
import androidx.appcompat.widget.TooltipCompat

class DiscoverFragment : Fragment() {

    data class UiUser(
        val name: String, 
        val age: Int, 
        var distance: String, 
        val bio: String,
        val color: Int = Color.DKGRAY,
        val isOnline: Boolean = false,
        var isMatched: Boolean = false,
        val isPending: Boolean = false,
        val countryCode: String = "US",
        val city: String = "New York",
        val country: String = "USA",
        val lat: Double = 0.0,
        val lon: Double = 0.0
    )

    private lateinit var rvDiscoverFeed: RecyclerView
    private val feedList = mutableListOf<UiUser>()
    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || 
            result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            updateDistances()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)
        
        rvDiscoverFeed = view.findViewById(R.id.rvDiscoverFeed)
        rvDiscoverFeed.layoutManager = LinearLayoutManager(context)
        rvDiscoverFeed.adapter = FeedAdapter(feedList) { user, action ->
            handleAction(user, action)
        }
        
        loadUsers()
        
        // Request Location
        permissionLauncher.launch(arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        // Load Banners
        val adViewSmart = view.findViewById<AdView>(R.id.adViewSmart)
        val adViewLarge = view.findViewById<AdView>(R.id.adViewLarge)
        val bannerRequest = AdRequest.Builder().build()
        adViewSmart.loadAd(bannerRequest)
        adViewLarge.loadAd(bannerRequest)

        val btnSearch = view.findViewById<ImageView>(R.id.btnSearch)
        TooltipCompat.setTooltipText(btnSearch, "Coming Soon")
        btnSearch.setOnClickListener {
            // view.showSnackbar("Coming Soon")
        }

        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadUsers()
            swipeRefresh.isRefreshing = false
        }

        return view
    }

    private fun updateDistances() {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val locationManager = requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER) 
            ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            
        location?.let { myLoc ->
            feedList.forEach { user ->
                if (user.lat != 0.0 && user.lon != 0.0) {
                    val userLoc = android.location.Location("user")
                    userLoc.latitude = user.lat
                    userLoc.longitude = user.lon
                    val distInMeters = myLoc.distanceTo(userLoc)
                    user.distance = String.format("%.1f km", distInMeters / 1000)
                }
            }
            rvDiscoverFeed.adapter?.notifyDataSetChanged()
        }
    }

    private fun handleAction(user: UiUser, action: String) {
        when(action) {
            "like" -> {
                user.isMatched = !user.isMatched
                rvDiscoverFeed.adapter?.notifyDataSetChanged()
            }
            "open" -> openProfileDetail(user)
        }
    }

    private fun openProfileDetail(user: UiUser) {
        val fragment = ProfileDetailFragment.newInstance(
            user.name, user.age, user.distance, user.bio, user.countryCode, user.isOnline, user.city, user.country
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadUsers() {
        feedList.clear()
        // Simulate real locations (near NYC approx 40.7, -74.0)
        // isMatched = false for potential matches
        feedList.add(UiUser("Katina", 29, "6.0 km", "My name is Katina, call me. I'm 29 and I love adventure.", Color.parseColor("#E91E63"), isOnline = true, isMatched = false, countryCode = "US", city = "Manhattan", country = "USA", lat = 40.7128, lon = -74.0060))
        feedList.add(UiUser("Emily", 23, "3.8 km", "Isn't online dating a blast? Okay, maybe it can be challenging at times... but let's make it fun!", Color.parseColor("#9C27B0"), isOnline = false, isMatched = false, countryCode = "GB", city = "London", country = "UK", lat = 40.7200, lon = -74.0100))
        feedList.add(UiUser("Robert", 30, "2.0 km", "Have a good day! Looking for someone to share it with.", Color.parseColor("#2196F3"), isOnline = true, isMatched = true, countryCode = "CA", city = "Toronto", country = "Canada", lat = 40.7300, lon = -74.0200)) // Matched example
        feedList.add(UiUser("Gloria", 20, "8.1 km", "Oh, it's amazing. I love art and music.", Color.parseColor("#FF9800"), isOnline = false, isMatched = false, isPending = true, countryCode = "FR", city = "Paris", country = "France", lat = 40.7400, lon = -74.0300)) // Pending example
        feedList.add(UiUser("Bruce", 28, "12 km", "You look so beautiful. Let's grab coffee?", Color.parseColor("#4CAF50"), isOnline = true, isMatched = false, countryCode = "DE", city = "Berlin", country = "Germany", lat = 40.7500, lon = -74.0400))
        feedList.add(UiUser("Leslie", 24, "5.5 km", "Love youth. Live life to the fullest.", Color.parseColor("#673AB7"), isOnline = false, isMatched = false, countryCode = "JP", city = "Tokyo", country = "Japan", lat = 40.7600, lon = -74.0500))
        feedList.add(UiUser("Colleen", 26, "4.2 km", "Maybe tomorrow? Or maybe today!", Color.parseColor("#009688"), isOnline = true, isMatched = false, countryCode = "IN", city = "Kolkata", country = "India", lat = 40.7700, lon = -74.0600))
        
        // Don't filter out matched users anymore as we need to show different icons
        // val filteredList = feedList.filter { !it.isMatched }
        // feedList.clear()
        // feedList.addAll(filteredList)
        
        rvDiscoverFeed.adapter?.notifyDataSetChanged()
    }

    class FeedAdapter(
        private val users: List<UiUser>,
        private val onAction: (UiUser, String) -> Unit
    ) : RecyclerView.Adapter<FeedAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvLocation: TextView = view.findViewById(R.id.tvLocation)
            val tvBio: TextView = view.findViewById(R.id.tvBio)
            val ivProfile: ImageView = view.findViewById(R.id.ivProfile)
            val layoutOnline: View = view.findViewById(R.id.layoutOnline)
            val tvFlag: TextView = view.findViewById(R.id.tvFlag)
            
            val btnLike: View = view.findViewById(R.id.btnLike)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feed_profile, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.tvName.text = "${user.name}, ${user.age}"
            holder.tvLocation.text = "${user.distance} away"
            holder.tvBio.text = user.bio
            holder.ivProfile.setBackgroundColor(user.color)
            
            holder.layoutOnline.visibility = if (user.isOnline) View.VISIBLE else View.GONE
            
            // Set flag emoji
            holder.tvFlag.text = getFlagEmoji(user.countryCode)

            val btnLikeImage = holder.btnLike as android.widget.ImageButton
            if (user.isMatched) {
                btnLikeImage.setImageResource(R.drawable.ic_heart_filled)
                btnLikeImage.setColorFilter(null) // White filled
            } else {
                btnLikeImage.setImageResource(R.drawable.ic_heart_outline)
                btnLikeImage.setColorFilter(null) // White outline
            }

            holder.btnLike.setOnClickListener { onAction(user, "like") }
            holder.itemView.setOnClickListener { onAction(user, "open") }
        }
        
        private fun getFlagEmoji(countryCode: String): String {
            val flagOffset = 0x1F1E6
            val asciiOffset = 0x41
            if (countryCode.length != 2) return "🌍"
            val firstChar = Character.codePointAt(countryCode, 0) - asciiOffset + flagOffset
            val secondChar = Character.codePointAt(countryCode, 1) - asciiOffset + flagOffset
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }

        override fun getItemCount() = users.size
    }
}