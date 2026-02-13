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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import androidx.core.widget.ImageViewCompat
import java.util.Collections
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import com.vibe.BuildConfig
import com.google.firebase.auth.FirebaseAuth

class ChatsFragment : Fragment() {

    private lateinit var rvChatList: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var socket: Socket
    private val userId by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "guest" }

    data class ChatSummary(
        val id: String,
        val name: String,
        val lastMessage: String,
        val time: String,
        val avatarRes: Int,
        val lastChatTime: Long, // For sorting
        val unreadCount: Int = 0,
        val isLastMessageSentByMe: Boolean = false,
        val messageStatus: Int = 0 // 0: Sending, 1: Sent, 2: Delivered, 3: Read
    )

    private val chats = mutableListOf(
        ChatSummary("1", "Katina", "Hey! How are you?", "10:30 AM", R.drawable.ic_profile, System.currentTimeMillis() - 3600000, 2),
        ChatSummary("2", "Robert", "The dark theme looks cool.", "Yesterday", R.drawable.ic_profile, System.currentTimeMillis() - 86400000, 0, true, 3),
        ChatSummary("3", "Alice", "Let's meet up later.", "Mon", R.drawable.ic_profile, System.currentTimeMillis() - 172800000, 5),
        ChatSummary("4", "John", "Sent you the file.", "10:45 AM", R.drawable.ic_profile, System.currentTimeMillis() - 1800000, 0, true, 2)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)

        rvChatList = view.findViewById(R.id.rvChatList)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        // Sort by time descending
        chats.sortByDescending { it.lastChatTime }

        if (chats.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvChatList.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvChatList.visibility = View.VISIBLE
            
            val adapter = ChatListAdapter(chats) { chat ->
                // Reset unread count locally for UI feedback
                val index = chats.indexOf(chat)
                if (index != -1) {
                    val updatedChat = chat.copy(unreadCount = 0)
                    chats[index] = updatedChat
                    rvChatList.adapter?.notifyItemChanged(index)
                }
                
                // Navigate to ChatDetailFragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, ChatDetailFragment.newInstance(chat.name, "Online"))
                    .addToBackStack(null)
                    .commit()
            }
            rvChatList.layoutManager = LinearLayoutManager(context)
            rvChatList.adapter = adapter
        }

        // Load Banners
        val adViewSmart = view.findViewById<AdView>(R.id.adViewSmart)
        val adViewLarge = view.findViewById<AdView>(R.id.adViewLarge)
        val bannerRequest = AdRequest.Builder().build()
        adViewSmart.loadAd(bannerRequest)
        adViewLarge.loadAd(bannerRequest)

        initSocket()

        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            // Simulate refresh
            chats.sortByDescending { it.lastChatTime }
            rvChatList.adapter?.notifyDataSetChanged()
            swipeRefresh.isRefreshing = false
        }

        return view
    }

    private fun initSocket() {
        try {
            val options = IO.Options()
            options.query = "userId=$userId"
            socket = IO.socket(BuildConfig.CONFIG_BASE_URL, options)

            socket.on(Socket.EVENT_CONNECT) {
                // Connected to socket
            }

            socket.on("new_message") { args ->
                if (args.isNotEmpty()) {
                    val data = args[0] as JSONObject
                    val senderId = data.optString("senderId")
                    val senderName = data.optString("sender")
                    val msg = data.optString("message")

                    activity?.runOnUiThread {
                        val index = chats.indexOfFirst { it.name == senderName }
                        if (index != -1) {
                            val chat = chats[index]
                            val updatedChat = chat.copy(
                                lastMessage = msg,
                                unreadCount = chat.unreadCount + 1,
                                lastChatTime = System.currentTimeMillis(),
                                time = "Now"
                            )
                            chats.removeAt(index)
                            chats.add(0, updatedChat)
                            // rvChatList.adapter?.notifyDataSetChanged() // Full refresh is safer for reordering
                        } else {
                            // New chat summary
                            val newChat = ChatSummary(
                                id = senderId,
                                name = senderName,
                                lastMessage = msg,
                                time = "Now",
                                avatarRes = R.drawable.ic_profile,
                                lastChatTime = System.currentTimeMillis(),
                                unreadCount = 1
                            )
                            chats.add(0, newChat)
                        }
                        // Sort by time descending to ensure newest is always at top
                        chats.sortByDescending { it.lastChatTime }
                        rvChatList.adapter?.notifyDataSetChanged()
                    }
                }
            }

            socket.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::socket.isInitialized) {
            socket.disconnect()
            socket.off()
        }
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
            val tvBadge: TextView = v.findViewById(R.id.tvBadge)
            val ivStatus: ImageView = v.findViewById(R.id.ivStatus)
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
            
            // Unread Count Logic
            if (item.unreadCount > 0) {
                holder.tvBadge.visibility = View.VISIBLE
                holder.tvBadge.text = item.unreadCount.toString()
                holder.tvLastMessage.setTextColor(Color.WHITE)
                holder.tvLastMessage.setTypeface(null, Typeface.BOLD)
                holder.tvTime.setTextColor(Color.WHITE)
            } else {
                holder.tvBadge.visibility = View.GONE
                holder.tvLastMessage.setTextColor(Color.parseColor("#AAAAAA"))
                holder.tvLastMessage.setTypeface(null, Typeface.NORMAL)
                holder.tvTime.setTextColor(Color.parseColor("#AAAAAA"))
            }

            // Message Status Ticks Logic (Only if last message sent by me)
            if (item.isLastMessageSentByMe) {
                holder.ivStatus.visibility = View.VISIBLE
                when (item.messageStatus) {
                    0, 1 -> { // Sent
                        holder.ivStatus.setImageResource(R.drawable.ic_check)
                        ImageViewCompat.setImageTintList(holder.ivStatus, ColorStateList.valueOf(Color.GRAY))
                    }
                    2 -> { // Delivered
                        holder.ivStatus.setImageResource(R.drawable.ic_done_all)
                        ImageViewCompat.setImageTintList(holder.ivStatus, ColorStateList.valueOf(Color.GRAY))
                    }
                    3 -> { // Read
                        holder.ivStatus.setImageResource(R.drawable.ic_done_all)
                        ImageViewCompat.setImageTintList(holder.ivStatus, ColorStateList.valueOf(Color.parseColor("#2196F3")))
                    }
                }
            } else {
                holder.ivStatus.visibility = View.GONE
            }

            holder.root.setOnClickListener {
                onClick(item)
            }
        }

        override fun getItemCount() = list.size
    }
}
