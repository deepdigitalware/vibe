package com.vibe.api

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class BalanceResponse(val balance: Double)
data class WalletActionResponse(val success: Boolean, val newBalance: Double?, val message: String?)
data class AddBalanceRequest(val userId: String, val amount: Double)
data class DeductRequest(val userId: String, val amount: Double)

data class ProfileUpdate(
    val userId: String, 
    val name: String, 
    val bio: String,
    val avatar: String? = null,
    val cover: String? = null,
    val gallery: List<String>? = null,
    val username: String? = null
)
data class ProfileResponse(val balance: Double, val name: String, val bio: String)
data class User(val id: String, val name: String, val bio: String)
data class UsernameCheckResponse(val available: Boolean)

data class LoginAppRequest(
    val uid: String, 
    val phone: String?, 
    val email: String?, 
    val username: String? = null,
    val deviceId: String? = null
)

data class WithdrawalRequest(val userId: String, val amount: Double)
data class WithdrawalResponse(val success: Boolean, val message: String)

data class LoginAppResponse(val token: String, val user: User?)
data class UploadResponse(val url: String, val filename: String)

data class InitTxnRequest(val amount: Double, val description: String)
data class InitTxnResponse(val txnId: String, val success: Boolean)
data class UpdateTxnRequest(val txnId: String, val status: String, val approvalRef: String?)
data class UpdateTxnResponse(val success: Boolean, val status: String)

interface VibeApiService {
    @GET("/wallet/balance/{userId}")
    fun getBalance(@Path("userId") userId: String): Call<BalanceResponse>

    @POST("/api/transaction/init")
    fun initTransaction(@Body req: InitTxnRequest): Call<InitTxnResponse>

    @POST("/api/transaction/update")
    fun updateTransaction(@Body req: UpdateTxnRequest): Call<UpdateTxnResponse>

    @POST("/wallet/withdraw")
    fun requestWithdrawal(@Body req: WithdrawalRequest): Call<WithdrawalResponse>

    @POST("/wallet/add")
    fun addBalance(@Body req: AddBalanceRequest): Call<WalletActionResponse>

    @POST("/wallet/deduct")
    fun deductBalance(@Body req: DeductRequest): Call<WalletActionResponse>

    @POST("/profile/update")
    fun updateProfile(@Body req: ProfileUpdate): Call<WalletActionResponse>

    @GET("/profile/{userId}")
    fun getProfile(@Path("userId") userId: String): Call<ProfileResponse>

    @GET("/discover/users")
    fun discoverUsers(): Call<List<User>>

    @GET("/api/check-username")
    fun checkUsername(@Query("username") username: String): Call<UsernameCheckResponse>

    @POST("/api/auth/login-app")
    fun loginApp(@Body req: LoginAppRequest): Call<LoginAppResponse>

    @Multipart
    @POST("/api/upload")
    fun uploadImage(@Part file: MultipartBody.Part): Call<UploadResponse>
}
