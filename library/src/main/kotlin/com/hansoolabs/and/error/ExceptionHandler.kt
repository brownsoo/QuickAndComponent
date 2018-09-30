package com.hansoolabs.and.error

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity

/**
 *
 * Created by brownsoo on 2017. 8. 3..
 */

abstract class ExceptionHandler {

    private val handlerMap = HashMap<String, (throwable: Throwable, data: Bundle?) -> Boolean>()
    protected val delegate: ContextDelegate
    protected val TAG_NULL = "null"

    abstract val resolving: Boolean
    abstract fun onError(throwable: Throwable, tag: String?, data: Bundle?): Boolean

    constructor(activity: AppCompatActivity) {
        this.delegate = WeakActivityDelegate(activity)
    }

    constructor(fragment: Fragment) {
        this.delegate = WeakFragmentDelegate(fragment)
    }

    open fun onAlertDialogResult(tag: String, resultCode: Int, resultData: Bundle?): Boolean {
        return false
    }

    protected val handlers: Map<String, (throwable: Throwable, data: Bundle?) -> Boolean>
        get() = handlerMap

    @JvmOverloads
    open fun addHandler(tag: String? = null,
                             handler: (throwable: Throwable, data: Bundle?) -> Boolean): ExceptionHandler {
        handlerMap.put(tag ?: TAG_NULL, handler)
        return this
    }

    open fun removeHandler(tag: String): ExceptionHandler {
        handlerMap.remove(tag)
        return this
    }

    open fun removeHandler(handler: (throwable: Throwable, data: Bundle?) -> Boolean)
            : ExceptionHandler {
        handlerMap.values.remove(handler)
        return this
    }

}