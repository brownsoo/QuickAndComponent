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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.hansoolabs.and.*
import com.hansoolabs.and.utils.UiUtil
import com.hansoolabs.and.error.BaseExceptionHandler
import com.hansoolabs.and.rx.RxAppCompatActivity
import com.hansoolabs.and.widget.MessageProgressView
import com.trello.rxlifecycle2.android.ActivityEvent
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.lang.ref.WeakReference

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */

@Suppress("MemberVisibilityCanBePrivate")
open class BaseActivity : RxAppCompatActivity(),
        Available,
        BaseDialogFragment.OnBaseDialogListener,
        AppForegroundObserver.AppForegroundListener {

    override val isAvailable: Boolean
        get() = !isFinishing && viewForeground

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

    protected val disposableBag by lazy { CompositeDisposable() }

    //private var progressDialog: ProgressDialog? = null
    private var finishDisposable: Disposable? = null
    private val mainHandler:BaseHandler
        get() = BaseHandler(this)

    private class BaseHandler(activity: BaseActivity): Handler() {
        private val ref = WeakReference(activity)
        override fun handleMessage(msg: Message?) {
            val base = ref.get()
            base?.handleMainHandlerMessage(msg)
        }
    }

    protected val exceptionHandler: BaseExceptionHandler by lazy {
        createCommonExceptionHandler()
    }

    protected fun getMainHandler(): Handler = mainHandler

    protected open fun handleMainHandlerMessage(msg: Message?) {}

    protected open fun createCommonExceptionHandler(): BaseExceptionHandler =
            BaseExceptionHandler(this)

    override fun setContentView(layoutResID: Int) {
        val inflater = LayoutInflater.from(this)
        // 0
        baseFrame = FrameLayout(this)
        // 1
        contentMain = inflater.inflate(layoutResID, baseFrame, false)
        baseFrame?.addView(contentMain)
        // 2
        loadingBar = ContentLoadingProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, UiUtil.dp2px(5f))
        }
        baseFrame?.addView(loadingBar)
        loadingBar?.visibility = View.GONE
        // 3
        errorView = inflater.inflate(R.layout.and__error_content, baseFrame, false)
                .apply { visibility = View.GONE }
        baseFrame?.addView(errorView)
        super.setContentView(baseFrame)
        Log.d("BaseActivity", "setContentView")
    }

    override fun setContentView(view: View) {
        val inflater = LayoutInflater.from(this)
        // 0
        baseFrame = FrameLayout(this)
        // 1
        contentMain = view
        baseFrame?.addView(contentMain)
        // 2
        loadingBar = ContentLoadingProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, UiUtil.dp2px(5f))
        }
        baseFrame?.addView(loadingBar)
        loadingBar?.visibility = View.GONE
        // 3
        errorView = inflater.inflate(R.layout.and__error_content, baseFrame, false)
                .apply { visibility = View.GONE }
        baseFrame?.addView(errorView)

        super.setContentView(baseFrame)
        Log.d("BaseActivity", "setContentView")
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        val inflater = LayoutInflater.from(this)
        baseFrame = FrameLayout(this)
        contentMain = view
        errorView = inflater.inflate(R.layout.and__error_content, baseFrame, false)
                .apply { visibility = View.GONE }
        baseFrame!!.addView(contentMain)
        baseFrame!!.addView(errorView)

        super.setContentView(baseFrame, params)
        Log.d("BaseActivity", "setContentView")
    }

    /**
     * add a fragment into containerView
     */
    protected fun setContentFragment(@IdRes containerId: Int,
                                  forceNewInstance: Boolean = false,
                                  builder: (Bundle?) -> Fragment) {
        val fragmentManager = supportFragmentManager
        var fragment = fragmentManager.findFragmentByTag(BODY)
        if (forceNewInstance || fragment == null) {
            val transaction = fragmentManager.beginTransaction()
            if (fragment != null) {
                transaction.remove(fragment)
            }
            fragment = builder.invoke(intent.extras)
            transaction
                    .add(containerId, fragment, BODY)
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
        disposableBag.clear()
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
        if (isAvailable) {
            if (progressMsgView == null) {
                progressMsgView = MessageProgressView(this)
                progressMsgView?.apply {
                    layoutParams = FrameLayout.LayoutParams(-1,-1)
                }
                baseFrame?.addView(progressMsgView)
            } else if (progressMsgView?.isShowing == true) {
                progressMsgView?.setMessage(message)
                return
            }
            progressMsgView?.setMessage(message)
            progressMsgView?.visibility = View.VISIBLE
        }
    }

    @UiThread
    open fun hideProgressMsg() {
        if (progressMsgView?.isShowing == true) {
            progressMsgView?.visibility = View.GONE
        }
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
    override fun onBaseDialogResult(tag: String, resultCode: Int, resultData: Bundle) {
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

    companion object {
        const val BODY = "body"
    }
}