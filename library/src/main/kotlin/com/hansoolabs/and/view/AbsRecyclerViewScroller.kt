package com.hansoolabs.and.view

import android.content.Context
import android.os.Build
import android.support.annotation.AttrRes
import android.support.annotation.RequiresApi
import android.support.annotation.StyleRes
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

/**
 * Created by brownsoo on 2017. 8. 23..
 */

abstract class AbsRecyclerViewScroller : FrameLayout, RecyclerViewScroller {

    protected var recyclerViewCalculator: RecyclerViewCalculator? = null
        private set
    protected var scrollerCalculator: ScrollerCalculator? = null
        private set
    private var recyclerView: RecyclerView? = null

    override val onScrollListener: RecyclerView.OnScrollListener
        get() = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (Math.abs(dy) > 0) {
                    val progress = recyclerViewCalculator!!.calculateScrollProgress(recyclerView!!)
                    onScrollProgressChanged(progress)
                }
            }
        }

    constructor(
            context: Context) : super(context) {
        init(context, null, 0)
    }

    constructor(context: Context,
                attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0)
    }

    constructor(context: Context,
                attrs: AttributeSet?,
                @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs, defStyleAttr)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context,
                attrs: AttributeSet?,
                @AttrRes defStyleAttr: Int,
                @StyleRes defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr)
    }

    abstract fun onScrollProgressChanged(scrollProgress: Float)

    abstract fun createRecyclerViewCalculator(): RecyclerViewCalculator

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        recyclerViewCalculator = createRecyclerViewCalculator()
        setOnTouchListener { v, event -> onScrollerTouched(event) }
    }

    open fun onScrollerTouched(event: MotionEvent): Boolean {
        if (scrollerCalculator != null) {
            scrollTo(scrollerCalculator!!.calculateScrollProgress(event), true)
            return true
        }
        return false
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (scrollerCalculator == null) {
            scrollerCalculator = ScrollerCalculator(top.toFloat(), (top + height).toFloat())
        } else {
            scrollerCalculator!!.updateScrollBounds(top.toFloat(), (top + height).toFloat())
        }
    }

    override fun setRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        recyclerView.addOnScrollListener(onScrollListener)
    }

    override fun scrollTo(scrollProgress: Float, fromTouch: Boolean) {
        if (recyclerView != null) {
            val position = recyclerViewCalculator!!
                    .calculatePositionFromScrollProgress(recyclerView!!, scrollProgress)
            recyclerView!!.scrollToPosition(position)
        }
    }

    protected fun getRecyclerView(): RecyclerView? = recyclerView
}
