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

import android.media.MediaPlayer
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import android.widget.VideoView
import android.graphics.Color

import com.vibe.util.ScalableVideoView
import android.util.Log

class LoginActivity : AppCompatActivity() {
    private val TAG = "LoginActivity"
    private var auth: FirebaseAuth? = null
    private var verificationId: String? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient

    // UI Components
    private lateinit var llLoginOptions: LinearLayout
    private lateinit var llOtpInput: LinearLayout
    private lateinit var btnGoogleLogin: SignInButton
    private lateinit var btnSendOtp: Button
    private lateinit var btnVerifyOtp: Button
    private lateinit var btnCancelOtp: Button
    private lateinit var etPhone: EditText
    private lateinit var etOtp: EditText
    private lateinit var ccp: CountryCodePicker
    private lateinit var videoBackground: ScalableVideoView
    private lateinit var tvTerms: TextView

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                val errorMsg = when (e.statusCode) {
                    10 -> "Google Sign-In failed: Developer Error (Check SHA-1/Package Name in Firebase)"
                    7 -> "Google Sign-In failed: Network Error"
                    12500 -> "Google Sign-In failed: Internal Error"
                    12501 -> "Google Sign-In cancelled"
                    else -> "Google Sign-In failed: ${e.statusCode}"
                }
                showSnackbar(errorMsg)
                Log.e(TAG, "Google Sign-In Error: ${e.statusCode}", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Hide the default Action Bar for modern look
        setContentView(R.layout.activity_login)

        prefs = getSharedPreferences("vibe_prefs", Context.MODE_PRIVATE)

        // Firebase is initialized in VibeApplication
        auth = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth initialization failed", e)
            null
        }
        
        if (auth == null) {
            showSnackbar("Firebase Auth not available. Check google-services.json")
        }

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        initViews()
        setupListeners()
        loadAds()
        applyDynamicBranding()
    }

    private fun initViews() {
        llLoginOptions = findViewById(R.id.llLoginOptions)
        llOtpInput = findViewById(R.id.llOtpInput)
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin)
        btnSendOtp = findViewById(R.id.btnSendOtp)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)
        btnCancelOtp = findViewById(R.id.btnCancelOtp)
        etPhone = findViewById(R.id.etPhone)
        etOtp = findViewById(R.id.etOtp)
        ccp = findViewById(R.id.ccp)
        videoBackground = findViewById(R.id.videoBackground)
        tvTerms = findViewById(R.id.tvTerms)
        
        ccp.registerCarrierNumberEditText(etPhone)
        setupBackgroundVideo()
        setupTermsAndPrivacy()
    }

    private fun loadAds() {
        try {
            val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
            findViewById<com.google.android.gms.ads.AdView>(R.id.adViewTop)?.loadAd(adRequest)
            findViewById<com.google.android.gms.ads.AdView>(R.id.adViewBottom)?.loadAd(adRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyDynamicBranding() {
        lifecycleScope.launch(Dispatchers.IO) {
            val config = BrandingManager.fetchConfig(this@LoginActivity)
            val logoBitmap = BrandingManager.loadLogoBitmap(this@LoginActivity, config?.appLogo)
            
            withContext(Dispatchers.Main) {
                config?.let { cfg ->
                    // Apply Subtitle/Hero Text
                    if (!cfg.heroText.isNullOrBlank()) {
                        findViewById<TextView>(R.id.tvSubtitle)?.text = cfg.heroText
                    }
                    
                    // Apply Logo
                    logoBitmap?.let {
                        findViewById<ImageView>(R.id.ivLogo)?.setImageBitmap(it)
                    }

                    // Apply Video Background if URL provided
                    if (!cfg.loginVideo.isNullOrBlank()) {
                        setupBackgroundVideo(cfg.loginVideo)
                    }

                    // Update Terms/Privacy links
                    setupTermsAndPrivacy(cfg.termsUrl, cfg.privacyUrl)
                }
            }
        }
    }

    private fun setupBackgroundVideo(videoUrl: String? = null) {
        try {
            val uri = if (videoUrl != null) {
                Uri.parse(videoUrl)
            } else {
                Uri.parse("android.resource://" + packageName + "/" + R.raw.login_bg)
            }
            videoBackground.setVideoURI(uri)
            videoBackground.setOnPreparedListener { mp ->
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                // ScalableVideoView handles the fit/fill automatically in onMeasure
                videoBackground.setVideoSize(mp.videoWidth, mp.videoHeight)
            }
            videoBackground.setOnInfoListener { mp, what, extra ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    videoBackground.setVideoSize(mp.videoWidth, mp.videoHeight)
                }
                false
            }
            videoBackground.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupTermsAndPrivacy(termsUrl: String? = null, privacyUrl: String? = null) {
        val fullText = "By continuing, you agree to our Terms of Services\nand Privacy Policy."
        val spannableString = SpannableString(fullText)

        val termsClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = termsUrl ?: "https://vibe.deepverse.cloud/terms"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = Color.WHITE
            }
        }

        val privacyClick = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = privacyUrl ?: "https://vibe.deepverse.cloud/privacy"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
                ds.color = Color.WHITE
            }
        }

        val termsStart = fullText.indexOf("Terms of Services")
        val termsEnd = termsStart + "Terms of Services".length
        val privacyStart = fullText.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length

        if (termsStart != -1) {
            spannableString.setSpan(termsClick, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (privacyStart != -1) {
            spannableString.setSpan(privacyClick, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        tvTerms.text = spannableString
        tvTerms.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onResume() {
        super.onResume()
        videoBackground.start()
    }

    private fun setupListeners() {
        btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        btnCancelOtp.setOnClickListener {
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
                    Log.e(TAG, "Phone Auth failed: ${e.message}", e)
                    val msg = when (e) {
                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid phone number."
                        is com.google.firebase.FirebaseTooManyRequestsException -> "Too many requests. Try again later."
                        else -> "Verification failed: ${e.message}"
                    }
                    showSnackbar(msg)
                    btnSendOtp.isEnabled = true
                    
                    // Specific hint for user/developer
                    if (e.message?.contains("SHA-1") == true || e.message?.contains("SHA1") == true) {
                        showSnackbar("Error: SHA-1 not configured in Firebase Console.")
                    }
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
        llOtpInput.visibility = View.GONE
        btnSendOtp.isEnabled = true
    }

    private fun showOtpInput() {
        llLoginOptions.visibility = View.GONE
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
                val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
                val response = withContext(Dispatchers.IO) {
                    val randomUsername = com.vibe.util.UsernameGenerator.generate()
                    ApiClient.api.loginApp(
                        LoginAppRequest(
                            uid = user.uid,
                            phone = user.phoneNumber,
                            email = user.email,
                            username = randomUsername,
                            deviceId = deviceId
                        )
                    ).execute()
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    SessionManager.saveToken(body.token)
                    body.user?.id?.let { SessionManager.saveUserId(it) }
                    
                    withContext(Dispatchers.Main) {
                        // showSnackbar("Connected!")
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
