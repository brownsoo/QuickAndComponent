package com.hansoolabs.and.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.hansoolabs.and.AppForegroundObserver
import com.hansoolabs.and.error.BaseExceptionHandler
import com.hansoolabs.and.utils.HLog
import com.hansoolabs.and.utils.UiUtil
import com.hansoolabs.and.utils.isLive
import com.hansoolabs.and.widget.MessageProgress
import com.hansoolabs.and.widget.MessageProgressDialog
import java.lang.ref.WeakReference

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class QuickActivity : AppCompatActivity(),
        QuickDialogListener,
        AppForegroundObserver.AppForegroundListener,
        MessageProgress {

    companion object {
        private const val TAG = "Quick"
        private const val CONTENT_FRAGMENT_TAG = "content-fragment+"
        private const val WHAT_SHOW_PROGRESS = -12
        private const val WHAT_DISMISS_PROGRESS = -11
        private const val EXTRA_PROGRESS_TITLE = "q-p-title"
        private const val EXTRA_PROGRESS_MESSAGE = "q-p-message"
    }

    protected var resumed = false

    protected val appForeground: Boolean
        get() = !AppForegroundObserver.instance.isAppInBackground

    protected var viewForeground = false

    private val mainHandler by lazy { QuickMainHandler(this) }

    private class QuickMainHandler(activity: QuickActivity) : Handler(Looper.getMainLooper()) {
        private val ref = WeakReference(activity)
        override fun handleMessage(msg: Message) {
            val base = ref.get()
            base?.handleMainHandlerMessage(msg)
        }
    }

    protected val exceptionHandler: BaseExceptionHandler by lazy {
        createCommonExceptionHandler()
    }

    protected fun getMainHandler(): Handler? = if (isFinishing) null else mainHandler

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

    protected open fun createCommonExceptionHandler(): BaseExceptionHandler =
            BaseExceptionHandler(this)


    protected open var progressDialog: MessageProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressDialog = MessageProgressDialog(this)
    }

    /**
     * add a fragment into containerView
     */
    protected fun setContentFragment(
            @IdRes containerId: Int,
            builder: (Bundle?) -> Fragment,
            forceNewInstance: Boolean = true
    ) {
        val fragmentManager = supportFragmentManager
        var fragment = fragmentManager.findFragmentByTag(CONTENT_FRAGMENT_TAG)
        if (forceNewInstance || fragment == null) {
            val transaction = fragmentManager.beginTransaction()
            if (fragment != null) {
                transaction.remove(fragment)
            }
            fragment = builder.invoke(intent.extras)
            transaction
                    .replace(containerId, fragment, CONTENT_FRAGMENT_TAG)
                    .commit()
            fragmentManager.executePendingTransactions()
        }
    }

    override fun onStart() {
        super.onStart()
        AppForegroundObserver.instance.registerObserver(this)
    }

    @CallSuper
    override fun onStop() {
        hideMessageProgress()
        AppForegroundObserver.instance.unregisterObserver(this)
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

    override fun onAppDidForeground() {
        notifyViewForegroundChanged()
    }

    override fun onAppDidBackground() {
        notifyViewForegroundChanged()
    }

    private fun notifyViewForegroundChanged() {
        val curr = resumed && appForeground
        if (viewForeground != curr) {
            viewForeground = curr
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

    override fun showMessageProgress(title: String?, message: String?) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread { showMessageProgress(title, message) }
            return
        }
        val hasProgressFragment = supportFragmentManager.fragments.firstOrNull { it is MessageProgress } != null
        if (hasProgressFragment) {
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
        if (isLive()) {
            dialog.show()
        }
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
            getMainHandler()?.post { hideKeyboard() }
            return
        }
        hideKeyboard(null)
    }

    @UiThread
    open fun hideKeyboard(focusView: View?) {
        val target = focusView ?: this.currentFocus
        if (target != null) {
            UiUtil.hideKeyboard(this, target) {
                HLog.d(TAG, "QuickActivity", it)
            }
        } else {
            UiUtil.hideKeyboard(this)
        }
    }

    @CallSuper
    override fun onQuickDialogResult(tag: String, resultCode: Int, resultData: Bundle) {
        exceptionHandler.onAlertDialogResult(tag, resultCode, resultData)
    }

    protected fun finishApplication() {
        setResult(Activity.RESULT_CANCELED)
        ActivityCompat.finishAffinity(this)
    }

    fun safeFinish(muteAnimation: Boolean = false) {
        if (isLive()) {
            finish()
            if (muteAnimation) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
                } else {
                    @Suppress("DEPRECATION")
                    overridePendingTransition(0, 0)
                }
            }
        }
    }

    @CallSuper
    override fun startActivity(intent: Intent?) {
        try {
            super.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            AlertDialog.Builder(this)
                .setTitle("Failed to find proper app")
                .setMessage("Please install the app which can handle this command")
                .setPositiveButton("Close", null)
                .setCancelable(true)
                .show()
        }
    }
}