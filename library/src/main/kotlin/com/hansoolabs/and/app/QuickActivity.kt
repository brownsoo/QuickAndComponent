package com.hansoolabs.and.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import com.hansoolabs.and.widget.MessageProgressDialog
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class QuickActivity : AppCompatActivity(),
    QuickDialogListener,
    AppForegroundObserver.AppForegroundListener {

    companion object {
        private const val TAG = "QuickActivity"
        private const val CONTENT_FRAGMENT_TAG = "content-fragment+"
        private const val WHAT_DISMISS_PROGRESS = -10
    }

    protected var resumed = false
    protected val appForeground: Boolean
        get() = !AppForegroundObserver.instance.isAppInBackground

    protected var viewForeground = false

    protected val compositeBag by lazy { CompositeDisposable() }

    private val mainHandler by lazy { QuickMainHandler(this) }

    private class QuickMainHandler(activity: QuickActivity) : Handler(Looper.getMainLooper()) {
        private val ref = WeakReference(activity)
        override fun handleMessage(msg: Message?) {
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
        if (msg?.what == WHAT_DISMISS_PROGRESS) {
            progressDialog?.dismiss()
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
    protected fun setContentFragment(@IdRes containerId: Int,
                                     builder: (Bundle?) -> Fragment,
                                     forceNewInstance: Boolean = true) {
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
        hideProgressMsg()
        hideKeyboard()
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

    override fun onDestroy() {
        compositeBag.clear()
        super.onDestroy()
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
    open fun showProgressMsg() {
        showProgressMsg(null)
    }

    @UiThread
    open fun showProgressMsg(message: String?) {
        showProgressMsg(null, message)
    }

    open fun showProgressMsg(title: String?, message: String?) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread { showProgressMsg(title, message) }
            return
        }
        val dialog = progressDialog ?: return
        if (!isFinishing) {
            getMainHandler()?.removeMessages(WHAT_DISMISS_PROGRESS)
            if (dialog.isShowing) {
                dialog.message = message
                return
            }
            dialog.message = message
            dialog.show()
        }
    }

    @UiThread
    open fun hideProgressMsg() {
        getMainHandler()?.let {
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
                overridePendingTransition(0, 0)
            }
        }
    }

    /**
     * **Hack**
     * Fixed for error: *Intent.migrateExtraStreamToClipData() on a null object reference*.
     * Occurred only when rooted user or emulator
     *
     *
     * [reference](http://stackoverflow.com/questions/38041230/intent-migrateextrastreamtoclipdata-on-a-null-object-reference)

     * @param intent
     * *
     * @param requestCode
     */
    @CallSuper
    override fun startActivityForResult(intent: Intent?, requestCode: Int) {
        try {
            super.startActivityForResult(intent ?: Intent(), requestCode)
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