@file:Suppress("unused")

package com.hansoolabs.and.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.hansoolabs.and.AppForegroundObserver
import com.hansoolabs.and.error.BaseExceptionHandler
import com.hansoolabs.and.utils.HLog
import com.hansoolabs.and.utils.UiUtil
import com.hansoolabs.and.utils.isLive
import com.hansoolabs.and.widget.MessageProgress
import com.hansoolabs.and.widget.MessageProgressDialog
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.lang.ref.WeakReference

@Suppress("UseExpressionBody", "MemberVisibilityCanBePrivate")
open class QuickFragment : Fragment(),
    QuickDialogListener, AppForegroundObserver.AppForegroundListener,
    MessageProgress {

    companion object {
        private const val WHAT_SHOW_PROGRESS = -12
        private const val WHAT_DISMISS_PROGRESS = -11
        private const val EXTRA_PROGRESS_TITLE = "q-p-title"
        private const val EXTRA_PROGRESS_MESSAGE = "q-p-message"
        private const val TAG = "Quick"
    }

    protected var resumed = false

    private val mainHandler by lazy { QuickMainHandler(this) }

    private class QuickMainHandler(fragment: QuickFragment) : Handler(Looper.getMainLooper()) {
        private val ref = WeakReference(fragment)
        override fun handleMessage(msg: Message) {
            ref.get()?.handleMainHandlerMessage(msg)
        }
    }

    @CallSuper
    protected open fun handleMainHandlerMessage(msg: Message?) {
        when(msg?.what) {
            WHAT_SHOW_PROGRESS -> {
                msg.data?.let {
                    doShowMessageProgress(
                        it.getString(EXTRA_PROGRESS_TITLE),
                        it.getString(EXTRA_PROGRESS_MESSAGE)
                    )
                }
            }
            WHAT_DISMISS_PROGRESS -> {
                progressDialog?.dismiss()
            }
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
        progressDialog = MessageProgressDialog(this.requireContext())
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

    protected fun getFragmentManagerOrNull(): FragmentManager? {
        return try {
            parentFragmentManager
        } catch (e: IllegalStateException) {
            HLog.e(TAG, "QuickFragment", e)
            null
        }
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
        getMainHandler()?.let {
            it.removeMessages(WHAT_DISMISS_PROGRESS)
            it.removeMessages(WHAT_SHOW_PROGRESS)
            val msg = Message()
            msg.what = WHAT_SHOW_PROGRESS
            msg.data = Bundle().apply {
                putString(EXTRA_PROGRESS_TITLE, title)
                putString(EXTRA_PROGRESS_MESSAGE, message)
            }
            it.sendMessageDelayed(msg, 200)
        }
    }

    private fun doShowMessageProgress(title: String?, message: String?) {
        val dialog = progressDialog ?: return
        dialog.setTitle(title)
        dialog.setMessage(message)
        if (dialog.isShowing) {
            return
        }
        dialog.show()
    }

    @UiThread
    override fun hideMessageProgress() {
        getMainHandler()?.let {
            it.removeMessages(WHAT_SHOW_PROGRESS)
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
    
}