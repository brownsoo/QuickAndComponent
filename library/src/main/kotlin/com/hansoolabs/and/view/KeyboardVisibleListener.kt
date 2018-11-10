package com.hansoolabs.and.view

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment

/**
 * Created by brownsoo on 2018-11-11.
 */

interface OnKeyboardVisibleListener {
    fun onKeyboardVisible(visible: Boolean)
}

class KeyboardVisibleListener {
    
    constructor(fragment: Fragment,
                listener: OnKeyboardVisibleListener? = null) {
        root = fragment.view
        this.listener = listener
    }
    
    constructor(activity: Activity,
                listener: OnKeyboardVisibleListener? = null) {
        root = activity.window.decorView.findViewById(android.R.id.content)
        this.listener = listener
    }
    
    private var root: View? = null
    var keyboardVisible: Boolean = false
        private set
    var listener: OnKeyboardVisibleListener? = null
    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val r = Rect()
        root?.getWindowVisibleDisplayFrame(r)
        val screenHeight = root?.rootView?.height ?: return@OnGlobalLayoutListener
        // r.bottom is the position above soft keypad or device button.
        // if keypad is keyboardVisible, the r.bottom is smaller than that before.
        val keyboardHeight = screenHeight - r.bottom
        var showing = false
        if (keyboardHeight > screenHeight * 0.15) {
            showing = true
        } else if (keyboardHeight <= screenHeight * 0.15) {
            showing = false
        }
        if (keyboardVisible != showing) {
            keyboardVisible = showing
            listener?.onKeyboardVisible(keyboardVisible)
        }
    }
    
    fun listen() {
        val observer = root?.viewTreeObserver
        if (observer?.isAlive != true) {
            return
        }
        observer.addOnGlobalLayoutListener(layoutListener)
    }
    
    fun avoid() {
        root?.viewTreeObserver?.removeOnGlobalLayoutListener(layoutListener)
    }
    
}