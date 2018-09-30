package com.hansoolabs.and.mvp

import androidx.annotation.StringRes
import androidx.annotation.UiThread
import com.hansoolabs.and.Available
import io.reactivex.Observable
import io.reactivex.Scheduler

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */

open class MvpContract {

    interface View : Available {

        val isForeground: Boolean
        val uiScheduler: Scheduler

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

        fun foreground(): Observable<Boolean>
        fun addForegroundListener(listener: ViewForegroundListener)
        fun removeForegroundListener(listener: ViewForegroundListener)

        fun <T> withProgressDialog(observable: Observable<T>): Observable<T>
        fun <T> withProgressDialog(observable: Observable<T>, message: String): Observable<T>
        fun <T> withProgressDialog(observable: Observable<T>,
                                   title: String,
                                   message: String): Observable<T>

        fun <T> bindUntilViewDestroy(observable: Observable<T>): Observable<T>
        fun <T> bindUntilViewForeground(observable: Observable<T>): Observable<T>

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
