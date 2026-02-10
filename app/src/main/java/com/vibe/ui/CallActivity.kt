package com.vibe.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.vibe.R
import com.vibe.billing.BillingConfig
import com.vibe.rtc.RtcClient
import com.vibe.wallet.WalletManager
import kotlinx.coroutines.launch

class CallActivity : AppCompatActivity() {

    private lateinit var tvBlockCountdown: TextView
    private lateinit var btnEndCall: Button
    private lateinit var rtcClient: RtcClient
    private lateinit var wallet: WalletManager
    private var blockTimer: CountDownTimer? = null
    private var role: String? = null
    private var roomId: String? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.CAMERA] == true && result[Manifest.permission.RECORD_AUDIO] == true
        if (granted) {
            startCallFlow()
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

        rtcClient = RtcClient(this)
        wallet = WalletManager(this)

        role = intent.getStringExtra(EXTRA_ROLE)
        roomId = intent.getStringExtra(EXTRA_ROOM_ID)

        btnEndCall.setOnClickListener { endCallFlow() }

        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    private fun startCallFlow() {
        lifecycleScope.launch {
            // Charge upfront for first 1-minute block
            if (!wallet.deductBlockIfPossible(BillingConfig.RATE_PER_BLOCK_RUPEES)) {
                AlertDialog.Builder(this@CallActivity)
                    .setTitle("Insufficient balance")
                    .setMessage("You need at least ₹${BillingConfig.RATE_PER_BLOCK_RUPEES} to start a call.")
                    .setPositiveButton("Add Funds") { _, _ ->
                        startActivity(RechargeActivity.newIntent(this@CallActivity))
                        finish()
                    }
                    .setNegativeButton("Cancel") { _, _ -> finish() }
                    .show()
                return@launch
            }

            // Start RTC preview and signaling
            val r = role
            val id = roomId
            if (r == "caller" && id != null) {
                rtcClient.startAsCaller(R.id.localContainer, R.id.remoteContainer, id)
            } else if (r == "callee" && id != null) {
                rtcClient.startAsCallee(R.id.localContainer, R.id.remoteContainer, id)
            } else {
                rtcClient.startPreview(R.id.localContainer, R.id.remoteContainer)
            }

            startBlockCountdown()
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
                tvBlockCountdown.text = String.format("Next charge in: %02d:%02d", mm, ss)
            }

            override fun onFinish() {
                lifecycleScope.launch {
                    // Try to deduct for next block; if not possible, end call
                    val ok = wallet.deductBlockIfPossible(BillingConfig.RATE_PER_BLOCK_RUPEES)
                    if (ok) {
                        startBlockCountdown()
                    } else {
                        AlertDialog.Builder(this@CallActivity)
                            .setTitle("Balance exhausted")
                            .setMessage("Your wallet is out of credits. The call will end.")
                            .setPositiveButton("OK") { _, _ -> endCallFlow() }
                            .setCancelable(false)
                            .show()
                    }
                }
            }
        }.start()
    }

    private fun endCallFlow() {
        blockTimer?.cancel()
        rtcClient.endCall()
        finish()
    }

    companion object {
        private const val EXTRA_ROLE = "extra_role"
        private const val EXTRA_ROOM_ID = "extra_room_id"
        fun newIntent(context: Context, role: String? = null, roomId: String? = null): Intent =
            Intent(context, CallActivity::class.java).apply {
                if (role != null) putExtra(EXTRA_ROLE, role)
                if (roomId != null) putExtra(EXTRA_ROOM_ID, roomId)
            }
    }
}
