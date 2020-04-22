package com.hansoolabs.and.view

import android.view.MotionEvent

/**
 * Convert scrollProgress ratio with y position Or versus
 * Created by brownsoo on 2017. 8. 23..
 */

class ScrollerCalculator(private var minScrollY: Float,
                         private var maxScrollY: Float) {

    fun updateScrollBounds(minScrollY: Float, maxScrollY: Float) {
        this.minScrollY = minScrollY
        this.maxScrollY = maxScrollY
    }

    fun calculateYFromScrollProgress(scrollProgress: Float): Float {
        return (maxScrollY - minScrollY) * scrollProgress + minScrollY
    }

    fun calculateScrollProgress(event: MotionEvent): Float {
        val height = maxScrollY - minScrollY
        if (height == 0f) {
            return 0.0f
        }
        var y = event.y
        y = Math.min(Math.max(minScrollY, y), maxScrollY) - minScrollY
        return y / height
    }
}
