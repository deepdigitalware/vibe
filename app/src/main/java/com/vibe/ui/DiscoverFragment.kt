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
import com.vibe.api.ApiClient
import com.vibe.api.SessionManager
import com.vibe.api.User
import com.vibe.api.ToggleFavouriteRequest
import com.vibe.api.ToggleFavouriteResponse
import com.vibe.utils.showSnackbar
import androidx.appcompat.widget.TooltipCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DiscoverFragment : Fragment() {

    private lateinit var rvDiscoverFeed: RecyclerView
    private val feedList = mutableListOf<User>()
    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || 
            result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // updateDistances() // Distance logic can be added later if needed
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

        val btnNotifications = view.findViewById<ImageView>(R.id.btnNotifications)
        btnNotifications.setOnClickListener {
            view.showSnackbar("Notifications Coming Soon")
        }

        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadUsers()
            swipeRefresh.isRefreshing = false
        }

        return view
    }

    private fun handleAction(user: User, action: String) {
        when(action) {
            "like" -> toggleFavourite(user)
            "open" -> openProfileDetail(user)
        }
    }

    private fun toggleFavourite(user: User) {
        val currentUserId = SessionManager.getUserId() ?: return
        ApiClient.api.toggleFavourite(ToggleFavouriteRequest(currentUserId, user.uid))
            .enqueue(object : Callback<ToggleFavouriteResponse> {
                override fun onResponse(call: Call<ToggleFavouriteResponse>, response: Response<ToggleFavouriteResponse>) {
                    if (response.isSuccessful) {
                        val action = response.body()?.action
                        if (action == "added") {
                            view?.showSnackbar("Added to Favourites")
                        } else {
                            view?.showSnackbar("Removed from Favourites")
                        }
                        loadUsers() // Refresh list to update heart icons
                    }
                }
                override fun onFailure(call: Call<ToggleFavouriteResponse>, t: Throwable) {
                    view?.showSnackbar("Failed to update favourite")
                }
            })
    }

    private fun openProfileDetail(user: User) {
        val fragment = ProfileDetailFragment.newInstance(
            user.uid, user.name, 20, user.bio, "IN", true, "Kolkata", "India"
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadUsers() {
        val userId = SessionManager.getUserId()
        ApiClient.api.discoverUsers(userId).enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (response.isSuccessful) {
                    feedList.clear()
                    response.body()?.let { feedList.addAll(it) }
                    rvDiscoverFeed.adapter?.notifyDataSetChanged()
                }
            }
            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                view?.showSnackbar("Failed to load users")
            }
        })
    }

    class FeedAdapter(
        private val users: List<User>,
        private val onAction: (User, String) -> Unit
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
            holder.tvName.text = user.name
            holder.tvLocation.text = "India"
            holder.tvBio.text = user.bio
            
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(user.avatar)
                .placeholder(android.graphics.drawable.ColorDrawable(Color.DKGRAY))
                .error(android.graphics.drawable.ColorDrawable(Color.DKGRAY))
                .into(holder.ivProfile)
            
            holder.layoutOnline.visibility = View.VISIBLE
            holder.tvFlag.text = "🇮🇳"

            val btnLikeImage = holder.btnLike as android.widget.ImageButton
            if (user.is_favourited) {
                btnLikeImage.setImageResource(R.drawable.ic_heart_filled)
                btnLikeImage.setColorFilter(Color.RED)
            } else {
                btnLikeImage.setImageResource(R.drawable.ic_heart_outline)
                btnLikeImage.setColorFilter(Color.WHITE)
            }

            holder.btnLike.setOnClickListener { onAction(user, "like") }
            holder.itemView.setOnClickListener { onAction(user, "open") }
        }
        
        override fun getItemCount() = users.size
    }
}
