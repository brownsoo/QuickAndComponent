package com.hansoolabs.and.mvp

import android.os.Handler
import android.os.Looper
import com.hansoolabs.and.RequestCallback
import com.hansoolabs.and.error.ExceptionHandler
import java.lang.ref.WeakReference

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */

open class MvpPresenter<T : MvpContract.View>(presentingView: T,
                                              protected val exceptionHandler: ExceptionHandler?)
    : MvpContract.Presenter, MvpContract.ViewForegroundListener {

    private val delayedCallbacks = ArrayList<DelayedCallback<*>>()
    protected val viewRef: WeakReference<T> = WeakReference(presentingView)

    protected fun getView(): T? = viewRef.get()
    
    protected fun viewAccessibleDo(run: () -> Unit) {
        if (getView() != null) run.invoke()
    }
    override fun initialize() {
        getView()?.addForegroundListener(this)
    }

    override fun terminate() {
        getView()?.removeForegroundListener(this)
        viewRef.clear()
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

    fun getString(resId: Int): String = getView()?.getString(resId) ?: ""

    fun getString(resId: Int, vararg args: Any): String = getView()?.getString(resId, args) ?: ""


    fun <K> delayUntilViewForeground(callback: RequestCallback<K>) : RequestCallback<K> {

        return object : RequestCallback<K> {
            override fun onSuccess(result: K?) {
                if (getView()?.isForeground == true) {
                    callback.onSuccess(result)
                }
                else {
                    synchronized(delayedCallbacks) {
                        delayedCallbacks.add(DelayedCallback(result, callback))
                    }
                }
            }

            override fun onFailure(e: Exception) {
                if (getView()?.isForeground == true) {
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
            handler = Handler(Looper.myLooper()!!)
        }

        constructor(e: Exception, callback: RequestCallback<T>) {
            this.value = null
            this.e = e
            this.callback = callback
            success = false
            handler = Handler(Looper.myLooper()!!)
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
