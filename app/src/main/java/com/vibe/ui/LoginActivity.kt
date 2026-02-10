package com.vibe.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.hbb20.CountryCodePicker
import com.vibe.R
import com.vibe.branding.BrandingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

import com.vibe.api.ApiClient
import com.vibe.api.LoginAppRequest
import com.vibe.api.SessionManager
import retrofit2.awaitResponse

class LoginActivity : AppCompatActivity() {
    private var auth: FirebaseAuth? = null
    private var verificationId: String? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient

    // UI Components
    private lateinit var llLoginOptions: LinearLayout
    private lateinit var llPhoneInput: LinearLayout
    private lateinit var llOtpInput: LinearLayout
    private lateinit var btnGoogleLogin: SignInButton
    private lateinit var btnPhoneLogin: Button
    private lateinit var btnSendOtp: Button
    private lateinit var btnCancel: Button
    private lateinit var btnVerifyOtp: Button
    private lateinit var etPhone: EditText
    private lateinit var etOtp: EditText
    private lateinit var ccp: CountryCodePicker

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showSnackbar("Google Sign-In failed: ${e.statusCode}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefs = getSharedPreferences("vibe_prefs", Context.MODE_PRIVATE)

        // Initialize Firebase
        val firebaseReady = try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            true
        } catch (_: Exception) { false }
        
        if (firebaseReady) {
            auth = FirebaseAuth.getInstance()
        } else {
            showSnackbar("Firebase not configured.")
        }

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        llLoginOptions = findViewById(R.id.llLoginOptions)
        llPhoneInput = findViewById(R.id.llPhoneInput)
        llOtpInput = findViewById(R.id.llOtpInput)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        btnPhoneLogin = findViewById(R.id.btnPhoneLogin)
        btnSendOtp = findViewById(R.id.btnSendOtp)
        btnCancel = findViewById(R.id.btnCancel)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)
        etPhone = findViewById(R.id.etPhone)
        etOtp = findViewById(R.id.etOtp)
        ccp = findViewById(R.id.ccp)
        
        ccp.registerCarrierNumberEditText(etPhone)
    }

    private fun setupListeners() {
        btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        btnPhoneLogin.setOnClickListener {
            showPhoneInput()
        }

        btnCancel.setOnClickListener {
            showLoginOptions()
        }

        btnSendOtp.setOnClickListener {
            if (!ccp.isValidFullNumber) {
                showSnackbar("Enter a valid phone number")
                return@setOnClickListener
            }
            
            val fullNumber = ccp.fullNumberWithPlus
            val a = auth
            if (a == null) {
                showSnackbar("Firebase not configured.")
                return@setOnClickListener
            }
            
            btnSendOtp.isEnabled = false
            
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    showSnackbar("Verification failed: ${e.message}")
                    btnSendOtp.isEnabled = true
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    this@LoginActivity.verificationId = verificationId
                    showSnackbar("OTP sent successfully")
                    showOtpInput()
                }
            }

            try {
                val options = PhoneAuthOptions.newBuilder(a)
                    .setPhoneNumber(fullNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(callbacks)
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            } catch (e: Exception) {
                showSnackbar("Error: ${e.message}")
                btnSendOtp.isEnabled = true
            }
        }

        btnVerifyOtp.setOnClickListener {
            val code = etOtp.text.toString().trim()
            val vid = verificationId
            if (vid.isNullOrEmpty() || code.isEmpty()) {
                showSnackbar("Enter the received OTP")
                return@setOnClickListener
            }
            val credential = PhoneAuthProvider.getCredential(vid, code)
            signInWithPhoneCredential(credential)
        }
    }

    private fun showLoginOptions() {
        llLoginOptions.visibility = View.VISIBLE
        llPhoneInput.visibility = View.GONE
        llOtpInput.visibility = View.GONE
        btnSendOtp.isEnabled = true
    }

    private fun showPhoneInput() {
        llLoginOptions.visibility = View.GONE
        llPhoneInput.visibility = View.VISIBLE
        llOtpInput.visibility = View.GONE
    }

    private fun showOtpInput() {
        llLoginOptions.visibility = View.GONE
        llPhoneInput.visibility = View.GONE
        llOtpInput.visibility = View.VISIBLE
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    onLoginSuccess()
                } else {
                    showSnackbar("Authentication Failed: ${task.exception?.message}")
                }
            }
    }

    private fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        auth?.signInWithCredential(credential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    onLoginSuccess()
                } else {
                    if (task.exception is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                        showSnackbar("Invalid code.")
                    } else {
                        showSnackbar("Sign In Failed: ${task.exception?.message}")
                    }
                }
            }
    }

    private fun onLoginSuccess() {
        val user = auth?.currentUser
        if (user == null) {
            navigateToMain()
            return
        }

        showSnackbar("Connecting to Vibe Server...")

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.api.loginApp(
                        LoginAppRequest(
                            uid = user.uid,
                            phone = user.phoneNumber,
                            email = user.email
                        )
                    ).execute()
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    SessionManager.saveToken(body.token)
                    body.user?.id?.let { SessionManager.saveUserId(it) }
                    
                    withContext(Dispatchers.Main) {
                        showSnackbar("Connected!")
                        navigateToMain()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showSnackbar("Server Connection Failed: ${response.code()}. Proceeding offline mode.")
                        navigateToMain()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showSnackbar("Server Error: ${e.message}. Proceeding offline mode.")
                    navigateToMain()
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
}
