package com.vibe.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.vibe.R
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

class ChatDetailFragment : Fragment() {

    private lateinit var socket: Socket
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var rvChat: RecyclerView
    private lateinit var btnBack: View
    
    data class ChatMessage(val sender: String, val message: String, val isMe: Boolean)
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: MessageAdapter

    private val roomId = "lobby"
    private val userId by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "guest" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chat_detail, container, false)

        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        rvChat = view.findViewById(R.id.rvChat)
        btnBack = view.findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Demo Data
        if (messages.isEmpty()) {
            messages.add(ChatMessage("System", "Welcome to Vibe Chat!", false))
            messages.add(ChatMessage("Katina", "Hey! How are you?", false))
            messages.add(ChatMessage("You", "I'm good, thanks! checking out this new app.", true))
            messages.add(ChatMessage("Robert", "The dark theme looks cool.", false))
            messages.add(ChatMessage("You", "Yeah, much better than before.", true))
        }

        adapter = MessageAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(context)
        rvChat.adapter = adapter
        
        // Scroll to bottom
        rvChat.scrollToPosition(messages.size - 1)

        initSocket(view)

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                val data = JSONObject()
                data.put("roomId", roomId)
                data.put("message", msg)
                data.put("senderId", userId)
                data.put("timestamp", System.currentTimeMillis())
                
                // Emit
                socket.emit("send_message", data)
                
                // Optimistic update
                activity?.runOnUiThread {
                    messages.add(ChatMessage("You", msg, true))
                    adapter.notifyItemInserted(messages.size - 1)
                    rvChat.scrollToPosition(messages.size - 1)
                }
                
                etMessage.text.clear()
            }
        }

        return view
    }

    private fun initSocket(view: View) {
        try {
            val baseUrl = com.vibe.BuildConfig.CONFIG_BASE_URL
            socket = IO.socket(baseUrl) 
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            return
        }

        socket.on(Socket.EVENT_CONNECT) {
            activity?.runOnUiThread {
                if (isAdded) {
                    socket.emit("join_room", roomId)
                    // Snackbar.make(view, "Connected", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        socket.on("receive_message") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val msg = data.optString("message")
                val senderId = data.optString("senderId")
                
                // Prevent showing my own message twice if I did optimistic update
                if (senderId != userId) {
                    activity?.runOnUiThread {
                        messages.add(ChatMessage("User", msg, false))
                        adapter.notifyItemInserted(messages.size - 1)
                        rvChat.scrollToPosition(messages.size - 1)
                    }
                }
            }
        }

        socket.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::socket.isInitialized) {
            socket.disconnect()
            socket.off()
        }
    }

    class MessageAdapter(private val list: List<ChatMessage>) : RecyclerView.Adapter<MessageAdapter.ChatVH>() {
        
        class ChatVH(v: View) : RecyclerView.ViewHolder(v) {
            val tvSender: TextView = v.findViewById(R.id.tvSenderName)
            val tvMessage: TextView = v.findViewById(R.id.tvMessageContent)
            val card: CardView = v.findViewById(R.id.cardMessage)
            val parent: ConstraintLayout = v as ConstraintLayout
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatVH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
            return ChatVH(view)
        }

        override fun onBindViewHolder(holder: ChatVH, position: Int) {
            val item = list[position]
            holder.tvSender.text = item.sender
            holder.tvMessage.text = item.message
            
            val constraintSet = ConstraintSet()
            constraintSet.clone(holder.parent)

            if (item.isMe) {
                // Align Right
                constraintSet.setHorizontalBias(R.id.cardMessage, 1.0f)
                constraintSet.setHorizontalBias(R.id.tvSenderName, 1.0f)
                holder.card.setCardBackgroundColor(Color.parseColor("#7C4DFF")) 
            } else {
                // Align Left
                constraintSet.setHorizontalBias(R.id.cardMessage, 0.0f)
                constraintSet.setHorizontalBias(R.id.tvSenderName, 0.0f)
                holder.card.setCardBackgroundColor(Color.parseColor("#1E1E1E"))
            }
            
            constraintSet.applyTo(holder.parent)
        }

        override fun getItemCount() = list.size
    }
}
