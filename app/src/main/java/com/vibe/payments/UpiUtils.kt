package com.vibe.payments

import android.content.Intent

object UpiUtils {
    data class UpiStatus(
        val success: Boolean,
        val raw: String?,
        val txnId: String?,
        val txnRef: String?
    )

    fun parseUpiPaymentStatus(data: Intent?): UpiStatus {
        val response = data?.getStringExtra("response")
        // Many UPI apps return a query string like: txnId=...&responseCode=...&Status=SUCCESS&txnRef=...
        val success = response?.contains("Status=SUCCESS", ignoreCase = true) == true
        val txnId = response?.substringAfter("txnId=", missingDelimiterValue = "")?.substringBefore('&').takeIf { it?.isNotBlank() == true }
        val txnRef = response?.substringAfter("txnRef=", missingDelimiterValue = "")?.substringBefore('&').takeIf { it?.isNotBlank() == true }
        return UpiStatus(success = success, raw = response, txnId = txnId, txnRef = txnRef)
    }
}