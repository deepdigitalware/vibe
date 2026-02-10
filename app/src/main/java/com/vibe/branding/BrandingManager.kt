package com.vibe.branding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class BrandingConfig(
    val appName: String?,
    val splashLogoUrl: String?,
    val heroTitle: String?,
    val heroSubtitle: String?,
    val billingRatePerBlockRupees: Int?,
    val blockDurationMinutes: Int?,
    val paymentsProvider: String?
)

object BrandingManager {
    private const val DEFAULT_BASE = "http://10.0.2.2:4000"
    private const val TAG = "BrandingManager"

    fun serverBaseUrl(ctx: Context): String {
        return try {
            val stream = ctx.assets.open("branding/server_base_url.txt")
            val reader = BufferedReader(InputStreamReader(stream))
            val url = reader.readLine()
            reader.close()
            if (url != null && url.trim().isNotEmpty()) url.trim() else DEFAULT_BASE
        } catch (_: Exception) {
            DEFAULT_BASE
        }
    }

    fun fetchConfig(ctx: Context): BrandingConfig? {
        return try {
            val base = serverBaseUrl(ctx)
            val url = URL("$base/config/app")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "GET"
            conn.doInput = true
            val code = conn.responseCode
            if (code in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                BrandingConfig(
                    appName = json.optString("appName"),
                    splashLogoUrl = json.optString("splashLogoUrl"),
                    heroTitle = json.optString("heroTitle"),
                    heroSubtitle = json.optString("heroSubtitle"),
                    billingRatePerBlockRupees = json.optInt("billingRatePerBlockRupees"),
                    blockDurationMinutes = json.optInt("blockDurationMinutes"),
                    paymentsProvider = json.optString("paymentsProvider")
                )
            } else {
                Log.w(TAG, "Config HTTP $code")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Config fetch failed: ${e.message}")
            null
        }
    }

    fun loadLogoBitmap(ctx: Context, urlString: String?): Bitmap? {
        // Try remote URL first
        if (urlString != null && urlString.isNotBlank()) {
            try {
                val url = URL(if (urlString.startsWith("http")) urlString else serverBaseUrl(ctx) + urlString)
                url.openStream().use { stream ->
                    return BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Remote logo load failed: ${e.message}")
            }
        }
        // Fallback to asset placeholder
        return try {
            ctx.assets.open("branding/branding_logo.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }
}