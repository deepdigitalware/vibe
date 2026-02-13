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

class MatchesFragment : Fragment() {

    private lateinit var rvMatches: RecyclerView

    data class MatchProfile(
        val name: String, 
        val age: Int, 
        val imageRes: Int,
        val isOnline: Boolean = false,
        val isVerified: Boolean = false,
        val countryCode: String = "US",
        val city: String = "New York",
        val country: String = "USA",
        val lastActive: Long = System.currentTimeMillis(),
        val interactions: Int = 0
    )

    private val matches = mutableListOf(
        MatchProfile("Jessica", 24, R.drawable.ic_profile, isOnline = true, isVerified = true, countryCode = "US", city = "New York", country = "USA", lastActive = System.currentTimeMillis(), interactions = 50),
        MatchProfile("Sophie", 22, R.drawable.ic_profile, isOnline = false, isVerified = false, countryCode = "FR", city = "Paris", country = "France", lastActive = System.currentTimeMillis() - 3600000, interactions = 10),
        MatchProfile("Amanda", 25, R.drawable.ic_profile, isOnline = true, isVerified = true, countryCode = "CA", city = "Toronto", country = "Canada", lastActive = System.currentTimeMillis() - 60000, interactions = 30),
        MatchProfile("Emily", 23, R.drawable.ic_profile, isOnline = false, isVerified = true, countryCode = "GB", city = "London", country = "UK", lastActive = System.currentTimeMillis() - 86400000, interactions = 5),
        MatchProfile("Sarah", 26, R.drawable.ic_profile, isOnline = true, isVerified = false, countryCode = "AU", city = "Sydney", country = "Australia", lastActive = System.currentTimeMillis() - 120000, interactions = 20),
        MatchProfile("Olivia", 21, R.drawable.ic_profile, isOnline = false, isVerified = true, countryCode = "DE", city = "Berlin", country = "Germany", lastActive = System.currentTimeMillis() - 1800000, interactions = 15)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_matches, container, false)
        
        rvMatches = view.findViewById(R.id.rvMatches)
        
        // Sort Matches: Online > Recent Activity > Interactions
        matches.sortWith(compareByDescending<MatchProfile> { it.isOnline }
            .thenByDescending { it.lastActive }
            .thenByDescending { it.interactions })

        val adapter = MatchesAdapter(matches) { profile ->
            val fragment = ProfileDetailFragment.newInstance(
                profile.name, profile.age, "2 km", "Bio loading...", profile.countryCode, profile.isOnline, profile.city, profile.country
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
        rvMatches.layoutManager = GridLayoutManager(context, 2)
        rvMatches.adapter = adapter

        // Load Banners
        val adViewSmart = view.findViewById<AdView>(R.id.adViewSmart)
        val adViewLarge = view.findViewById<AdView>(R.id.adViewLarge)
        val bannerRequest = AdRequest.Builder().build()
        adViewSmart.loadAd(bannerRequest)
        adViewLarge.loadAd(bannerRequest)

        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            // Simulate refresh
            matches.shuffle()
            adapter.notifyDataSetChanged()
            swipeRefresh.isRefreshing = false
        }

        return view
    }

    class MatchesAdapter(
        private val list: MutableList<MatchProfile>,
        private val onClick: (MatchProfile) -> Unit
    ) : RecyclerView.Adapter<MatchesAdapter.MatchVH>() {
        
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
            holder.tvNameAge.text = "${item.name}, ${item.age}"
            // holder.ivProfile.setImageResource(item.imageRes)
            
            holder.ivOnline.visibility = if (item.isOnline) View.VISIBLE else View.GONE
            holder.tvFlag.text = getFlagEmoji(item.countryCode)
            
            holder.itemView.setOnClickListener { onClick(item) }
            
            holder.ivOptions.setOnClickListener {
                val popup = PopupMenu(holder.itemView.context, holder.ivOptions)
                popup.menu.add("Unmatch")
                popup.setOnMenuItemClickListener { menuItem ->
                    if (menuItem.title == "Unmatch") {
                        val pos = holder.adapterPosition
                        if (pos != RecyclerView.NO_POSITION) {
                            list.removeAt(pos)
                            notifyItemRemoved(pos)
                        }
                        true
                    } else {
                        false
                    }
                }
                popup.show()
            }
        }

        override fun getItemCount() = list.size

        private fun getFlagEmoji(countryCode: String): String {
            val flagOffset = 0x1F1E6
            val asciiOffset = 0x41
            if (countryCode.length != 2) return "🌍"
            val firstChar = Character.codePointAt(countryCode, 0) - asciiOffset + flagOffset
            val secondChar = Character.codePointAt(countryCode, 1) - asciiOffset + flagOffset
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }
    }
}
