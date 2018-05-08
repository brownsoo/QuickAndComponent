package com.cherrytree.skinnyyoga.util

import android.app.Activity
import android.support.v4.app.Fragment
import android.view.View

/**
 * Created by brownsoo on 2017. 8. 3..
 */


fun Activity.isAvailable() = !this.isFinishing

fun Fragment.isAvailable() = isAdded && !(activity?.isFinishing ?: true)

fun View.isVisible() = this.visibility == View.VISIBLE

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) {
        View.VISIBLE
    } else {
        View.GONE
    }
}