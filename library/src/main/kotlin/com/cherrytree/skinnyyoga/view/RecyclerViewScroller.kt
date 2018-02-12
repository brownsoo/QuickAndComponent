package com.cherrytree.skinnyyoga.view

import android.support.v7.widget.RecyclerView

/**
 * Set RecyclerView, Scroll it
 * Created by brownsoo on 2017. 8. 23..
 */

interface RecyclerViewScroller {
    val onScrollListener: RecyclerView.OnScrollListener
    fun setRecyclerView(recyclerView: RecyclerView)
    fun scrollTo(scrollProgress: Float, fromTouch: Boolean)
}
