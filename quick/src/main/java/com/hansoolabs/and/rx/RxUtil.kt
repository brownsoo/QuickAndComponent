package com.hansoolabs.and.rx

import android.os.Looper
import io.reactivex.rxjava3.core.Observer

/**
 * Created by brownsoo on 2017. 9. 11..
 */

object RxUtil {

    fun checkNotNull(value: Any?, message: String) {
        if (value == null) {
            throw NullPointerException(message)
        }
    }

    fun checkMainThread(observer: Observer<*>): Boolean {
        if (Looper.myLooper() !== Looper.getMainLooper()) {
            observer.onError(IllegalStateException(
                    "Expected to be called on the main thread but was " + Thread.currentThread().name))
            return false
        }
        return true
    }

}