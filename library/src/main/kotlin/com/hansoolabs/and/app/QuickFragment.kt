@file:Suppress("unused")

package com.hansoolabs.and.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.hansoolabs.and.AppForegroundObserver
import com.hansoolabs.and.error.BaseExceptionHandler
import com.hansoolabs.and.utils.HLog
import com.hansoolabs.and.utils.UiUtil
import com.hansoolabs.and.utils.isLive
import com.hansoolabs.and.widget.MessageProgress
import com.hansoolabs.and.widget.MessageProgressDialog
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference

@Suppress("UseExpressionBody", "MemberVisibilityCanBePrivate")
open class QuickFragment : Fragment(),
    QuickDialogListener, AppForegroundObserver.AppForegroundListener,
    MessageProgress {

    companion object {
        private const val WHAT_DISMISS_PROGRESS = -11
        private const val TAG = "Quick"
    }

    protected var resumed = false

    private val mainHandler by lazy { QuickMainHandler(this) }

    private class QuickMainHandler(fragment: QuickFragment) : Handler(Looper.getMainLooper()) {
        private val ref = WeakReference(fragment)
        override fun handleMessage(msg: Message?) {
            val base = ref.get()
            base?.handleMainHandlerMessage(msg)
        }
    }

    @CallSuper
    protected open fun handleMainHandlerMessage(msg: Message?) {
        if (msg?.what == WHAT_DISMISS_PROGRESS) {
            progressDialog?.dismiss()
        }
    }

    protected fun getMainHandler(): Handler? = if (!isLive()) null else mainHandler

    protected open var progressDialog: MessageProgressDialog? = null

    private var viewForeground = false

    protected val appForeground: Boolean
        get() = !AppForegroundObserver.instance.isAppInBackground

    val viewForegrounded: Boolean
        get() = viewForeground

    val rxBag by lazy { CompositeDisposable() }

    protected val exceptionHandler: BaseExceptionHandler by lazy {
        createCommonExceptionHandler()
    }

    protected open fun createCommonExceptionHandler(): BaseExceptionHandler =
        BaseExceptionHandler(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressDialog = MessageProgressDialog(this.context!!)
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        AppForegroundObserver.instance.registerObserver(this)
    }

    @CallSuper
    override fun onStop() {
        AppForegroundObserver.instance.unregisterObserver(this)
        hideMessageProgress()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        notifyViewForegroundChanged()
    }

    override fun onPause() {
        resumed = false
        notifyViewForegroundChanged()
        super.onPause()
    }

    override fun onDestroy() {
        rxBag.clear()
        super.onDestroy()
    }

    override fun onAppDidForeground() {
        notifyViewForegroundChanged()
    }

    override fun onAppDidBackground() {
        notifyViewForegroundChanged()
    }

    private fun notifyViewForegroundChanged() {
        val prevForeground = viewForeground
        val currForeground = resumed && appForeground
        if (prevForeground != currForeground) {
            viewForeground = currForeground
            if (viewForeground) {
                onViewForeground()
            } else {
                onViewBackground()
            }
        }
    }

    protected open fun onViewForeground() {
    }

    protected open fun onViewBackground() {
    }

    @UiThread
    override fun showMessageProgress() {
        showMessageProgress(null)
    }

    @UiThread
    override fun showMessageProgress(message: String?) {
        showMessageProgress(null, message)
    }

    @UiThread
    override fun showMessageProgress(title: String?, message: String?) {
        if (!isLive()) return
        this.context ?: return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { showMessageProgress(title, message) }
            return
        }
        val dialog = progressDialog ?: return
        getMainHandler()?.removeMessages(WHAT_DISMISS_PROGRESS)
        if (dialog.isShowing) {
            dialog.setMessage(message)
            return
        }
        dialog.setMessage(message)
        dialog.show()
    }

    @UiThread
    override fun hideMessageProgress() {
        getMainHandler()?.let {
            it.removeMessages(WHAT_DISMISS_PROGRESS)
            it.sendEmptyMessageDelayed(WHAT_DISMISS_PROGRESS, 100)
        }
    }

    @UiThread
    open fun hideKeyboard() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { hideKeyboard() }
            return
        }
        hideKeyboard(null)
    }

    @UiThread
    open fun hideKeyboard(focusView: View?) {
        val target = focusView ?: activity?.currentFocus
        if (target != null) {
            UiUtil.hideKeyboard(context, target) {
                HLog.d(TAG, "QuickFragment", "hideKeyboard", it)
            }
        } else {
            UiUtil.hideKeyboard(this) {
                HLog.d(TAG, "QuickFragment", "hideKeyboard", it)
            }
        }
    }

    override fun onQuickDialogResult(tag: String, resultCode: Int, resultData: Bundle) {
        exceptionHandler.onAlertDialogResult(tag, resultCode, resultData)
    }

    @CallSuper
    override fun startActivityForResult(intent: Intent?, requestCode: Int) {
        try {
            super.startActivityForResult(intent ?: Intent(), requestCode)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            context?.let {
                AlertDialog.Builder(it)
                    .setTitle("Failed to find proper app")
                    .setMessage("Please install the app which can handle this command")
                    .setPositiveButton("Close", null)
                    .setCancelable(true)
                    .show()
            }
        }
    }


}