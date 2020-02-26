package com.hansoolabs.and.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.hansoolabs.and.AppForegroundObserver
import com.hansoolabs.and.error.BaseExceptionHandler
import com.hansoolabs.and.utils.UiUtil
import com.hansoolabs.and.utils.isLive
import com.hansoolabs.and.widget.MessageProgressView
import io.reactivex.disposables.CompositeDisposable
import java.lang.ref.WeakReference

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class QuickActivity : AppCompatActivity(),
    QuickDialogListener,
    AppForegroundObserver.AppForegroundListener {

    companion object {
        const val CONTENT_FRAGMENT_TAG = "content-fragment+"
        const val TAG = "BaseActivity"
    }

    protected var resumed = false
    protected val appForeground: Boolean
        get() = !AppForegroundObserver.instance.isAppInBackground

    protected var viewForeground = false

    protected val compositeBag by lazy { CompositeDisposable() }

    private val mainHandler by lazy {  BaseHandler(this) }

    private class BaseHandler(activity: QuickActivity) : Handler(Looper.getMainLooper()) {
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

    protected open fun handleMainHandlerMessage(msg: Message?) {}

    protected open fun createCommonExceptionHandler(): BaseExceptionHandler =
        BaseExceptionHandler(this)

    override fun setContentView(layoutResID: Int) {
        val view = LayoutInflater.from(this).inflate(layoutResID, null)
        setupContentView(view, null)
    }
    override fun setContentView(view: View) {
        setupContentView(view, null)
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        setupContentView(view, params)
    }

    protected var rootLayout: FrameLayout? = null
    protected open lateinit var progressMsgView: MessageProgressView

    private fun setupContentView(view: View?, params: ViewGroup.LayoutParams?) {
        Log.d(TAG, "setupContentView $view params=$params")
        if (view == null || view !is FrameLayout || view is ScrollView) {
            rootLayout = FrameLayout(this)
            view?.layoutParams = FrameLayout.LayoutParams(-1, -1)
            rootLayout!!.addView(view)
        } else {
            rootLayout = view
        }

        progressMsgView = MessageProgressView(this)
        progressMsgView.layoutParams = FrameLayout.LayoutParams(-1, -1)
        progressMsgView.visibility = View.GONE
        rootLayout!!.addView(progressMsgView)

        if (params == null) {
            super.setContentView(rootLayout)
        } else {
            super.setContentView(rootLayout, params)
        }
    }

    override fun <T : View> findViewById(id: Int): T? {
        return rootLayout?.findViewById(id) ?: super.findViewById(id)
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

        //val rootId = rootLayout ?: return
        //val root = findViewById<ViewGroup>(rootId)

        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread { showProgressMsg(title, message) }
            return
        }
        if (!isFinishing) {
            if (progressMsgView.isShowing) {
                progressMsgView.setMessage(message)
                rootLayout?.bringChildToFront(progressMsgView)
                return
            }
            progressMsgView.setMessage(message)
            progressMsgView.isShowing = true
        }
    }

    @UiThread
    open fun hideProgressMsg() {
        Log.d(TAG, "hideProgressMsg")
        progressMsgView.isShowing = false
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
        val target = focusView ?: this.currentFocus
        if (target != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(target.windowToken, 0)
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