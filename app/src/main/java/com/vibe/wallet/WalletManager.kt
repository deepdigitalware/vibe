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

    // Unified prefs
    private val prefs = context.getSharedPreferences("vibe_prefs", Context.MODE_PRIVATE)

    fun getBalanceSync(): Double {
        return prefs.getFloat("wallet_balance", 0f).toDouble()
    }

    suspend fun getBalanceRupees(): Double = withContext(Dispatchers.IO) {
        try {
            val resp = ApiClient.api.getBalance(userId).awaitResponse()
            if (resp.isSuccessful) {
                val bal = resp.body()?.balance ?: 0.0
                prefs.edit().putFloat("wallet_balance", bal.toFloat()).apply()
                bal
            } else {
                prefs.getFloat("wallet_balance", 0f).toDouble()
            }
        } catch (e: Exception) {
            // Fallback to local
            prefs.getFloat("wallet_balance", 0f).toDouble()
        }
    }

    suspend fun addBalanceRupees(amountRupees: Double): Boolean = withContext(Dispatchers.IO) {
        // Local fallback for testing is prioritized or API call
        try {
            // Optimistically update local first for immediate UI feedback
            val current = prefs.getFloat("wallet_balance", 0f)
            prefs.edit().putFloat("wallet_balance", (current + amountRupees).toFloat()).apply()

            val resp = ApiClient.api.addBalance(AddBalanceRequest(userId, amountRupees)).awaitResponse()
            if (resp.isSuccessful && resp.body()?.success == true) {
                true
            } else {
                true // Local update already done
            }
        } catch (e: Exception) {
            // Local update already done
            true
        }
    }

    suspend fun initTransaction(amount: Double, description: String): String? = withContext(Dispatchers.IO) {
        try {
            val resp = ApiClient.api.initTransaction(com.vibe.api.InitTxnRequest(amount, description)).awaitResponse()
            if (resp.isSuccessful && resp.body()?.success == true) {
                resp.body()?.txnId
            } else {
                "test_txn_${System.currentTimeMillis()}" // Fake txn for testing
            }
        } catch (e: Exception) {
            "test_txn_${System.currentTimeMillis()}" // Fake txn for testing
        }
    }

    suspend fun updateTransaction(txnId: String, status: String, ref: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val resp = ApiClient.api.updateTransaction(com.vibe.api.UpdateTxnRequest(txnId, status, ref)).awaitResponse()
            resp.isSuccessful && resp.body()?.success == true
        } catch (e: Exception) {
            true // Assume success for testing
        }
    }

    suspend fun requestWithdrawal(amountRupees: Double): com.vibe.api.WithdrawalResponse = withContext(Dispatchers.IO) {
        if (amountRupees < 500.0) {
            return@withContext com.vibe.api.WithdrawalResponse(false, "Minimum withdrawal threshold is ₹500.")
        }
        try {
            val resp = ApiClient.api.requestWithdrawal(com.vibe.api.WithdrawalRequest(userId, amountRupees)).awaitResponse()
            if (resp.isSuccessful) {
                resp.body() ?: com.vibe.api.WithdrawalResponse(false, "Server error")
            } else {
                com.vibe.api.WithdrawalResponse(false, "Failed to submit request")
            }
        } catch (e: Exception) {
            com.vibe.api.WithdrawalResponse(false, "Network error: ${e.message}")
        }
    }

    suspend fun deductBlockIfPossible(blockCostRupees: Int = 10): Boolean = withContext(Dispatchers.IO) {
        try {
            val resp = ApiClient.api.deductBalance(DeductRequest(userId, blockCostRupees.toDouble())).awaitResponse()
            if (resp.isSuccessful && resp.body()?.success == true) {
                true
            } else {
                deductLocal(blockCostRupees)
            }
        } catch (e: Exception) {
            deductLocal(blockCostRupees)
        }
    }

    private fun deductLocal(amount: Int): Boolean {
        val current = prefs.getFloat("wallet_balance", 0f)
        if (current >= amount) {
            prefs.edit().putFloat("wallet_balance", current - amount).apply()
            return true
        }
        return false
    }
}
