package com.hansoolabs.and.view

import android.support.design.widget.FloatingActionButton
import android.view.View

import com.hansoolabs.and.utils.UiUtil

/**
 *
 * Created by brownsoo on 2017. 8. 23..
 */

class ScrollToTopFabManager(private val fab: FloatingActionButton) {
    private val threshold: Int

    init {
        threshold = UiUtil.dp2px(THRESHOLD)
    }

    fun onScroll(scrollTo: Int, dy: Int) {
        if (fab.visibility == View.VISIBLE && scrollTo < threshold) {
            fab.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
                override fun onHidden(fab: FloatingActionButton?) {
                    super.onHidden(fab)
                    fab!!.visibility = View.INVISIBLE
                }
            })
        } else if (scrollTo >= threshold && fab.visibility != View.VISIBLE) {
            fab.show()
        }
    }

    companion object {

        private const val THRESHOLD = 50f // in dp
    }
}
