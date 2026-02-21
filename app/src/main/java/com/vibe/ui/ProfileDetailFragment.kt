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
import com.vibe.api.ApiClient
import com.vibe.api.ToggleFavouriteRequest
import com.vibe.api.ToggleFavouriteResponse
import com.vibe.api.ProfileResponse
import com.vibe.api.SessionManager
import com.vibe.utils.showSnackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileDetailFragment : Fragment() {

    private var userId: String = ""
    private var name: String = "Unknown"
    private var age: Int = 20
    private var distance: String = "Unknown"
    private var bio: String = ""
    private var city: String = "Unknown"
    private var country: String = "Unknown"
    private var countryCode: String = "US"
    private var isOnline: Boolean = false
    private var isFavourited: Boolean = false
    private var avatar: String? = null
    private var cover: String? = null
    private var gallery: List<String> = emptyList()

    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "ProfileDetailFragment"

    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val ARG_NAME = "name"
        private const val ARG_AGE = "age"
        private const val ARG_DISTANCE = "distance"
        private const val ARG_BIO = "bio"
        private const val ARG_COUNTRY_CODE = "country_code"
        private const val ARG_CITY = "city"
        private const val ARG_COUNTRY = "country"
        private const val ARG_ONLINE = "online"

        fun newInstance(userId: String, name: String, age: Int, bio: String, countryCode: String = "US", isOnline: Boolean = false, city: String = "New York", country: String = "USA") =
            ProfileDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                    putString(ARG_NAME, name)
                    putInt(ARG_AGE, age)
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
            userId = it.getString(ARG_USER_ID, "")
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
        val btnLike = view.findViewById<android.widget.ImageButton>(R.id.btnLike)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val btnVideoCall = view.findViewById<Button>(R.id.btnVideoCall)
        val btnChat = view.findViewById<Button>(R.id.btnChat)
        val rvPhotos = view.findViewById<RecyclerView>(R.id.rvPhotos)

        tvName.text = "$name, $age"
        tvDetail.text = "$city, $country"
        tvAbout.text = bio.ifEmpty { "No bio available." }
        tvFlag.text = getFlagEmoji(countryCode)
        ivOnlineStatus.visibility = if (isOnline) View.VISIBLE else View.GONE

        loadProfileData(view)

        btnLike.setOnClickListener {
            toggleFavourite(btnLike)
        }

        btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnVideoCall.setOnClickListener {
            val intent = CallActivity.newIntent(
                requireContext(), 
                role = "caller", 
                roomId = "Room_${name.replace(" ", "_")}",
                userName = name,
                userImage = avatar ?: ""
            )
            startActivity(intent)
        }

        btnChat.setOnClickListener {
            if (mInterstitialAd != null) {
                mInterstitialAd?.show(requireActivity())
            } else {
                navigateToChat()
            }
        }

        return view
    }

    private fun loadProfileData(view: View) {
        val currentUserId = SessionManager.getUserId()
        ApiClient.api.getProfile(userId, currentUserId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                if (response.isSuccessful) {
                    val profile = response.body() ?: return
                    name = profile.name
                    bio = profile.bio
                    avatar = profile.avatar
                    cover = profile.cover
                    gallery = profile.gallery ?: emptyList()
                    isFavourited = profile.is_favourited

                    updateUI(view)
                }
            }
            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                view.showSnackbar("Failed to load profile details")
            }
        })
    }

    private fun updateUI(view: View) {
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvAbout = view.findViewById<TextView>(R.id.tvAbout)
        val ivCoverImage = view.findViewById<ImageView>(R.id.ivCoverImage)
        val ivProfileAvatar = view.findViewById<ImageView>(R.id.ivProfileAvatar)
        val btnLike = view.findViewById<android.widget.ImageButton>(R.id.btnLike)
        val rvPhotos = view.findViewById<RecyclerView>(R.id.rvPhotos)

        tvName.text = "$name, $age"
        tvAbout.text = bio.ifEmpty { "No bio available." }

        if (isFavourited) {
            btnLike.setImageResource(R.drawable.ic_heart_filled)
            btnLike.setColorFilter(Color.RED)
        } else {
            btnLike.setImageResource(R.drawable.ic_heart_outline)
            btnLike.setColorFilter(Color.WHITE)
        }

        com.bumptech.glide.Glide.with(this)
            .load(cover)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(ivCoverImage)

        com.bumptech.glide.Glide.with(this)
            .load(avatar)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(ivProfileAvatar)

        val tvNoPhotos = view.findViewById<TextView>(R.id.tvNoPhotos)
        
        if (gallery.isEmpty()) {
            tvNoPhotos.visibility = View.VISIBLE
            rvPhotos.visibility = View.GONE
        } else {
            tvNoPhotos.visibility = View.GONE
            rvPhotos.visibility = View.VISIBLE
        }

        val photoAdapter = PhotoAdapter(gallery) { position ->
            val intent = android.content.Intent(context, FullScreenImageActivity::class.java)
            intent.putStringArrayListExtra("images", ArrayList(gallery))
            intent.putExtra("position", position)
            startActivity(intent)
        }
        rvPhotos.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvPhotos.adapter = photoAdapter
    }

    private fun toggleFavourite(btnLike: android.widget.ImageButton) {
        val currentUserId = SessionManager.getUserId() ?: return
        ApiClient.api.toggleFavourite(ToggleFavouriteRequest(currentUserId, userId))
            .enqueue(object : Callback<ToggleFavouriteResponse> {
                override fun onResponse(call: Call<ToggleFavouriteResponse>, response: Response<ToggleFavouriteResponse>) {
                    if (response.isSuccessful) {
                        isFavourited = response.body()?.action == "added"
                        if (isFavourited) {
                            btnLike.setImageResource(R.drawable.ic_heart_filled)
                            btnLike.setColorFilter(Color.RED)
                            view?.showSnackbar("Added to Favourites")
                        } else {
                            btnLike.setImageResource(R.drawable.ic_heart_outline)
                            btnLike.setColorFilter(Color.WHITE)
                            view?.showSnackbar("Removed from Favourites")
                        }
                    }
                }
                override fun onFailure(call: Call<ToggleFavouriteResponse>, t: Throwable) {
                    view?.showSnackbar("Failed to update favourite")
                }
            })
    }
    
    
    class PhotoAdapter(private val photos: List<String>, private val onClick: (Int) -> Unit) : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: android.widget.ImageView = view.findViewById(R.id.ivImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_edit, parent, false)
            view.findViewById<View>(R.id.btnDelete).visibility = View.GONE
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(photos[position])
                .centerCrop()
                .into(holder.imageView)
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
