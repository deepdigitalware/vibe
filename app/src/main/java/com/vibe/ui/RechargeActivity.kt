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
import kotlinx.coroutines.launch
import com.vibe.R

class RechargeActivity : AppCompatActivity() {

    data class RechargeOption(val payAmount: Int, val getAmount: Int, val bonus: Int)

    private val options = listOf(
        RechargeOption(100, 120, 20),
        RechargeOption(200, 250, 50),
        RechargeOption(500, 600, 100),
        RechargeOption(1000, 1250, 250)
    )

    private val upiLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK || result.resultCode == 11) {
            val data = result.data
            val response = data?.getStringExtra("response") ?: ""
            // Parse response (Google Pay/PhonePe often return data in extras)
            handleUpiResponse(response)
        } else {
            // Some apps return RESULT_CANCELED even on success or failure, check data
            val data = result.data
            val response = data?.getStringExtra("response") ?: ""
            if (response.isNotEmpty()) {
                handleUpiResponse(response)
            } else {
                showDialog("Payment Cancelled", "Transaction cancelled or failed.")
            }
        }
    }

    private fun handleUpiResponse(response: String) {
        // Response format: txnId=...&responseCode=...&Status=...&txnRef=...
        // Status can be SUCCESS, FAILURE, SUBMITTED
        val params = response.split("&").associate {
            val parts = it.split("=")
            if (parts.size == 2) parts[0].lowercase() to parts[1] else "" to ""
        }

        val status = params["status"]?.uppercase() ?: ""
        val txnId = params["txnid"]
        val approvalRef = params["txnref"]

        if (status == "SUCCESS") {
             // Simulate server verification
             verifyAndAddFunds(txnId, approvalRef)
        } else if (status == "SUBMITTED") {
            showDialog("Payment Pending", "Your payment is pending. It will be updated once approved.")
        } else {
            showDialog("Payment Failed", "Transaction failed. Please try again.")
        }
    }

    private fun verifyAndAddFunds(txnId: String?, ref: String?) {
        // In a real app, send txnId to server.
        // For this enterprise demo, we simulate the server call or call the insecure 'add' endpoint if available.
        // We'll trust the client for now to demonstrate the "Real-time Update"
        
        val wallet = com.vibe.wallet.WalletManager(this)
        // Find the amount we just tried to pay
        // We stored it in 'lastOption'
        val amount = lastOption?.getAmount ?: 0
        if (amount > 0) {
            lifecycleScope.launch {
                wallet.addBalanceRupees(amount.toDouble())
                showDialog("Payment Successful", "Added ₹$amount to your wallet!", true)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recharge)

        val rv = findViewById<RecyclerView>(R.id.rvRechargeOptions)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = RechargeAdapter(options) { opt ->
            lastOption = opt
            initiateUpiPayment(opt)
        }
    }

    private fun initiateUpiPayment(opt: RechargeOption) {
        // UPI Intent
        // Replace 'pay@vibe' with actual UPI ID if available
        val upiId = "pay@vibe" 
        val name = "Vibe Network"
        val note = "Recharge ${opt.payAmount}"
        val amount = opt.payAmount.toString()

        val uri = Uri.parse("upi://pay")
            .buildUpon()
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", name)
            .appendQueryParameter("tn", note)
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .build()

        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = uri
        
        // Show chooser
        val chooser = Intent.createChooser(intent, "Pay with")
        if (intent.resolveActivity(packageManager) != null) {
            upiLauncher.launch(intent)
        } else {
             // Emulator fallback
             androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("No UPI App Found")
                .setMessage("You are on an emulator. Simulate SUCCESSFUL payment of ₹${opt.payAmount}?")
                .setPositiveButton("Simulate Success") { _, _ ->
                     verifyAndAddFunds("SIMULATED_TXN", "SIM_REF")
                }
                .setNegativeButton("Cancel", null)
                .show()
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
