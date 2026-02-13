package com.vibe.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.vibe.R
import com.vibe.billing.BillingConfig
import com.vibe.rtc.RtcClient
import com.vibe.rtc.LiveKitClient
import com.vibe.wallet.WalletManager
import kotlinx.coroutines.launch

class CallActivity : AppCompatActivity() {

    private lateinit var tvBlockCountdown: TextView
    private lateinit var btnEndCall: Button
    private lateinit var layoutRinging: android.view.View
    private lateinit var btnAnswerCall: Button
    private lateinit var btnDeclineCall: Button
    private lateinit var tvCallStatus: TextView
    private lateinit var rtcClient: RtcClient
    private lateinit var liveKitClient: LiveKitClient
    private lateinit var wallet: WalletManager
    private var blockTimer: CountDownTimer? = null
    private var ringtone: android.media.Ringtone? = null
    private var role: String? = null
    private var roomId: String? = null
    private var liveKitToken: String? = null
    private var liveKitUrl: String? = null
    private val socket = com.vibe.util.SocketManager.getSocket()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.CAMERA] == true && result[Manifest.permission.RECORD_AUDIO] == true
        if (granted) {
            setupRingingUI()
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Camera/Mic permissions required", Snackbar.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        tvBlockCountdown = findViewById(R.id.tvBlockCountdown)
        btnEndCall = findViewById(R.id.btnEndCall)
        layoutRinging = findViewById(R.id.layoutRinging)
        btnAnswerCall = findViewById(R.id.btnAnswerCall)
        btnDeclineCall = findViewById(R.id.btnDeclineCall)
        tvCallStatus = findViewById(R.id.tvCallStatus)

        rtcClient = RtcClient(this)
        liveKitClient = LiveKitClient(this).apply { init(this@CallActivity) }
        wallet = WalletManager(this)

        role = intent.getStringExtra(EXTRA_ROLE)
        roomId = intent.getStringExtra(EXTRA_ROOM_ID)
        liveKitToken = intent.getStringExtra(EXTRA_LIVEKIT_TOKEN)
        liveKitUrl = intent.getStringExtra(EXTRA_LIVEKIT_URL)
        
        btnEndCall.setOnClickListener { endCallFlow() }
        btnDeclineCall.setOnClickListener { endCallFlow() }
        btnAnswerCall.setOnClickListener { acceptCall() }
        
        findViewById<android.view.View>(R.id.btnSwitchCamera).setOnClickListener {
            if (liveKitToken != null) {
                // LiveKit camera switch logic can be added here
            } else {
                rtcClient.switchCamera()
            }
        }
        setupDraggableLocalView()
        hideSystemUI()

        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    private fun setupRingingUI() {
        if (role == "callee") {
            tvCallStatus.text = "Incoming video call..."
            btnAnswerCall.visibility = android.view.View.VISIBLE
            playRingtone()
        } else {
            tvCallStatus.text = "Ringing..."
            btnAnswerCall.visibility = android.view.View.GONE
            playRingtone()
            // In a real app, emit "initiate_call" here via socket
            socket?.emit("initiate_call", roomId)
        }
        
        // Listen for call acceptance/rejection
        socket?.on("call_accepted") {
            runOnUiThread {
                onCallConnected()
            }
        }
        socket?.on("call_rejected") {
            runOnUiThread {
                finish()
            }
        }
    }

    private fun acceptCall() {
        stopRingtone()
        socket?.emit("accept_call", roomId)
        onCallConnected()
    }

    private fun onCallConnected() {
        stopRingtone()
        layoutRinging.visibility = android.view.View.GONE
        startCallFlow()
    }
    
    private fun setupDraggableLocalView() {
        val view = findViewById<android.view.View>(R.id.cvLocalView)
        view.setOnTouchListener(object : android.view.View.OnTouchListener {
            var dX = 0f
            var dY = 0f
            override fun onTouch(v: android.view.View, event: android.view.MotionEvent): Boolean {
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        dX = v.x - event.rawX
                        dY = v.y - event.rawY
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        v.animate()
                            .x(event.rawX + dX)
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                    }
                }
                return true
            }
        })
    }
    
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
    
    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    private fun playRingtone() {
        try {
            val uri = if (role == "caller") {
                 android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE) // Or TYPE_ALARM/NOTIFICATION for caller
            } else {
                 android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            }
            // Ideally caller hears a ringback, callee hears a ringtone.
            // Using system ringtone for both as placeholder for now, or distinguish if possible.
            // For caller, usually we play a "beep beep" tone (ToneGenerator), but user asked for ringtone.
            
            ringtone = android.media.RingtoneManager.getRingtone(applicationContext, uri)
            ringtone?.play()
            
            // Auto-cancel call if not answered within 45 seconds
            object : CountDownTimer(45000, 1000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    if (layoutRinging.visibility == android.view.View.VISIBLE) {
                        endCallFlow()
                    }
                }
            }.start()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopRingtone() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAds() {
        /*
        try {
            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
            findViewById<com.google.android.gms.ads.AdView>(R.id.adViewTop).loadAd(adRequest)
            findViewById<com.google.android.gms.ads.AdView>(R.id.adViewBottom).loadAd(adRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        */
    }

    private fun startCallFlow() {
        lifecycleScope.launch {
            // Role check: Only caller pays, Callee earns
            val r = role
            
            if (r == "caller") {
                // We no longer deduct upfront on client-side.
                // The VPS backend (Socket.IO) handles billing on disconnect.
                // But we still check if they have enough balance to start.
                val balance = wallet.getBalanceSync()
                if (balance < BillingConfig.CALLER_COST_PER_MINUTE) {
                    AlertDialog.Builder(this@CallActivity)
                        .setTitle("Insufficient balance")
                        .setMessage("You need at least ₹${BillingConfig.CALLER_COST_PER_MINUTE} to start a call.")
                        .setPositiveButton("Add Funds") { _, _ ->
                            startActivity(RechargeActivity.newIntent(this@CallActivity))
                            finish()
                        }
                        .setNegativeButton("Cancel") { _, _ -> finish() }
                        .show()
                    return@launch
                }
            }

            // Start RTC preview and signaling
            val id = roomId
            val lkToken = liveKitToken
            val lkUrl = liveKitUrl

            if (lkToken != null && lkUrl != null) {
                // Use LiveKit for SFU-based calling
                liveKitClient.joinRoom(
                    lkUrl,
                    lkToken,
                    findViewById(R.id.localContainer),
                    findViewById(R.id.remoteContainer)
                )
            } else if (r == "caller" && id != null) {
                rtcClient.startAsCaller(R.id.localContainer, R.id.remoteContainer, id)
            } else if (r == "callee" && id != null) {
                rtcClient.startAsCallee(R.id.localContainer, R.id.remoteContainer, id)
            } else {
                rtcClient.startPreview(R.id.localContainer, R.id.remoteContainer)
            }
            
            // Server-side handles the timer now via Socket.IO disconnect
            // startBlockCountdown() // Deprecated client-side timer
        }
    }

    private fun startBlockCountdown() {
        blockTimer?.cancel()
        blockTimer = object : CountDownTimer(
            BillingConfig.BLOCK_DURATION_SECONDS * 1000L,
            1000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).toInt()
                val mm = seconds / 60
                val ss = seconds % 60
                
                if (role == "caller") {
                    tvBlockCountdown.text = String.format("Next charge in: %02d:%02d", mm, ss)
                } else {
                    tvBlockCountdown.text = String.format("Earning ₹${BillingConfig.RECEIVER_EARNING_PER_MINUTE} in: %02d:%02d", mm, ss)
                }
            }

            override fun onFinish() {
                lifecycleScope.launch {
                    if (role == "caller") {
                        // Caller pays
                        val ok = wallet.deductBlockIfPossible(BillingConfig.CALLER_COST_PER_MINUTE)
                        if (ok) {
                            startBlockCountdown()
                        } else {
                            Snackbar.make(
                                findViewById(android.R.id.content),
                                "Balance exhausted! Call ending...",
                                Snackbar.LENGTH_LONG
                            ).show()
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ endCallFlow() }, 2000)
                        }
                    } else {
                        // Callee earns
                        wallet.addBalanceRupees(BillingConfig.RECEIVER_EARNING_PER_MINUTE.toDouble())
                        
                        // Show earnings toast/snackbar
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "You earned ₹${BillingConfig.RECEIVER_EARNING_PER_MINUTE}!",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        
                        startBlockCountdown()
                    }
                }
            }
        }.start()
    }

    private fun endCallFlow() {
        stopRingtone()
        blockTimer?.cancel()
        rtcClient.endCall()
        liveKitClient.leaveRoom()
        finish()
    }

    companion object {
        private const val EXTRA_ROLE = "extra_role"
        private const val EXTRA_ROOM_ID = "extra_room_id"
        private const val EXTRA_LIVEKIT_TOKEN = "extra_livekit_token"
        private const val EXTRA_LIVEKIT_URL = "extra_livekit_url"

        fun newIntent(
            context: Context,
            role: String? = null,
            roomId: String? = null,
            liveKitToken: String? = null,
            liveKitUrl: String? = null
        ): Intent =
            Intent(context, CallActivity::class.java).apply {
                if (role != null) putExtra(EXTRA_ROLE, role)
                if (roomId != null) putExtra(EXTRA_ROOM_ID, roomId)
                if (liveKitToken != null) putExtra(EXTRA_LIVEKIT_TOKEN, liveKitToken)
                if (liveKitUrl != null) putExtra(EXTRA_LIVEKIT_URL, liveKitUrl)
            }
    }
}
