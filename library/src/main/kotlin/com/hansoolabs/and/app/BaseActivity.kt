package com.hansoolabs.and.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.annotation.CallSuper
import androidx.annotation.IdRes
import androidx.annotation.UiThread
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.core.widget.ContentLoadingProgressBar
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.hansoolabs.and.*
import com.hansoolabs.and.utils.UiUtil
import com.hansoolabs.and.error.BaseExceptionHandler
import com.hansoolabs.and.widget.MessageProgressView
import com.trello.rxlifecycle3.android.ActivityEvent
import com.trello.rxlifecycle3.components.support.RxAppCompatActivity
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.lang.ref.WeakReference

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class BaseActivity : RxAppCompatActivity(),
    Available,
    QuickDialogListener,
    AppForegroundObserver.AppForegroundListener {

    companion object {
        const val CONTENT_FRAGMENT_TAG = "content-fragment+"
        const val TAG = "BaseActivity"
    }

    /**
     * Activity is not finishing and foreground
     */
    override val isAvailable: Boolean
        get() = !isDestroyed && !isFinishing

    protected var resumed = false
    protected var appForeground = true
    protected var viewForeground = false
    protected val androidContentView: ViewGroup?
        get() = findViewById<View>(android.R.id.content) as? ViewGroup
    protected var baseFrame: FrameLayout? = null
    protected var contentMain: View? = null
    protected var errorView: View? = null
    private var progressMsgView: MessageProgressView? = null
    protected var loadingBar: ContentLoadingProgressBar? = null

    protected val compositeBag by lazy { CompositeDisposable() }

    private var finishDisposable: Disposable? = null
    private val mainHandler by lazy {  BaseHandler(this) }
    private class BaseHandler(activity: BaseActivity) : Handler() {
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
        if (baseFrame == null) baseFrame = FrameLayout(this)
        setContentView(LayoutInflater.from(this).inflate(layoutResID, baseFrame, false))
    }
    override fun setContentView(view: View) {
        setContentView(view, null)
    }
    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        Log.d(TAG, "setContentView $view params=$params")
        if (baseFrame == null) baseFrame = FrameLayout(this)
        baseFrame?.removeAllViews()
        // 1
        contentMain = view
        baseFrame?.addView(contentMain)
        // 2
        if (loadingBar == null) {
            loadingBar = ContentLoadingProgressBar(this).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, UiUtil.dp2px(5f))
                    .also { it.gravity = Gravity.TOP }
            }
        }
        baseFrame?.addView(loadingBar)
        loadingBar?.visibility = View.GONE
        // 3
        if (errorView == null) {
            errorView = LayoutInflater.from(this).inflate(R.layout.and__error_content, baseFrame, false)
                .apply { visibility = View.GONE }
        }
        baseFrame?.addView(errorView)
        if (params == null) {
            super.setContentView(baseFrame)
        } else {
            super.setContentView(baseFrame, params)
        }
    }

    /**
     * add a fragment into containerView
     */
    protected fun setContentFragment(@IdRes containerId: Int,
                                     forceNewInstance: Boolean = false,
                                     builder: (Bundle?) -> Fragment) {
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

    override fun <T : View?> findViewById(id: Int): T {
        if (contentMain != null) {
            return contentMain!!.findViewById<T>(id)
        }
        return super.findViewById<T>(id)
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
        appForeground = true
        notifyViewForegroundChanged()
    }

    override fun onAppDidBackground() {
        appForeground = false
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

    protected fun showLoadingBar() {
        loadingBar?.visibility = View.VISIBLE
    }

    protected fun hideLoadingBar() {
        loadingBar?.visibility = View.GONE
    }

    protected open fun <T> bindUntilViewDestroy(observable: Observable<T>): Observable<T> =
        observable.compose(bindUntilEvent<T>(ActivityEvent.DESTROY))

    protected open fun <T> bindUntilViewForeground(observable: Observable<T>): Observable<T> =
        observable.compose(bindUntilEvent<T>(ActivityEvent.PAUSE))

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
        if (!isFinishing) {
            if (progressMsgView == null) {
                progressMsgView = MessageProgressView(this)
                progressMsgView?.apply {
                    layoutParams = FrameLayout.LayoutParams(-1, -1)
                }
                baseFrame?.addView(progressMsgView)
            } else if (progressMsgView!!.isShowing) {
                progressMsgView?.setMessage(message)
                baseFrame?.bringChildToFront(progressMsgView!!)
                return
            }
            progressMsgView?.setMessage(message)
            progressMsgView?.visibility = View.VISIBLE
        }
    }

    @UiThread
    open fun hideProgressMsg() {
        Log.d(TAG, "hideProgressMsg")
        progressMsgView?.visibility = View.GONE
    }

    @UiThread
    open fun hideKeyboard() {
        hideKeyboard(null)
    }

    @UiThread
    open fun hideKeyboard(focusView: View?) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val target = focusView ?: currentFocus
        if (target != null) {
            imm.hideSoftInputFromWindow(target.windowToken, 0)
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

    fun deferredFinish(muteAnimation: Boolean = false) {
        if (finishDisposable == null || finishDisposable!!.isDisposed) {
            finishDisposable = lifecycle()
                .filter { it == ActivityEvent.PAUSE }
                .take(1)
                .compose(bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe { safeFinish(muteAnimation) }
        }
    }

    fun safeFinish(muteAnimation: Boolean = false) {
        if (isAvailable) {
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