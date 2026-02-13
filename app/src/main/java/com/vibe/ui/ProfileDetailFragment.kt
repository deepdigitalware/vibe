package com.vibe.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.AdView
import com.vibe.R
import com.vibe.utils.showSnackbar

class ProfileDetailFragment : Fragment() {

    private var name: String = "Unknown"
    private var age: Int = 20
    private var distance: String = "Unknown"
    private var bio: String = ""
    private var city: String = "Unknown"
    private var country: String = "Unknown"
    private var countryCode: String = "US"
    private var isOnline: Boolean = false
    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "ProfileDetailFragment"

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_AGE = "age"
        private const val ARG_DISTANCE = "distance"
        private const val ARG_BIO = "bio"
        private const val ARG_COUNTRY_CODE = "country_code"
        private const val ARG_CITY = "city"
        private const val ARG_COUNTRY = "country"
        private const val ARG_ONLINE = "online"

        fun newInstance(name: String, age: Int, distance: String, bio: String, countryCode: String = "US", isOnline: Boolean = false, city: String = "New York", country: String = "USA") =
            ProfileDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, name)
                    putInt(ARG_AGE, age)
                    putString(ARG_DISTANCE, distance)
                    putString(ARG_BIO, bio)
                    putString(ARG_COUNTRY_CODE, countryCode)
                    putBoolean(ARG_ONLINE, isOnline)
                    putString(ARG_CITY, city)
                    putString(ARG_COUNTRY, country)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.setBottomNavVisibility(false)
    }

    override fun onStop() {
        super.onStop()
        (activity as? MainActivity)?.setBottomNavVisibility(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            name = it.getString(ARG_NAME, "Unknown")
            age = it.getInt(ARG_AGE, 20)
            distance = it.getString(ARG_DISTANCE, "Unknown")
            bio = it.getString(ARG_BIO, "")
            countryCode = it.getString(ARG_COUNTRY_CODE, "US")
            isOnline = it.getBoolean(ARG_ONLINE, false)
            city = it.getString(ARG_CITY, "New York")
            country = it.getString(ARG_COUNTRY, "USA")
        }
        
        loadInterstitialAd()
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(requireContext(), getString(R.string.admob_interstitial_id), adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                    setAdCallbacks()
                }
            })
    }
    
    private fun setAdCallbacks() {
        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed.")
                navigateToChat()
                mInterstitialAd = null
                loadInterstitialAd() // Preload next one
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Log.d(TAG, "Ad failed to show.")
                mInterstitialAd = null
                navigateToChat()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
                mInterstitialAd = null
            }
        }
    }

    private fun navigateToChat() {
        // Navigate to ChatDetailFragment
        // In a real app, we'd pass the user ID/Name to the fragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ChatDetailFragment.newInstance(name, "Online"))
            .addToBackStack(null)
            .commit()
    }

    private fun getFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🇺🇸"
        val flagOffset = 0x1F1E6
        val asciiOffset = 0x41
        val firstChar = Character.codePointAt(countryCode, 0) - asciiOffset + flagOffset
        val secondChar = Character.codePointAt(countryCode, 1) - asciiOffset + flagOffset
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile_detail, container, false)

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvDetail = view.findViewById<TextView>(R.id.tvDetail)
        val tvAbout = view.findViewById<TextView>(R.id.tvAbout)
        val tvFlag = view.findViewById<TextView>(R.id.tvFlag)
        val ivOnlineStatus = view.findViewById<ImageView>(R.id.ivOnlineStatus)
        val ivCoverImage = view.findViewById<ImageView>(R.id.ivCoverImage)
        val ivProfileAvatar = view.findViewById<ImageView>(R.id.ivProfileAvatar)
        val btnLike = view.findViewById<ImageView>(R.id.btnLike)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val btnVideoCall = view.findViewById<Button>(R.id.btnVideoCall)
        val btnChat = view.findViewById<Button>(R.id.btnChat)
        val rvPhotos = view.findViewById<RecyclerView>(R.id.rvPhotos)

        tvName.text = "$name, $age"
        tvDetail.text = "$city, $country"
        tvAbout.text = bio.ifEmpty { "No bio available." }
        
        // Set flag
        tvFlag.text = getFlagEmoji(countryCode)
        
        // Set Online Status
        ivOnlineStatus.visibility = if (isOnline) View.VISIBLE else View.GONE
        
        // Load Profile Image with Palette for Adaptive Color
        val profileResId = R.drawable.ic_profile // Default
        
        // Use Glide to load and generate Palette
        com.bumptech.glide.Glide.with(this)
            .asBitmap()
            .load(profileResId) 
            .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                override fun onResourceReady(resource: android.graphics.Bitmap, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?) {
                    ivCoverImage.setImageBitmap(resource)
                    ivProfileAvatar.setImageBitmap(resource)
                    /* androidx.palette.graphics.Palette.from(resource).generate { palette ->
                        val vibrant = palette?.vibrantSwatch
                        val dominant = palette?.dominantSwatch
                        val color = vibrant?.rgb ?: dominant?.rgb ?: Color.DKGRAY
                        
                        viewAdaptiveBar.setBackgroundColor(color)
                    } */
                }
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
            })
            
        // Like Button Logic
        var isMatchSent = false
        btnLike.setOnClickListener {
            if (isMatchSent) {
                isMatchSent = false
                btnLike.setImageResource(R.drawable.ic_heart)
                view.showSnackbar("Match Request Cancelled")
            } else {
                isMatchSent = true
                btnLike.setImageResource(R.drawable.ic_check) // Using check as "Sent" indicator
                view.showSnackbar("Match Request Sent")
            }
        }

        // Setup Photo Adapter
        val dummyPhotos = listOf(
            R.drawable.ic_profile,
            R.drawable.ic_profile,
            R.drawable.ic_profile
        )
        val photoAdapter = PhotoAdapter(dummyPhotos) { position ->
            // Open Full Screen Image
            // We can pass the list and position
            val intent = android.content.Intent(context, FullScreenImageActivity::class.java)
            intent.putExtra("images", ArrayList(dummyPhotos)) // Passing resource IDs for now
            intent.putExtra("position", position)
            startActivity(intent)
        }
        rvPhotos.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvPhotos.addItemDecoration(HorizontalMarginItemDecoration(8))
        rvPhotos.adapter = photoAdapter

        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnVideoCall.setOnClickListener {
            // Start video call logic (deduct money)
            val intent = CallActivity.newIntent(requireContext(), role = "caller", roomId = "Room_${name.replace(" ", "_")}")
            startActivity(intent)
        }

        btnChat.setOnClickListener {
            view.showSnackbar("Opening chat with $name...")
            
            if (mInterstitialAd != null) {
                mInterstitialAd?.show(requireActivity())
            } else {
                Log.d(TAG, "The interstitial ad wasn't ready yet.")
                navigateToChat()
            }
        }

        // Load Banners
        val adViewSmart = view.findViewById<AdView>(R.id.adViewSmart)
        val adViewLarge = view.findViewById<AdView>(R.id.adViewLarge)
        val bannerRequest = AdRequest.Builder().build()
        adViewSmart.loadAd(bannerRequest)
        adViewLarge.loadAd(bannerRequest)
        
        return view
    }
    
    
    class PhotoAdapter(private val photos: List<Int>, private val onClick: (Int) -> Unit) : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: android.widget.ImageView = view.findViewById(R.id.ivImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_edit, parent, false)
            view.findViewById<View>(R.id.btnDelete).visibility = View.GONE
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.imageView.setImageResource(photos[position])
            holder.itemView.setOnClickListener { onClick(position) }
        }

        override fun getItemCount() = photos.size
    }

    class HorizontalMarginItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.right = spaceHeight
        }
    }
}
