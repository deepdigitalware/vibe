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
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)

        var width = widthSpecSize
        var height = heightSpecSize

        if (videoWidth > 0 && videoHeight > 0) {
            val viewAspectRatio = widthSpecSize.toFloat() / heightSpecSize.toFloat()
            val videoAspectRatio = videoWidth.toFloat() / videoHeight.toFloat()

            if (videoAspectRatio > viewAspectRatio) {
                // Video is wider than the view, scale by height
                width = (heightSpecSize * videoAspectRatio).toInt()
                height = heightSpecSize
            } else {
                // Video is taller than the view, scale by width
                width = widthSpecSize
                height = (widthSpecSize / videoAspectRatio).toInt()
            }
        }
        setMeasuredDimension(width, height)
    }
}
