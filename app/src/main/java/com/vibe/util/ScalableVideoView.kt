package com.vibe.util

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec
import android.widget.VideoView

class ScalableVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : VideoView(context, attrs, defStyle) {

    private var videoWidth = 0
    private var videoHeight = 0

    fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = getDefaultSize(videoWidth, widthMeasureSpec)
        var height = getDefaultSize(videoHeight, heightMeasureSpec)

        if (videoWidth > 0 && videoHeight > 0) {
            val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
            val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
            val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)

            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                // Fill and crop logic
                width = widthSpecSize
                height = heightSpecSize

                // for center crop
                if (videoWidth * height < width * videoHeight) {
                    // video is too wide, crop left/right
                    // height is already set to heightSpecSize
                } else if (videoWidth * height > width * videoHeight) {
                    // video is too tall, crop top/bottom
                }
            }
        }
        setMeasuredDimension(width, height)
    }
}
