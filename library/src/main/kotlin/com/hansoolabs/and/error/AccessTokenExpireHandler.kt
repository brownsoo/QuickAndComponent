package com.hansoolabs.and.error

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import com.hansoolabs.and.AlertDialogFragment
import com.hansoolabs.and.R
import com.hansoolabs.and.utils.UiUtil

/**
 * Created by brownsoo on 2017. 8. 17..
 */

class AccessTokenExpireHandler : ExceptionHandler {

    override var resolving: Boolean = false

    constructor(activity: AppCompatActivity) : super(activity)

    constructor(fragment: Fragment) : super(fragment)

    override fun addHandler(tag: String?, handler: (throwable: Throwable, data: Bundle?) -> Boolean): AccessTokenExpireHandler {
        return super.addHandler(tag, handler) as AccessTokenExpireHandler
    }

    override fun removeHandler(tag: String): AccessTokenExpireHandler {
        return super.removeHandler(tag) as AccessTokenExpireHandler
    }

    override fun removeHandler(handler: (throwable: Throwable, data: Bundle?) -> Boolean): AccessTokenExpireHandler {
        return super.removeHandler(handler) as AccessTokenExpireHandler
    }

    override fun onError(throwable: Throwable, tag: String?, data: Bundle?): Boolean {
        val e: BaseException? = BaseExceptionHandler.toCommonException(throwable) ?: return false
        val code = e!!.error.code
        if (code == BaseError.Code.SessionExpired ||
                code == BaseError.Code.ConcurrentLogin ||
                code == BaseError.Code.DeactivatedUser) {
            val key = tag ?: TAG_NULL
            val handler = handlers[key]
            if (handler != null && handler.invoke(e, data)) {
                return true
            }
            val context = delegate.context
            val fragmentManager = delegate.fragmentManager
            if (context!= null && fragmentManager != null) {
                resolving = true
                val default = Bundle()
                data?.let {
                    default.putAll(it)
                }
                // todo Change texts by code
                val title = context.getString(R.string.error__invalid_credential_dialog__title)
                val message = context.getString(R.string.error__invalid_credential_dialog__msg)
                AlertDialogFragment.Builder(context)
                        .setCancelable(false)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(R.string.confirm)
                        .setDefaultResultData(default)
                        .show(fragmentManager, TAG_DIALOG)
                return true
            }
        }
        return false
    }

    override fun onAlertDialogResult(tag: String, resultCode: Int, resultData: Bundle?): Boolean {
        if(super.onAlertDialogResult(tag, resultCode, resultData)) {
            return true
        }
        if (tag == TAG_DIALOG) {
            resolving = false
            val positive = resultData!=null && AlertDialogFragment.isPositiveClick(resultData)
            if (positive) {
                notifyLogoutRequired()
                return true
            }
        }
        return false
    }

    private fun notifyLogoutRequired() {
        delegate.context?.let {
            LocalBroadcastManager.getInstance(it).sendBroadcast(Intent(ACTION_LOGOUT_REQUIRED))
        }
    }

    companion object {
        @JvmField
        val ACTION_LOGOUT_REQUIRED = UiUtil.constant("ACTION_LOGOUT_REQUIRED")
        private val TAG_DIALOG = "AccessTokenExpireDialog"
    }
}