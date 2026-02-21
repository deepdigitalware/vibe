package com.vibe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.vibe.api.ApiClient
import com.vibe.api.SessionManager
import com.vibe.api.User
import com.vibe.utils.showSnackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FavouriteFragment : Fragment() {

    private lateinit var rvMatches: RecyclerView
    private val favouriteUsers = mutableListOf<User>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_matches, container, false)
        
        rvMatches = view.findViewById(R.id.rvMatches)
        
        val adapter = FavouriteAdapter(favouriteUsers) { user ->
            val fragment = ProfileDetailFragment.newInstance(
                user.uid, user.name, 20, "Bio loading...", "IN", true, "Kolkata", "India"
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
        rvMatches.layoutManager = GridLayoutManager(context, 2)
        rvMatches.adapter = adapter

        loadFavourites()

        // Load Banners
        val adViewSmart = view.findViewById<AdView>(R.id.adViewSmart)
        val adViewLarge = view.findViewById<AdView>(R.id.adViewLarge)
        val bannerRequest = AdRequest.Builder().build()
        adViewSmart.loadAd(bannerRequest)
        adViewLarge.loadAd(bannerRequest)

        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            loadFavourites()
            swipeRefresh.isRefreshing = false
        }

        return view
    }

    private fun loadFavourites() {
        val userId = SessionManager.getUserId() ?: return
        ApiClient.api.getFavourites(userId).enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (response.isSuccessful) {
                    favouriteUsers.clear()
                    response.body()?.let { favouriteUsers.addAll(it) }
                    rvMatches.adapter?.notifyDataSetChanged()
                    
                    val tvSubtitle = view?.findViewById<TextView>(R.id.tvSubtitle)
                    if (favouriteUsers.isEmpty()) {
                        tvSubtitle?.text = "No favourites yet. Start discovering!"
                    } else {
                        tvSubtitle?.text = "People you've added to favourites"
                    }
                }
            }
            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                view?.showSnackbar("Failed to load favourites")
            }
        })
    }

    class FavouriteAdapter(
        private val list: MutableList<User>,
        private val onClick: (User) -> Unit
    ) : RecyclerView.Adapter<FavouriteAdapter.MatchVH>() {
        
        class MatchVH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNameAge: TextView = v.findViewById(R.id.tvNameAge)
            val ivProfile: ImageView = v.findViewById(R.id.ivProfile)
            val ivOnline: ImageView = v.findViewById(R.id.ivOnline)
            val tvFlag: TextView = v.findViewById(R.id.tvFlag)
            val ivOptions: ImageView = v.findViewById(R.id.ivOptions)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchVH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_match_card, parent, false)
            return MatchVH(view)
        }

        override fun onBindViewHolder(holder: MatchVH, position: Int) {
            val item = list[position]
            holder.tvNameAge.text = item.name
            
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(item.avatar)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(holder.ivProfile)
            
            holder.ivOnline.visibility = View.GONE // TODO: Get online status from API
            holder.tvFlag.text = "🇮🇳" // Default
            
            holder.itemView.setOnClickListener { onClick(item) }
            
            holder.ivOptions.setOnClickListener {
                val popup = PopupMenu(holder.itemView.context, holder.ivOptions)
                popup.menu.add("Remove from Favourite")
                popup.setOnMenuItemClickListener { menuItem ->
                    if (menuItem.title == "Remove from Favourite") {
                        removeFromFavourites(item, holder.adapterPosition)
                        true
                    } else {
                        false
                    }
                }
                popup.show()
            }
        }

        private fun removeFromFavourites(user: User, position: Int) {
            val currentUserId = SessionManager.getUserId() ?: return
            ApiClient.api.toggleFavourite(com.vibe.api.ToggleFavouriteRequest(currentUserId, user.uid))
                .enqueue(object : Callback<com.vibe.api.ToggleFavouriteResponse> {
                    override fun onResponse(call: Call<com.vibe.api.ToggleFavouriteResponse>, response: Response<com.vibe.api.ToggleFavouriteResponse>) {
                        if (response.isSuccessful && response.body()?.action == "removed") {
                            list.removeAt(position)
                            notifyItemRemoved(position)
                        }
                    }
                    override fun onFailure(call: Call<com.vibe.api.ToggleFavouriteResponse>, t: Throwable) {}
                })
        }

        override fun getItemCount() = list.size
    }
}
