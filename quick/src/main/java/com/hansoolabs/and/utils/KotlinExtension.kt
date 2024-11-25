package com.hansoolabs.and.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.hansoolabs.and.R
import kotlin.math.roundToInt

object ClickThrottle {
    var lastClickTime: Long = 0
}

fun View.onClickThrottle(throttle: Long = 600L, action: (View) -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long
            set(value) {
                ClickThrottle.lastClickTime = value
            }
            get() = ClickThrottle.lastClickTime
        
        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < throttle) return
            else action(v)
            
            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}

fun Any.getClassInstanceName(): String {
    return "${this.javaClass.simpleName}@${Integer.toHexString(this.hashCode())}"
}

fun Number.dp2px(): Int {
    val metrics = Resources.getSystem().displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), metrics).roundToInt()
}


fun Uri.resolveByActivity(activity: Activity): Boolean {
    if (this.scheme != null) {
        val intent = Intent(Intent.ACTION_VIEW, this)
        try {
            val chooser = Intent.createChooser(intent, "")
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(chooser)
            } else {
                activity.startActivity(intent)
            }
        } catch (e: ActivityNotFoundException) {
            MaterialAlertDialogBuilder(activity)
                .setMessage(R.string.error__not_found_proper_app)
                .create()
                .show()
        }
        return true
    }
    return false
}

fun Activity.isLive() = !this.isFinishing && !this.isDestroyed

fun Fragment.isLive() = isAdded && activity?.isLive() == true

fun View.isVisible() = this.visibility == View.VISIBLE

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) {
        View.VISIBLE
    } else {
        View.GONE
    }
}

fun Context.isNightMode(): Boolean {
    val currentNightMode: Int = this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return when (currentNightMode) {
        Configuration.UI_MODE_NIGHT_YES -> true
        else -> false
    }
}

@Suppress("DEPRECATION")
fun Context.isInternetAvailable(): Boolean {
    var result = false
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        cm?.run {
            cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                result = when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            }
        }
    } else {
        cm?.run {
            cm.activeNetworkInfo?.run {
                if (type == ConnectivityManager.TYPE_WIFI) {
                    result = true
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    result = true
                }
            }
        }
    }
    return result
}

fun Context.versionName(): String {
    return try {
        val manager = this.packageManager
        val info = manager.getPackageInfo(this.packageName, 0)
        info.versionName ?: "0.0.0"
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