package com.vibe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.R

class ChatsFragment : Fragment() {

    private lateinit var rvChatList: RecyclerView
    private lateinit var tvEmpty: TextView

    data class ChatSummary(
        val id: String,
        val name: String,
        val lastMessage: String,
        val time: String,
        val avatarRes: Int
    )

    private val chats = listOf(
        ChatSummary("1", "Katina", "Hey! How are you?", "10:30 AM", R.drawable.ic_profile),
        ChatSummary("2", "Robert", "The dark theme looks cool.", "Yesterday", R.drawable.ic_profile),
        ChatSummary("3", "Alice", "Let's meet up later.", "Mon", R.drawable.ic_profile)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)

        rvChatList = view.findViewById(R.id.rvChatList)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        if (chats.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvChatList.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvChatList.visibility = View.VISIBLE
            
            val adapter = ChatListAdapter(chats) { chat ->
                // Navigate to ChatDetailFragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, ChatDetailFragment())
                    .addToBackStack(null)
                    .commit()
            }
            rvChatList.layoutManager = LinearLayoutManager(context)
            rvChatList.adapter = adapter
        }

        return view
    }

    class ChatListAdapter(
        private val list: List<ChatSummary>,
        private val onClick: (ChatSummary) -> Unit
    ) : RecyclerView.Adapter<ChatListAdapter.ChatListVH>() {

        class ChatListVH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvName)
            val tvLastMessage: TextView = v.findViewById(R.id.tvLastMessage)
            val tvTime: TextView = v.findViewById(R.id.tvTime)
            val ivAvatar: ImageView = v.findViewById(R.id.ivAvatar)
            val root: View = v
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListVH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_row, parent, false)
            return ChatListVH(view)
        }

        override fun onBindViewHolder(holder: ChatListVH, position: Int) {
            val item = list[position]
            holder.tvName.text = item.name
            holder.tvLastMessage.text = item.lastMessage
            holder.tvTime.text = item.time
            // holder.ivAvatar.setImageResource(item.avatarRes) // Use default for now or load image
            
            holder.root.setOnClickListener {
                onClick(item)
            }
        }

        override fun getItemCount() = list.size
    }
}
