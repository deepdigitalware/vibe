package com.vibe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.vibe.R

class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)

        val images = intent.getIntegerArrayListExtra("images") ?: arrayListOf()
        val startPosition = intent.getIntExtra("position", 0)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val btnClose = findViewById<ImageView>(R.id.btnClose)
        val btnLeft = findViewById<ImageView>(R.id.btnLeft)
        val btnRight = findViewById<ImageView>(R.id.btnRight)

        val adapter = FullScreenAdapter(images)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(startPosition, false)

        btnClose.setOnClickListener { finish() }

        // Navigation Arrows Logic
        btnLeft.setOnClickListener {
            val current = viewPager.currentItem
            if (current > 0) viewPager.currentItem = current - 1
        }

        btnRight.setOnClickListener {
            val current = viewPager.currentItem
            if (current < images.size - 1) viewPager.currentItem = current + 1
        }
        
        // Hide/Show arrows based on position
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                btnLeft.visibility = if (position > 0) View.VISIBLE else View.GONE
                btnRight.visibility = if (position < images.size - 1) View.VISIBLE else View.GONE
            }
        })
        
        // Initial state
        btnLeft.visibility = if (startPosition > 0) View.VISIBLE else View.GONE
        btnRight.visibility = if (startPosition < images.size - 1) View.VISIBLE else View.GONE
    }

    class FullScreenAdapter(private val images: List<Int>) : RecyclerView.Adapter<FullScreenAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivFullImage: ImageView = view.findViewById(R.id.ivFullImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fullscreen_image, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            Glide.with(holder.itemView.context)
                .load(images[position])
                .into(holder.ivFullImage)
        }

        override fun getItemCount() = images.size
    }
}