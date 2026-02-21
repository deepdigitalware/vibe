package com.vibe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vibe.R
import com.vibe.utils.showSnackbar
import kotlinx.coroutines.*
import kotlin.random.Random
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

import com.bumptech.glide.Glide
import com.vibe.api.ApiClient
import com.vibe.api.ProfileResponse
import com.vibe.api.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private var viewsCount = 340
    private var job: Job? = null
    private lateinit var rvPhotos: RecyclerView
    private lateinit var ivAvatar: android.widget.ImageView
    private lateinit var ivCover: android.widget.ImageView
    private lateinit var tvName: TextView
    private lateinit var tvBalance: TextView
    private val galleryPhotos = mutableListOf<String>()
    private lateinit var photoAdapter: PhotoAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        rvPhotos = view.findViewById(R.id.rvPhotos)
        ivAvatar = view.findViewById(R.id.ivProfileImage)
        ivCover = view.findViewById(R.id.ivCoverImage)
        tvName = view.findViewById(R.id.tvName)
        tvBalance = view.findViewById(R.id.btnBalance)
        
        val btnEditProfile = view.findViewById<View>(R.id.btnEditProfile)
        val btnRecharge = view.findViewById<View>(R.id.btnRecharge)
        
        val tvLikesCount = view.findViewById<TextView>(R.id.tvLikesCount)
        val tvMatchesCount = view.findViewById<TextView>(R.id.tvMatchesCount)
        val tvViewsCount = view.findViewById<TextView>(R.id.tvViewsCount)

        // Setup Photo Adapter
        photoAdapter = PhotoAdapter(galleryPhotos)
        rvPhotos.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvPhotos.addItemDecoration(HorizontalMarginItemDecoration(8))
        rvPhotos.adapter = photoAdapter

        btnEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        val walletListener = View.OnClickListener {
            startActivity(android.content.Intent(context, RechargeActivity::class.java))
        }
        
        btnRecharge.setOnClickListener(walletListener)
        tvBalance.setOnClickListener(walletListener)

        loadProfileData()

        // Initialize Stats (Simulated for now)
        tvLikesCount.text = (100..500).random().toString()
        tvMatchesCount.text = (20..100).random().toString()
        tvViewsCount.text = viewsCount.toString()

        // Hide location if it's default
        view.findViewById<TextView>(R.id.tvLocation)?.text = "Online"

        // Simulate Real-time Views
        startRealtimeViewsSimulation(tvViewsCount)

        // Load Banners
        val adViewSmart = view.findViewById<AdView>(R.id.adViewSmart)
        val adViewLarge = view.findViewById<AdView>(R.id.adViewLarge)
        val bannerRequest = AdRequest.Builder().build()
        adViewSmart.loadAd(bannerRequest)
        adViewLarge.loadAd(bannerRequest)

        return view
    }

    private fun loadProfileData() {
        val userId = SessionManager.getUserId() ?: return
        ApiClient.api.getProfile(userId).enqueue(object : Callback<ProfileResponse> {
            override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val p = response.body()!!
                    tvName.text = p.name
                    tvBalance.text = "Credit: ₹${p.balance.toInt()}"
                    
                    Glide.with(this@ProfileFragment)
                        .load(p.avatar)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(ivAvatar)
                        
                    Glide.with(this@ProfileFragment)
                        .load(p.cover)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(ivCover)
                        
                    p.gallery?.let {
                        galleryPhotos.clear()
                        galleryPhotos.addAll(it)
                        photoAdapter.notifyDataSetChanged()
                        
                        val tvNoPhotos = view?.findViewById<TextView>(R.id.tvNoPhotos)
                        if (it.isEmpty()) {
                            tvNoPhotos?.visibility = View.VISIBLE
                            rvPhotos.visibility = View.GONE
                        } else {
                            tvNoPhotos?.visibility = View.GONE
                            rvPhotos.visibility = View.VISIBLE
                        }
                    } ?: run {
                        val tvNoPhotos = view?.findViewById<TextView>(R.id.tvNoPhotos)
                        tvNoPhotos?.visibility = View.VISIBLE
                        rvPhotos.visibility = View.GONE
                    }
                }
            }
            override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                // Ignore
            }
        })
    }

    private fun startRealtimeViewsSimulation(textView: TextView) {
        job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(Random.nextLong(2000, 5000)) // Random delay between 2-5 seconds
                viewsCount++
                textView.text = viewsCount.toString()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        job?.cancel()
    }

    class PhotoAdapter(private val photos: List<String>) : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: android.widget.ImageView = view.findViewById(R.id.ivImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_edit, parent, false)
            view.findViewById<View>(R.id.btnDelete).visibility = View.GONE
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Glide.with(holder.itemView.context)
                .load(photos[position])
                .centerCrop()
                .into(holder.imageView)
        }

        override fun getItemCount() = photos.size
    }

    class HorizontalMarginItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.right = spaceHeight
        }
    }
}
