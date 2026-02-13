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
        val avatarUrl: String? = null,
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
        // Using real Indian/Bengali female user data matching database population
        feedList.add(UiUser("Priya", 22, "1.2 km", "Lover of music and travel. Bengali girl living in Kolkata.", "https://images.unsplash.com/photo-1589156229687-496a31ad1d1f?auto=format&fit=crop&q=80&w=1000", Color.parseColor("#E91E63"), isOnline = true, isMatched = false, countryCode = "IN", city = "Kolkata", country = "India", lat = 22.5726, lon = 88.3639))
        feedList.add(UiUser("Anjali", 24, "3.8 km", "Yoga enthusiast and foodie from Mumbai.", "https://images.unsplash.com/photo-1614283233556-f35b0c801ef1?auto=format&fit=crop&q=80&w=1000", Color.parseColor("#9C27B0"), isOnline = true, isMatched = false, countryCode = "IN", city = "Mumbai", country = "India", lat = 19.0760, lon = 72.8777))
        feedList.add(UiUser("Sneha", 21, "2.5 km", "Art and culture lover. From beautiful Bengal.", "https://images.unsplash.com/photo-1594744803329-e58b31de3957?auto=format&fit=crop&q=80&w=1000", Color.parseColor("#2196F3"), isOnline = true, isMatched = false, countryCode = "IN", city = "Kolkata", country = "India", lat = 22.5726, lon = 88.3639))
        feedList.add(UiUser("Riya", 23, "5.0 km", "Dance is my passion. Let's connect!", "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&q=80&w=1000", Color.parseColor("#FF9800"), isOnline = true, isMatched = false, countryCode = "IN", city = "Ahmedabad", country = "India", lat = 23.0225, lon = 72.5714))
        feedList.add(UiUser("Ishani", 25, "4.2 km", "Professional photographer and dreamer.", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=1000", Color.parseColor("#4CAF50"), isOnline = true, isMatched = false, countryCode = "IN", city = "Kolkata", country = "India", lat = 22.5726, lon = 88.3639))
        
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
            
            // Load real image with Glide
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(user.avatarUrl)
                .placeholder(android.graphics.drawable.ColorDrawable(user.color))
                .error(android.graphics.drawable.ColorDrawable(user.color))
                .into(holder.ivProfile)
            
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