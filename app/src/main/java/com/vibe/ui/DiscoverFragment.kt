package com.vibe.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.R

class DiscoverFragment : Fragment() {

    data class UiUser(
        val name: String, 
        val age: Int, 
        val distance: String, 
        val bio: String,
        val color: Int = Color.DKGRAY
    )

    private lateinit var rvDiscoverFeed: RecyclerView
    private val feedList = mutableListOf<UiUser>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_discover, container, false)
        
        rvDiscoverFeed = view.findViewById(R.id.rvDiscoverFeed)
        rvDiscoverFeed.layoutManager = LinearLayoutManager(context)
        rvDiscoverFeed.adapter = FeedAdapter(feedList) { user, action ->
            handleAction(user, action)
        }
        
        loadUsers()
        
        return view
    }

    private fun handleAction(user: UiUser, action: String) {
        when(action) {
            "like" -> Toast.makeText(context, "Liked ${user.name}", Toast.LENGTH_SHORT).show()
            "pass" -> Toast.makeText(context, "Passed ${user.name}", Toast.LENGTH_SHORT).show()
            "super" -> Toast.makeText(context, "Super Liked ${user.name}!", Toast.LENGTH_SHORT).show()
            "open" -> openProfileDetail(user)
        }
    }

    private fun openProfileDetail(user: UiUser) {
        val fragment = ProfileDetailFragment.newInstance(
            user.name, user.age, user.distance, user.bio
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun loadUsers() {
        feedList.clear()
        feedList.add(UiUser("Katina", 29, "6.0 km", "My name is Katina, call me. I'm 29 and I love adventure.", Color.parseColor("#E91E63")))
        feedList.add(UiUser("Emily", 23, "3.8 km", "Isn't online dating a blast? Okay, maybe it can be challenging at times... but let's make it fun!", Color.parseColor("#9C27B0")))
        feedList.add(UiUser("Robert", 30, "2.0 km", "Have a good day! Looking for someone to share it with.", Color.parseColor("#2196F3")))
        feedList.add(UiUser("Gloria", 20, "8.1 km", "Oh, it's amazing. I love art and music.", Color.parseColor("#FF9800")))
        feedList.add(UiUser("Bruce", 28, "12 km", "You look so beautiful. Let's grab coffee?", Color.parseColor("#4CAF50")))
        feedList.add(UiUser("Leslie", 24, "5.5 km", "Love youth. Live life to the fullest.", Color.parseColor("#673AB7")))
        feedList.add(UiUser("Colleen", 26, "4.2 km", "Maybe tomorrow? Or maybe today!", Color.parseColor("#009688")))
        
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
            val ivVerified: ImageView = view.findViewById(R.id.ivVerified)
            
            val btnLike: View = view.findViewById(R.id.btnLike)
            val btnPass: View = view.findViewById(R.id.btnPass)
            val btnSuperLike: View = view.findViewById(R.id.btnSuperLike)
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
            
            // Random verified status for demo
            holder.ivVerified.visibility = if (position % 3 == 0) View.VISIBLE else View.GONE

            holder.btnLike.setOnClickListener { onAction(user, "like") }
            holder.btnPass.setOnClickListener { onAction(user, "pass") }
            holder.btnSuperLike.setOnClickListener { onAction(user, "super") }
            holder.itemView.setOnClickListener { onAction(user, "open") }
        }

        override fun getItemCount() = users.size
    }
}