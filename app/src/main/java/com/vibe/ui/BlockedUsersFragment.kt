package com.vibe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.R

class BlockedUsersFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_blocked_users, container, false)
        val rvBlockedUsers = view.findViewById<RecyclerView>(R.id.rvBlockedUsers)
        rvBlockedUsers.layoutManager = LinearLayoutManager(context)
        
        val dummyBlockedUsers = mutableListOf("John Doe", "Jane Smith", "Bob Johnson", "Alice Brown")
        rvBlockedUsers.adapter = BlockedUserAdapter(dummyBlockedUsers)
        
        return view
    }
}