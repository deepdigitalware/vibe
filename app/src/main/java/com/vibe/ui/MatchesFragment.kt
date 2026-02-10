package com.vibe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.R

class MatchesFragment : Fragment() {

    private lateinit var rvMatches: RecyclerView

    data class MatchProfile(
        val name: String, 
        val age: Int, 
        val imageRes: Int,
        val isOnline: Boolean = false,
        val isVerified: Boolean = false,
        val countryCode: String = "US"
    )

    private val matches = listOf(
        MatchProfile("Jessica", 24, R.drawable.ic_profile, isOnline = true, isVerified = true),
        MatchProfile("Sophie", 22, R.drawable.ic_profile, isOnline = false, isVerified = false),
        MatchProfile("Amanda", 25, R.drawable.ic_profile, isOnline = true, isVerified = true),
        MatchProfile("Emily", 23, R.drawable.ic_profile, isOnline = false, isVerified = true),
        MatchProfile("Sarah", 26, R.drawable.ic_profile, isOnline = true, isVerified = false),
        MatchProfile("Olivia", 21, R.drawable.ic_profile, isOnline = false, isVerified = true)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_matches, container, false)
        
        rvMatches = view.findViewById(R.id.rvMatches)
        
        val adapter = MatchesAdapter(matches) { profile ->
            val fragment = ProfileDetailFragment.newInstance(
                profile.name, profile.age, "2 km", "Bio loading..."
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
        rvMatches.layoutManager = GridLayoutManager(context, 2)
        rvMatches.adapter = adapter

        return view
    }

    class MatchesAdapter(
        private val list: List<MatchProfile>,
        private val onClick: (MatchProfile) -> Unit
    ) : RecyclerView.Adapter<MatchesAdapter.MatchVH>() {
        
        class MatchVH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNameAge: TextView = v.findViewById(R.id.tvNameAge)
            val ivProfile: ImageView = v.findViewById(R.id.ivProfile)
            val ivVerified: ImageView = v.findViewById(R.id.ivVerified)
            val ivOnline: ImageView = v.findViewById(R.id.ivOnline)
            val ivFlag: ImageView = v.findViewById(R.id.ivFlag)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchVH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_match_card, parent, false)
            return MatchVH(view)
        }

        override fun onBindViewHolder(holder: MatchVH, position: Int) {
            val item = list[position]
            holder.tvNameAge.text = "${item.name}, ${item.age}"
            // holder.ivProfile.setImageResource(item.imageRes)
            
            holder.ivVerified.visibility = if (item.isVerified) View.VISIBLE else View.GONE
            holder.ivOnline.visibility = if (item.isOnline) View.VISIBLE else View.GONE
            
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = list.size
    }
}
