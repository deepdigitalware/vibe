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
    val appName: String? = null,
    val appVersion: String? = null,
    val heroText: String? = null,
    val appLogo: String? = null,
    val loginVideo: String? = null,
    val termsUrl: String? = null,
    val privacyUrl: String? = null,
    val billingRatePerBlockRupees: Int? = null,
    val blockDurationMinutes: Int? = null,
    val paymentsProvider: String? = null
)

object BrandingManager {
    private const val DEFAULT_BASE = "https://vibe.deepverse.cloud"
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
            val url = URL("$base/api/config")
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
                    appName = json.optString("app_name"),
                    appVersion = json.optString("app_version"),
                    heroText = json.optString("hero_text"),
                    appLogo = json.optString("app_logo"),
                    loginVideo = json.optString("login_video"),
                    termsUrl = json.optString("terms_url"),
                    privacyUrl = json.optString("privacy_url"),
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
        return try {
            ctx.assets.open("logo.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }
}