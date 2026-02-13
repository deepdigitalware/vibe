package com.vibe.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.launch
import com.vibe.R
import com.vibe.BuildConfig
import org.json.JSONObject

class RechargeActivity : AppCompatActivity(), PaymentResultListener {

    data class RechargeOption(val payAmount: Int, val getAmount: Int, val bonus: Int)

    private val options = listOf(
        RechargeOption(50, 60, 10),
        RechargeOption(100, 120, 20),
        RechargeOption(200, 250, 50),
        RechargeOption(500, 700, 200),
        RechargeOption(1000, 1500, 500)
    )

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        verifyAndAddFunds("SUCCESS", razorpayPaymentId)
    }

    override fun onPaymentError(code: Int, response: String?) {
        showDialog("Payment Failed", "Error $code: $response")
    }

    private fun verifyAndAddFunds(status: String, ref: String?) {
        val wallet = com.vibe.wallet.WalletManager(this)
        val txnId = currentTxnId ?: return
        
        lifecycleScope.launch {
            val success = wallet.updateTransaction(txnId, status, ref)
            if (success && status == "SUCCESS") {
                showDialog("Payment Successful", "Added funds to your wallet!", true)
            } else if (status == "PENDING") {
                showDialog("Payment Pending", "Transaction submitted. Balance will update once approved.", true)
            } else {
                showDialog("Payment Failed", "Transaction failed or cancelled.")
            }
        }
    }

    private fun showDialog(title: String, msg: String, finish: Boolean = false) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("OK") { _, _ ->
                if (finish) finish()
            }
            .show()
    }

    private var lastOption: RechargeOption? = null
    private lateinit var tvWalletBalance: TextView
    private lateinit var tvEarnedCash: TextView
    private lateinit var btnWithdraw: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recharge)

        tvWalletBalance = findViewById(R.id.tvWalletBalance)
        tvEarnedCash = findViewById(R.id.tvEarnedCash)
        btnWithdraw = findViewById(R.id.btnWithdraw)

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val rv = findViewById<RecyclerView>(R.id.rvRechargeOptions)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = RechargeAdapter(options) { opt ->
            lastOption = opt
            initiateRazorpayPayment(opt)
        }
        
        btnWithdraw.setOnClickListener {
            handleWithdrawal()
        }

        // Custom Recharge Logic
        val etCustomAmount = findViewById<android.widget.EditText>(R.id.etCustomAmount)
        val btnCustomPay = findViewById<Button>(R.id.btnCustomPay)
        
        btnCustomPay.setOnClickListener {
            val amountStr = etCustomAmount.text.toString().trim()
            if (amountStr.isNotEmpty()) {
                val amount = amountStr.toIntOrNull() ?: 0
                if (amount >= 10) {
                    val opt = RechargeOption(amount, amount, 0)
                    lastOption = opt
                    initiateRazorpayPayment(opt)
                } else {
                    etCustomAmount.error = "Minimum recharge is ₹10"
                }
            }
        }

        updateBalances()
        loadAds()
        Checkout.preload(applicationContext)
    }

    private fun updateBalances() {
        val wallet = com.vibe.wallet.WalletManager(this)
        lifecycleScope.launch {
            val balance = wallet.getBalanceRupees()
            tvWalletBalance.text = "₹${String.format("%.2f", balance)}"
            
            // For demo, earned cash is a fraction of balance or separate pref
            // In real app, this would come from a separate API field
            val prefs = getSharedPreferences("vibe_prefs", Context.MODE_PRIVATE)
            val earned = prefs.getFloat("earned_cash", 0f).toDouble()
            tvEarnedCash.text = "₹${String.format("%.2f", earned)}"
        }
    }

    private fun handleWithdrawal() {
        val prefs = getSharedPreferences("vibe_prefs", Context.MODE_PRIVATE)
        val earned = prefs.getFloat("earned_cash", 0f)
        
        if (earned < 1000) {
            showDialog("Minimum Withdrawal", "You need at least ₹1000 in earned cash to withdraw. Current: ₹${String.format("%.2f", earned)}")
            return
        }

        // Proceed with withdrawal request
        lifecycleScope.launch {
            val wallet = com.vibe.wallet.WalletManager(this@RechargeActivity)
            val response = wallet.requestWithdrawal(earned.toDouble())
            if (response.success) {
                // Reset earned cash locally on success
                prefs.edit().putFloat("earned_cash", 0f).apply()
                updateBalances()
                showDialog("Success", "Withdrawal request of ₹$earned submitted successfully!")
            } else {
                showDialog("Withdrawal Failed", response.message ?: "Unknown error occurred")
            }
        }
    }

    private fun loadAds() {
        try {
            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
            findViewById<com.google.android.gms.ads.AdView>(R.id.adViewTop).loadAd(adRequest)
            findViewById<com.google.android.gms.ads.AdView>(R.id.adViewBottom).loadAd(adRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Store current Txn ID
    private var currentTxnId: String? = null

    private fun initiateRazorpayPayment(opt: RechargeOption) {
        val wallet = com.vibe.wallet.WalletManager(this)
        
        lifecycleScope.launch {
            // 1. Init Transaction on Server
            val txnId = wallet.initTransaction(opt.getAmount.toDouble(), "Recharge ${opt.payAmount}")
            if (txnId == null) {
                showDialog("Error", "Could not initiate transaction. Check internet.")
                return@launch
            }
            currentTxnId = txnId

            // 2. Launch Razorpay Checkout
            val checkout = Checkout()
            checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)
            
            try {
                val options = JSONObject()
                options.put("name", "Vibe")
                options.put("description", "Wallet Recharge")
                options.put("image", "https://vibe.deepverse.cloud/logo.png")
                options.put("currency", "INR")
                options.put("amount", opt.payAmount * 100) // Razorpay expects amount in paise
                options.put("prefill.email", "user@example.com")
                options.put("prefill.contact", "9999999999")
                options.put("theme.color", "#6200EE")
                
                // Important for Google Play Compliance: 
                // Enable 'User Choice Billing' compatible payment methods
                options.put("send_sms_hash", true)
                
                checkout.open(this@RechargeActivity, options)
            } catch (e: Exception) {
                showDialog("Error", "Error in starting Razorpay: ${e.message}")
            }
        }
    }

    class RechargeAdapter(
        private val list: List<RechargeOption>,
        private val onClick: (RechargeOption) -> Unit
    ) : RecyclerView.Adapter<RechargeAdapter.Holder>() {

        class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val tvAmount: TextView = v.findViewById(R.id.tvAmount)
            val tvBonus: TextView = v.findViewById(R.id.tvBonus)
            val btnPay: Button = v.findViewById(R.id.btnPay)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_recharge, parent, false)
            return Holder(v)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = list[position]
            holder.tvAmount.text = "₹${item.payAmount}"
            holder.tvBonus.text = "Get ₹${item.getAmount} (₹${item.bonus} Bonus)"
            holder.btnPay.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = list.size
    }
    
    companion object {
        fun newIntent(context: Context) = Intent(context, RechargeActivity::class.java)
    }
}
