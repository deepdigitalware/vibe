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

class ProfileFragment : Fragment() {

    private var viewsCount = 340
    private var job: Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val rvPhotos = view.findViewById<RecyclerView>(R.id.rvPhotos)
        val btnEditProfile = view.findViewById<View>(R.id.btnEditProfile)
        val btnRecharge = view.findViewById<View>(R.id.btnRecharge)
        val btnBalance = view.findViewById<TextView>(R.id.btnBalance)
        
        val tvLikesCount = view.findViewById<TextView>(R.id.tvLikesCount)
        val tvMatchesCount = view.findViewById<TextView>(R.id.tvMatchesCount)
        val tvViewsCount = view.findViewById<TextView>(R.id.tvViewsCount)

        // Setup Photo Adapter
        val dummyPhotos = listOf(
            android.R.drawable.star_big_on,
            android.R.drawable.star_big_off,
            android.R.drawable.btn_star_big_on,
            android.R.drawable.btn_star_big_off
        )
        val photoAdapter = PhotoAdapter(dummyPhotos)
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
        btnBalance.setOnClickListener(walletListener)

        // Set Balance
        val prefs = requireContext().getSharedPreferences("vibe_prefs", android.content.Context.MODE_PRIVATE)
        val balance = prefs.getFloat("wallet_balance", 0f)
        btnBalance.text = "Credit: ₹${balance.toInt()}"

        // Initialize Stats
        tvLikesCount.text = "125"
        tvMatchesCount.text = "45"
        tvViewsCount.text = viewsCount.toString()

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

    class PhotoAdapter(private val photos: List<Int>) : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: android.widget.ImageView = view.findViewById(R.id.ivImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Using item_gallery_edit layout which has an ImageView with id ivImage
            // Or create a simple item layout for display
            // For now reusing item_gallery_edit but hiding delete button if possible or just ignoring it
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_edit, parent, false)
            view.findViewById<View>(R.id.btnDelete).visibility = View.GONE
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.imageView.setImageResource(photos[position])
        }

        override fun getItemCount() = photos.size
    }

    class HorizontalMarginItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.right = spaceHeight
        }
    }
}
