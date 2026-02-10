package com.vibe.config

import com.vibe.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppConfig(
    val appName: String?,
    val splashLogoUrl: String?,
    val heroTitle: String?,
    val heroSubtitle: String?,
    val billingRatePerBlockRupees: Int?,
    val blockDurationMinutes: Int?,
    val paymentsProvider: String?
)

object RemoteConfig {
    fun fetch(): AppConfig? {
        return try {
            val url = URL("${BuildConfig.CONFIG_BASE_URL}/config/app")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            AppConfig(
                appName = json.optString("appName", null),
                splashLogoUrl = json.optString("splashLogoUrl", null),
                heroTitle = json.optString("heroTitle", null),
                heroSubtitle = json.optString("heroSubtitle", null),
                billingRatePerBlockRupees = json.optInt("billingRatePerBlockRupees"),
                blockDurationMinutes = json.optInt("blockDurationMinutes"),
                paymentsProvider = json.optString("paymentsProvider", null)
            )
        } catch (_: Exception) {
            null
        }
    }
}