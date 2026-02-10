package com.vibe.payments

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.vibe.BuildConfig

object PaymentManager {
    fun buildUpiIntent(context: Context, amountRupees: Double, preferGooglePay: Boolean = false): Intent {
        val txnRef = "VIBE-${System.currentTimeMillis()}"
        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", BuildConfig.UPI_ID)
            .appendQueryParameter("pn", BuildConfig.PAYEE_NAME)
            .appendQueryParameter("tn", "Vibe Wallet Recharge")
            .appendQueryParameter("am", String.format("%.2f", amountRupees))
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tr", txnRef)
            .build()

        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (preferGooglePay) {
            intent.`package` = "com.google.android.apps.nbu.paisa.user"
        }
        return Intent.createChooser(intent, "Pay with UPI")
    }
}