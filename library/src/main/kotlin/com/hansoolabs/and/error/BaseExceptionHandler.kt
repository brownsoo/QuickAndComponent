package com.hansoolabs.and.error

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import com.hansoolabs.and.app.QuickDialogFragment
import com.hansoolabs.and.R
import com.hansoolabs.and.utils.ClassUtil
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Composited Exception handler to handle base exceptions
 * Created by brownsoo on 2017. 8. 3..
 */

class BaseExceptionHandler : ExceptionHandler {

    private val networkHandler: NetworkExceptionHandler

    private val tokenExpireHandler: AccessTokenExpireHandler

    override var resolving = false

    constructor(activity: AppCompatActivity) : super(activity) {
        tokenExpireHandler = AccessTokenExpireHandler(activity)
        networkHandler = NetworkExceptionHandler(activity)
    }

    constructor(fragment: Fragment) : super(fragment) {
        tokenExpireHandler = AccessTokenExpireHandler(fragment)
        networkHandler = NetworkExceptionHandler(fragment)
    }

    override fun addHandler(tag: String?, handler: (throwable: Throwable, data: Bundle?) -> Boolean)
            : BaseExceptionHandler {
        return super.addHandler(tag, handler) as BaseExceptionHandler
    }

    override fun removeHandler(tag: String): BaseExceptionHandler {
        return super.removeHandler(tag) as BaseExceptionHandler
    }

    override fun removeHandler(handler: (throwable: Throwable, data: Bundle?) -> Boolean)
            : BaseExceptionHandler {
        return super.removeHandler(handler) as BaseExceptionHandler
    }


    @JvmOverloads
    fun onError(throwable: Throwable, tag: String? = null) = onError(throwable, tag, null)

    override fun onError(throwable: Throwable, tag: String?, data: Bundle?): Boolean {
        val fixedTag = tag ?: TAG_NULL
        if (tokenExpireHandler.onError(throwable, fixedTag, data) ||
                networkHandler.onError(throwable, fixedTag, data)) {
            return true
        }
        if (handlers[fixedTag]?.invoke(throwable, data) == true) {
            return true
        }
        throwable.printStackTrace()

        val context = delegate.context
        val fm = delegate.fragmentManager
        if (ClassUtil.allNotNull(context, fm)) {
            resolving = true
            context!!
            fm!!
            val bundle = Bundle().apply {
                if (data != null) {
                    putAll(data)
                }
            }
            val title: String?
            var message: String
            if (throwable is BaseException) {
                title = null
                message = throwable.error.facingMessage ?: throwable.error.message
                message += "\n\n${throwable.error.sector.code}: ${throwable.error.code}"
            } else {
                title = context.getString(R.string.error__undefined_title)
                message = context.getString(R.string.error__undefined_message)
            }
            // todo email report handler ??
            QuickDialogFragment.BasicBuilder(context)
                    .setCancelable(true)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.and__close)
                    .setDefaultResultData(bundle)
                    .show(fm)
            return true
        }
        return false
    }

    override fun onAlertDialogResult(tag: String, resultCode: Int, resultData: Bundle?): Boolean {
        return (tokenExpireHandler.onAlertDialogResult(tag, resultCode, resultData) ||
                networkHandler.onAlertDialogResult(tag, resultCode, resultData) ||
                super.onAlertDialogResult(tag, resultCode, resultData)).apply {
            resolving = false
        }
    }

    companion object {

        fun toCommonException(throwable: Throwable,
                              defaultValue: BaseException? = null): BaseException {
            when (throwable) {
                is BaseException -> return throwable
                is ConnectException -> return BaseException(
                        BaseError.Sector.Network,
                        BaseError.Code.ConnectionError,
                        "Connection exception",
                        null,
                        throwable)
                is SocketTimeoutException -> return BaseException(
                        BaseError.Sector.Network,
                        BaseError.Code.Timeout,
                        "Timeout exception",
                        null,
                        throwable)
            }
            return defaultValue
                    ?: BaseException(BaseError.Sector.Internal,
                                    BaseError.Code.UnknownError,
                                    "Runtime exception",
                                    null,
                                    throwable)
        }
    }

}