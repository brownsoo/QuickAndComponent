package com.hansoolabs.and.mvp

import android.os.Handler
import android.os.Looper
import com.hansoolabs.and.RequestCallback
import com.hansoolabs.and.error.ExceptionHandler

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */

open class BaseMvpPresenter<out T : BaseMvpContract.View>(protected val view: T,
                                                          protected val exceptionHandler: ExceptionHandler)
    : BaseMvpContract.Presenter, BaseMvpContract.ViewForegroundListener {

    private val delayedCallbacks = ArrayList<DelayedCallback<*>>()
    protected fun viewAccessibleDo(run: () -> Unit) {
        if (view.isForeground) run.invoke()
    }
    override fun initialize() {
        view.addForegroundListener(this)
    }

    override fun terminate() {
        view.removeForegroundListener(this)
    }

    override fun onViewForeground() {
        synchronized(delayedCallbacks) {
            for (callback in delayedCallbacks) {
                callback.fire()
            }
            delayedCallbacks.clear()
        }
    }

    override fun onViewBackground() {
        //
    }

    fun getString(resId: Int): String = view.getString(resId)

    fun getString(resId: Int, vararg args: Any): String = view.getString(resId, args)


    fun <K> delayUntilViewForeground(callback: RequestCallback<K>) : RequestCallback<K> {

        return object : RequestCallback<K> {
            override fun onSuccess(result: K?) {
                if (view.isForeground) {
                    callback.onSuccess(result)
                }
                else {
                    synchronized(delayedCallbacks) {
                        delayedCallbacks.add(DelayedCallback(result, callback))
                    }
                }
            }

            override fun onFailure(e: Exception) {
                if (view.isForeground) {
                    callback.onFailure(e)
                }
                else {
                    synchronized(delayedCallbacks) {
                        delayedCallbacks.add(DelayedCallback(e, callback))
                    }
                }
            }
        }

    }



    private class DelayedCallback<T> {
        private val success: Boolean
        private val callback: RequestCallback<T>
        private val value: T?
        private val e: Exception?
        private val handler: Handler

        constructor(value: T?, callback: RequestCallback<T>) {
            this.value = value
            this.e = null
            this.callback = callback
            success = true
            handler = Handler(Looper.myLooper())
        }

        constructor(e: Exception, callback: RequestCallback<T>) {
            this.value = null
            this.e = e
            this.callback = callback
            success = false
            handler = Handler(Looper.myLooper())
        }

        fun fire() {
            handler.post {
                if (success) {
                    callback.onSuccess(value)
                } else {
                    callback.onFailure(e as Exception)
                }
            }
        }
    }
}
