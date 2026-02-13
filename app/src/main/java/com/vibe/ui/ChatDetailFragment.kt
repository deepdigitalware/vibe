package com.vibe.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.view.MotionEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.auth.FirebaseAuth
import com.vibe.R
import com.vibe.utils.showSnackbar
import gun0912.tedimagepicker.builder.TedImagePicker
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*

class ChatDetailFragment : Fragment() {

    private lateinit var socket: Socket
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var btnGallery: ImageButton
    private lateinit var btnEmoji: ImageButton
    private lateinit var rvChat: RecyclerView
    private lateinit var btnBack: View
    private lateinit var btnBlock: ImageView
    private lateinit var btnVideoCallHeader: View
    private lateinit var layoutBlocked: LinearLayout
    private lateinit var btnUnblock: android.widget.Button
    private lateinit var layoutInput: LinearLayout
    
    private var mediaRecorder: MediaRecorder? = null
    private var audioPath: String = ""
    
    // Header Views
    private lateinit var tvHeaderName: TextView
    private lateinit var tvHeaderStatus: TextView
    private lateinit var ivHeaderAvatar: ImageView

    data class ChatMessage(
        val sender: String, 
        val message: String, 
        val isMe: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val status: Int = 3, // 0: Sending, 1: Sent, 2: Delivered, 3: Read
        val type: String = "text", // "text", "image", "video", "audio", "file"
        val mediaUrl: String? = null
    )
    
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: MessageAdapter

    private fun openGalleryPicker() {
        TedImagePicker.with(requireContext())
            .start { uri ->
                val type = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
                val msgType = when {
                    type.startsWith("image") -> "image"
                    type.startsWith("video") -> "video"
                    type.startsWith("audio") -> "audio"
                    else -> "file"
                }
                
                val msgText = when(msgType) {
                    "image" -> "Photo"
                    "video" -> "Video"
                    "audio" -> "Audio"
                    else -> "File"
                }

                val message = ChatMessage(
                    sender = "You",
                    message = msgText,
                    isMe = true,
                    status = 0,
                    type = msgType,
                    mediaUrl = uri.toString()
                )
                messages.add(message)
                adapter.notifyItemInserted(messages.size - 1)
                rvChat.scrollToPosition(messages.size - 1)
                
                // In a real app, upload file here then emit socket event
            }
    }

