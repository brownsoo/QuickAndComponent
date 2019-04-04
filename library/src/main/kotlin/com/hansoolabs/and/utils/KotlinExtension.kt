package com.hansoolabs.and.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import androidx.fragment.app.Fragment
import android.view.View
import com.google.android.material.textfield.TextInputLayout

/**
 * Created by brownsoo on 2017. 8. 3..
 */


fun Activity.isLive() = !this.isFinishing && !this.isDestroyed

fun Fragment.isLive() = isAdded && activity?.isFinishing == false

fun View.isVisible() = this.visibility == View.VISIBLE

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) {
        View.VISIBLE
    } else {
        View.GONE
    }
}

fun Context.isOnline(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val netInfo = cm.activeNetworkInfo
    return netInfo != null && netInfo.isConnected
}

fun Context.versionName(): String {
    return try {
        val manager = this.packageManager
        val info = manager.getPackageInfo(this.packageName, 0)
        info.versionName
    } catch (e: Throwable) {
        "0.0.0"
    }
}

fun TextInputLayout.showError(msg: CharSequence?) {
    this.error = msg
    this.isErrorEnabled = true
}

fun TextInputLayout.hideError() {
    this.error = null
    this.isErrorEnabled = false
}