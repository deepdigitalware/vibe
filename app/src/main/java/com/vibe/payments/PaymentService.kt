package com.vibe.payments

import android.util.Log
import com.vibe.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object PaymentService {
    private const val TAG = "PaymentService"

    data class VerificationResult(val status: String, val credited: Boolean, val message: String?)

    // Blocking network call; call from coroutine/IO in real app
    fun verifyUpiPayment(amountRupees: Int, txnId: String?, txnRef: String?, raw: String?): VerificationResult {
        return try {
            val url = URL("${BuildConfig.CONFIG_BASE_URL}/payments/verify")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }
            val payload = JSONObject().apply {
                put("amountRupees", amountRupees)
                put("txnId", txnId ?: JSONObject.NULL)
                put("txnRef", txnRef ?: JSONObject.NULL)
                put("raw", raw ?: JSONObject.NULL)
            }
            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val code = conn.responseCode
            val responseBody = BufferedReader(InputStreamReader(if (code in 200..299) conn.inputStream else conn.errorStream)).use { it.readText() }
            val json = JSONObject(responseBody)
            val status = json.optString("status", "UNKNOWN")
            val credited = json.optBoolean("credited", false)
            val message = json.optString("message", null)
            VerificationResult(status, credited, message)
        } catch (e: Exception) {
            Log.e(TAG, "Verification error", e)
            VerificationResult(status = "ERROR", credited = false, message = e.message)
        }
    }
}