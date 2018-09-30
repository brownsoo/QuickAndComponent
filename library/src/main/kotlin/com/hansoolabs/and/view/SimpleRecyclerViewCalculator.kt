package com.hansoolabs.and.view

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by brownsoo on 2017. 8. 23..
 */

class SimpleRecyclerViewCalculator : RecyclerViewCalculator {

    override fun calculatePositionFromScrollProgress(recyclerView: RecyclerView,
                                                     progress: Float): Int {
        val adapter = recyclerView.adapter
        return Math.round((adapter.itemCount - 1) * progress)
    }

    override fun calculateScrollProgress(recyclerView: RecyclerView): Float {
        val scrollTop = recyclerView.computeVerticalScrollOffset() + recyclerView.computeHorizontalScrollExtent()
        val maxScrollTop = recyclerView.computeVerticalScrollRange()
        return scrollTop / maxScrollTop.toFloat()
    }
}

