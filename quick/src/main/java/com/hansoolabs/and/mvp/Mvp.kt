package com.hansoolabs.and.mvp

import androidx.annotation.StringRes
import androidx.annotation.UiThread

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */

open class Mvp {

    interface View {

        val isForeground: Boolean

        fun getString(@StringRes resId: Int): String
        fun getString(@StringRes resId: Int, vararg formatArgs: Any): String

        @UiThread
        fun showProgressMsg()
        @UiThread
        fun showProgressMsg(message: String?)
        @UiThread
        fun showProgressMsg(title: String?, message: String?)
        @UiThread
        fun hideProgressMsg()
        @UiThread
        fun hideKeyboard()

        fun showToast(message: String)
        fun showToast(@StringRes resId: Int)

        fun addForegroundListener(listener: ViewForegroundListener)
        fun removeForegroundListener(listener: ViewForegroundListener)
    }

    interface Presenter {
        fun initialize()
        fun terminate()
    }

    interface ViewForegroundListener {
        fun onViewForeground()
        fun onViewBackground()
    }
}
