package com.vibe.wallet

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.vibe.api.AddBalanceRequest
import com.vibe.api.ApiClient
import com.vibe.api.DeductRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class WalletManager(private val context: Context) {

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "guest_user"

    // Only a fallback cache
    private val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)

    suspend fun getBalanceRupees(): Double = withContext(Dispatchers.IO) {
        try {
            val resp = ApiClient.api.getBalance(userId).awaitResponse()
            if (resp.isSuccessful) {
                val bal = resp.body()?.balance ?: 0.0
                prefs.edit().putFloat("cached_bal", bal.toFloat()).apply()
                bal
            } else {
                prefs.getFloat("cached_bal", 0f).toDouble()
            }
        } catch (e: Exception) {
            prefs.getFloat("cached_bal", 0f).toDouble()
        }
    }

    suspend fun addBalanceRupees(amountRupees: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val resp = ApiClient.api.addBalance(AddBalanceRequest(userId, amountRupees)).awaitResponse()
            resp.isSuccessful && resp.body()?.success == true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deductBlockIfPossible(blockCostRupees: Int = 10): Boolean = withContext(Dispatchers.IO) {
        try {
            val resp = ApiClient.api.deductBalance(DeductRequest(userId, blockCostRupees.toDouble())).awaitResponse()
            resp.isSuccessful && resp.body()?.success == true
        } catch (e: Exception) {
            false
        }
    }
}
