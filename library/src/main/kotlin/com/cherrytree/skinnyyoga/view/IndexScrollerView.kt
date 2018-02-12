package com.cherrytree.skinnyyoga.view

import android.content.Context
import android.os.Build
import android.support.annotation.AttrRes
import android.support.annotation.RequiresApi
import android.support.annotation.StyleRes
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * Created by brownsoo on 2017. 8. 23..
 */

class IndexScrollerView : AbsRecyclerViewScroller {

    private var indexIndicator: IndexIndicator? = null

    interface IndexIndicator {
        val isShown: Boolean
        val isAvailable: Boolean
        fun moveToPosition(scrollProgress: Float)
        fun show()
        fun hide()
        fun updateIndex(current: Int, total: Int)
    }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}

    constructor(context: Context,
                attrs: AttributeSet?,
                @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context,
                attrs: AttributeSet?,
                @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    fun setIndexIndicator(indexIndicator: IndexIndicator?) {
        this.indexIndicator = indexIndicator
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val recyclerView = getRecyclerView()
        if (recyclerView != null && indexIndicator != null && indexIndicator!!.isShown) {
            val scrollProgress = recyclerViewCalculator!!
                    .calculateScrollProgress(recyclerView)
            onScrollProgressChanged(scrollProgress)
        }
    }

    override fun onScrollProgressChanged(scrollProgress: Float) {
        var handled = false
        val recyclerView = getRecyclerView()
        if (indexIndicator == null || !indexIndicator!!.isAvailable) {
            if (indexIndicator != null && indexIndicator!!.isShown) {
                indexIndicator!!.hide()
            }
            return
        }
        if (recyclerView != null) {
            var total = 0
            val adapter = recyclerView.adapter
            if (adapter != null) {
                total = adapter.itemCount
            }
            var firstVisibleIndex = RecyclerView.NO_POSITION
            val layoutManager = recyclerView.layoutManager
            if (layoutManager is LinearLayoutManager) {
                firstVisibleIndex = layoutManager
                        .findFirstVisibleItemPosition()
            }
            if (total > 0 && firstVisibleIndex != RecyclerView.NO_POSITION) {
                if (!indexIndicator!!.isShown) {
                    indexIndicator!!.show()
                }
                indexIndicator!!.moveToPosition(scrollProgress)
                indexIndicator!!.updateIndex(firstVisibleIndex, total)
                handled = true
            }
        }
        if (!handled && indexIndicator!!.isShown) {
            indexIndicator!!.hide()
        }
    }

    override fun createRecyclerViewCalculator(): RecyclerViewCalculator {
        return SimpleRecyclerViewCalculator()
    }

    override fun onScrollerTouched(event: MotionEvent): Boolean {
        return false
    }
}
