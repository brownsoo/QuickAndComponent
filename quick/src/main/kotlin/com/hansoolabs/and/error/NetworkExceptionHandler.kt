package com.hansoolabs.and.error

import android.content.Context
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.appcompat.app.AppCompatActivity
import com.hansoolabs.and.app.QuickDialogFragment
import com.hansoolabs.and.R
import com.hansoolabs.and.app.QuickDialog
import com.hansoolabs.and.utils.StringUtil

/**
 * Handle network exception by delivering it to handler.
 * Created by brownsoo on 2017. 8. 3..
 */

class NetworkExceptionHandler : ExceptionHandler {

    override var resolving: Boolean = false

    interface RetryHandler {
        fun handleRetry(@NonNull throwable: Throwable, @Nullable data: Bundle)
    }

    private val retryHandlerMap = HashMap<String, RetryHandler>()

    constructor(activity: AppCompatActivity) : super(activity)

    constructor(fragment: Fragment) : super(fragment)

    override fun addHandler(tag: String?, handler: (throwable: Throwable, data: Bundle?) -> Boolean): NetworkExceptionHandler {
        return super.addHandler(tag, handler) as NetworkExceptionHandler
    }

    override fun removeHandler(tag: String): NetworkExceptionHandler {
        return super.removeHandler(tag) as NetworkExceptionHandler
    }

    override fun removeHandler(handler: (throwable: Throwable, data: Bundle?) -> Boolean): NetworkExceptionHandler {
        return super.removeHandler(handler) as NetworkExceptionHandler
    }

    fun addRetryHandler(@NonNull handler: RetryHandler, @Nullable tag: String?): NetworkExceptionHandler {
        retryHandlerMap.put(tag ?: TAG_NULL, handler)
        return this
    }

    fun removeRetryHandler(@NonNull tag: String): NetworkExceptionHandler {
        retryHandlerMap.remove(tag)
        return this
    }

    fun removeRetryHandler(@NonNull handler: RetryHandler): NetworkExceptionHandler {
        retryHandlerMap.values.remove(handler)
        return this
    }

    override fun onError(throwable: Throwable, tag: String?, data: Bundle?): Boolean {
        val e: BaseException? = BaseExceptionHandler.toCommonException(throwable)
        if (e?.error?.sector == BaseError.Sector.Network) {
            val key = tag ?: TAG_NULL
            val handler = handlers[key]
            if (handler?.invoke(e, data) == true) {
                return true
            }
            val context: Context? = delegate.context
            val fm: FragmentManager? = delegate.fragmentManager
            if (context != null && fm != null) {
                resolving = true
                val retryHandler = retryHandlerMap[key]
                val builder = QuickDialogFragment.BasicBuilder(context)
                        .setCancelable(false)
                        .setTitle(R.string.error__no_internet_dialog__title)
                        .setMessage(R.string.error__no_internet_dialog__msg)
                val bundle = Bundle().apply {
                    putSerializable(EXTRA_ERROR_THROWABLE, e)
                    putString(EXTRA_ERROR_TAG, key)
                    if (data != null) putAll(data)
                }
                if (bundle.keySet().isNotEmpty()) {
                    builder.setDefaultResultData(bundle)
                }
                if (retryHandler != null) {
                    builder.setPositiveButton(R.string.and__retry)
                            .setNegativeButton(R.string.and__close)
                            .show(fm, TAG_DIALOG)
                } else {
                    builder.setPositiveButton(R.string.and__close)
                            .show(fm)
                }
                return true
            }
        }
        return false
    }

    override fun onAlertDialogResult(tag: String,
                                     resultCode: Int,
                                     resultData: Bundle?): Boolean {
        if (TAG_DIALOG == tag) {
            resolving = false
            if (resultCode == QuickDialogFragment.RESULT_OK && resultData != null) {
                if (QuickDialog.isPositiveClick(resultData)) {
                    val key = resultData.getString(EXTRA_ERROR_TAG) ?: return false
                    retryHandlerMap[key]?.handleRetry(
                        throwable = resultData.getSerializable(EXTRA_ERROR_THROWABLE) as Throwable,
                        data = resultData)
                    return true
                }
            }
        }
        return false
    }

    companion object {
        private val TAG_DIALOG = StringUtil.constant("NetworkExceptionDialog")
        private val EXTRA_ERROR_TAG = StringUtil.constant("ExtraErrorTag")
        private val EXTRA_ERROR_THROWABLE = StringUtil.constant("ExtraErrorThrowable")
    }
}