    private val roomId = "lobby"
    private val userId by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "guest" }

    // Args
    private var chatName: String = "User"
    private var chatStatus: String = "Online"

    companion object {
        fun newInstance(name: String, status: String = "Online") = ChatDetailFragment().apply {
            arguments = Bundle().apply {
                putString("arg_name", name)
                putString("arg_status", status)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatName = it.getString("arg_name", "User")
            chatStatus = it.getString("arg_status", "Online")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chat_detail, container, false)

        initViews(view)
        setupListeners()
        setupHeader()
        setupRecyclerView()
        setupInput()
        
        initSocket(view)
        loadAds(view)

        val swipeRefresh = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            // Simulate refresh
            swipeRefresh.isRefreshing = false
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.setBottomNavVisibility(false)
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.setBottomNavVisibility(true)
    }

    private fun initViews(view: View) {
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        btnMic = view.findViewById(R.id.btnMic)
        btnGallery = view.findViewById(R.id.btnGallery)
        btnEmoji = view.findViewById(R.id.btnEmoji)
        rvChat = view.findViewById(R.id.rvChat)
        btnBack = view.findViewById(R.id.btnBack)
        btnBlock = view.findViewById(R.id.btnBlock)
        btnVideoCallHeader = view.findViewById(R.id.btnVideoCallHeader)
        layoutBlocked = view.findViewById(R.id.layoutBlocked)
        btnUnblock = view.findViewById(R.id.btnUnblock)
        layoutInput = view.findViewById(R.id.layoutInput)
        
        tvHeaderName = view.findViewById(R.id.tvHeaderName)
        tvHeaderStatus = view.findViewById(R.id.tvHeaderStatus)
        ivHeaderAvatar = view.findViewById(R.id.ivHeaderAvatar)
    }

    private fun setupHeader() {
        tvHeaderName.text = chatName
        tvHeaderStatus.text = chatStatus
        
        val openProfile = View.OnClickListener {
            // Open Profile Detail
            val fragment = ProfileDetailFragment.newInstance(
                chatName, 25, "Unknown", "Bio loading...", "US", true, "City", "Country"
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

        tvHeaderName.setOnClickListener(openProfile)
        tvHeaderStatus.setOnClickListener(openProfile)
        ivHeaderAvatar.setOnClickListener(openProfile)
        
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        btnBlock.setOnClickListener { 
            toggleBlockState(true)
        }
        
        btnUnblock.setOnClickListener {
            toggleBlockState(false)
        }
        
        btnVideoCallHeader.setOnClickListener {
            val intent = CallActivity.newIntent(requireContext(), role = "caller", roomId = roomId)
            startActivity(intent)
        }
    }

    private fun toggleBlockState(blocked: Boolean) {
        if (blocked) {
            layoutBlocked.visibility = View.VISIBLE
            layoutInput.visibility = View.GONE
        } else {
            layoutBlocked.visibility = View.GONE
            layoutInput.visibility = View.VISIBLE
        }
    }
    
    private fun playSentSound() {
        try {
            val mediaPlayer = android.media.MediaPlayer.create(context, R.raw.sent_message)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun playReceivedSound() {
        try {
            val mediaPlayer = android.media.MediaPlayer.create(context, R.raw.received_message)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showAttachmentMenu(v: View) {
        val popup = PopupMenu(context, v)
        popup.menu.add("Gallery")
        popup.menu.add("Audio")
        popup.menu.add("Documents")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Gallery" -> openGalleryPicker() // Prioritize images/videos
                "Audio" -> openGalleryPicker() // TedImagePicker supports video/audio if configured
                "Documents" -> openGalleryPicker()
            }
            true
        }
        popup.show()
    }

    private fun showEmojiPicker() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_emoji_picker, null)
        val rv = view.findViewById<RecyclerView>(R.id.rvEmojis)
        
        val emojis = listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
            "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
            "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
            "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬",
            "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗",
            "🤔", "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯",
            "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐",
            "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑", "🤠", "😈",
            "👿", "👹", "👺", "🤡", "💩", "👻", "💀", "☠️", "👽", "👾",
            "🤖", "🎃", "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿",
            "😾", "👋", "🤚", "🖐", "✋", "🖖", "👌", "🤏", "✌️", "🤞",
            "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "👍",
            "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝",
            "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦵", "🦿", "🦶", "👂",
            "🦻", "👃", "🧠", "🦷", "🦴", "👀", "👁", "👅", "👄", "💋",
            "🩸", "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎",
            "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟"
        )
        
        rv.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context, 6)
        rv.adapter = EmojiAdapter(emojis) { emoji ->
            val currentText = etMessage.text.toString()
            val selectionStart = etMessage.selectionStart
            val selectionEnd = etMessage.selectionEnd
            val newText = currentText.replaceRange(selectionStart, selectionEnd, emoji)
            etMessage.setText(newText)
            etMessage.setSelection(selectionStart + emoji.length)
            dialog.dismiss()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }

    class EmojiAdapter(private val list: List<String>, private val onClick: (String) -> Unit) : RecyclerView.Adapter<EmojiAdapter.VH>() {
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvEmoji: TextView = v.findViewById(R.id.tvEmoji)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_emoji, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.tvEmoji.text = list[position]
            holder.itemView.setOnClickListener { onClick(list[position]) }
        }
        override fun getItemCount() = list.size
    }

    private fun setupInput() {
        etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    btnSend.visibility = View.GONE
                    btnMic.visibility = View.VISIBLE
                    btnGallery.visibility = View.VISIBLE
                } else {
                    btnSend.visibility = View.VISIBLE
                    btnMic.visibility = View.GONE
                    btnGallery.visibility = View.GONE 
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun startRecording() {
        try {
            audioPath = "${requireContext().externalCacheDir?.absolutePath}/audiorecord_${System.currentTimeMillis()}.3gp"
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioPath)
                prepare()
                start()
            }
            // requireView().showSnackbar("Recording...")
            // btnMic.setColorFilter(Color.RED)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            btnMic.setColorFilter(Color.parseColor("#AAAAAA"))
            
            val message = ChatMessage(
                sender = "You",
                message = "Audio",
                isMe = true,
                status = 0,
                type = "audio",
                mediaUrl = audioPath
            )
            messages.add(message)
            adapter.notifyItemInserted(messages.size - 1)
            rvChat.scrollToPosition(messages.size - 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                val data = JSONObject()
                data.put("roomId", roomId)
                data.put("message", msg)
                data.put("senderId", userId)
                data.put("timestamp", System.currentTimeMillis())
                
                socket.emit("send_message", data)
                
                playSentSound()

                activity?.runOnUiThread {
                    messages.add(ChatMessage("You", msg, true, System.currentTimeMillis(), 0)) // 0 = Sending
                    adapter.notifyItemInserted(messages.size - 1)
                    rvChat.scrollToPosition(messages.size - 1)
                }
                
                etMessage.text.clear()
            }
        }

        btnMic.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (checkAudioPermission()) {
                        startRecording()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopRecording()
                    true
                }
                else -> false
            }
        }

        btnGallery.setOnClickListener {
            openGalleryPicker()
        }

        btnEmoji.setOnClickListener {
            showEmojiPicker()
        }
    }

    private fun checkAudioPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            false
        } else {
            true
        }
    }
    
    private fun setupRecyclerView() {
        adapter = MessageAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(context)
        rvChat.adapter = adapter
        if (messages.isNotEmpty()) rvChat.scrollToPosition(messages.size - 1)
    }

    private fun loadAds(view: View) {
        try {
            val adViewSmart = view.findViewById<AdView>(R.id.adViewSmart)
            val bannerRequest = AdRequest.Builder().build()
            
            adViewSmart.adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdLoaded() {
                    adViewSmart.visibility = View.VISIBLE
                }
            }
            adViewSmart.loadAd(bannerRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                }
            }
        }

        socket.on("receive_message") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val msg = data.optString("message")
                val senderId = data.optString("senderId")
                
                if (senderId != userId) {
                    playReceivedSound()
                    activity?.runOnUiThread {
                        messages.add(ChatMessage("Katina", msg, false, System.currentTimeMillis(), 3))
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
            val tvTimestamp: TextView = v.findViewById(R.id.tvTimestamp)
            val ivStatus: ImageView = v.findViewById(R.id.ivStatus)
            val ivImage: ImageView = v.findViewById(R.id.ivMessageImage)
            val ivPlay: ImageView = v.findViewById(R.id.ivPlayIcon)
            val card: View = v.findViewById(R.id.cardMessage)
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
            holder.tvTimestamp.text = formatTime(item.timestamp)
            
            // Handle Media
            holder.ivImage.visibility = View.GONE
            holder.ivPlay.visibility = View.GONE
            
            if (item.mediaUrl != null) {
                holder.ivImage.visibility = View.VISIBLE
                
                when (item.type) {
                    "image" -> {
                        Glide.with(holder.itemView.context)
                            .load(item.mediaUrl)
                            .transform(RoundedCorners(16))
                            .into(holder.ivImage)
                    }
                    "video" -> {
                        holder.ivPlay.visibility = View.VISIBLE
                        Glide.with(holder.itemView.context)
                            .load(item.mediaUrl) // Glide loads video thumbnails
                            .transform(RoundedCorners(16))
                            .into(holder.ivImage)
                    }
                    "audio" -> {
                        holder.ivImage.setImageResource(R.drawable.ic_music_note)
                        holder.ivImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        holder.ivImage.setBackgroundColor(Color.TRANSPARENT)
                    }
                    "file" -> {
                        holder.ivImage.setImageResource(R.drawable.ic_insert_drive_file)
                        holder.ivImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        holder.ivImage.setBackgroundColor(Color.TRANSPARENT)
                    }
                    else -> {
                        // Fallback for unknown types or just text with url
                        if (item.type == "text") {
                            holder.ivImage.visibility = View.GONE
                        }
                    }
                }
            }

            val constraintSet = ConstraintSet()
            constraintSet.clone(holder.parent)

            if (item.isMe) {
                // Align Right
                constraintSet.setHorizontalBias(R.id.cardMessage, 1.0f)
                constraintSet.setHorizontalBias(R.id.tvSenderName, 1.0f)
                
                // Light Green Bubble (#d8fcd1)
                holder.card.setBackgroundResource(R.drawable.bg_chat_outgoing)
                holder.tvMessage.setTextColor(Color.BLACK)
                holder.tvTimestamp.setTextColor(Color.parseColor("#666666"))
                ImageViewCompat.setImageTintList(holder.ivStatus, ColorStateList.valueOf(Color.parseColor("#666666")))
                
                holder.ivStatus.visibility = View.VISIBLE
                setStatusIcon(holder.ivStatus, item.status)
                
            } else {
                // Align Left
                constraintSet.setHorizontalBias(R.id.cardMessage, 0.0f)
                constraintSet.setHorizontalBias(R.id.tvSenderName, 0.0f)
                
                // White Bubble (#FFFFFF)
                holder.card.setBackgroundResource(R.drawable.bg_chat_incoming)
                holder.tvMessage.setTextColor(Color.BLACK)
                holder.tvTimestamp.setTextColor(Color.parseColor("#888888"))
                
                holder.ivStatus.visibility = View.GONE
            }
            
            constraintSet.applyTo(holder.parent)
        }

        private fun setStatusIcon(iv: ImageView, status: Int) {
            when (status) {
                0 -> iv.setImageResource(R.drawable.ic_check) // Using check for sending too for now
                1 -> iv.setImageResource(R.drawable.ic_check)
                2 -> iv.setImageResource(R.drawable.ic_done_all)
                3 -> {
                    iv.setImageResource(R.drawable.ic_done_all)
                    ImageViewCompat.setImageTintList(iv, ColorStateList.valueOf(Color.parseColor("#2196F3"))) // Blue
                }
            }
        }

        private fun formatTime(time: Long): String {
            val date = Date(time)
            val now = Date()
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            
            // Check if today (simplified)
            val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            if (fmt.format(date) == fmt.format(now)) {
                return sdf.format(date)
            } else {
                return "Yesterday" // Simplified for demo
            }
        }

        override fun getItemCount() = list.size
    }
}