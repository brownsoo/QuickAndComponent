package com.cherrytree.skinnyyoga.view

import android.support.v7.widget.RecyclerView

/**
 * Get position from scrollProgress or
 * get scrollProgress from position of RecyclerView item
 * Created by brownsoo on 2017. 8. 23..
 */

interface RecyclerViewCalculator {
    fun calculatePositionFromScrollProgress(
            recyclerView: RecyclerView,
            progress: Float): Int

    fun calculateScrollProgress(
            recyclerView: RecyclerView): Float
}
