package com.hansoolabs.and.app

import android.app.Activity
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.annotation.CallSuper
import android.support.annotation.IdRes
import android.support.annotation.UiThread
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.hansoolabs.and.*
import com.hansoolabs.and.utils.UiUtil
import com.hansoolabs.and.error.BaseExceptionHandler
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.lang.ref.WeakReference

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */

open class BaseActivity : RxAppCompatActivity(),
        Available,
        AlertDialogFragment.Listener,
        AppForegroundObserver.AppForegroundListener {

    override val isAvailable: Boolean
        get() = !isFinishing

    protected var resumed = false
    protected var appForeground = true
    protected var viewForeground = false
    protected val androidContentView: ViewGroup?
        get() = findViewById<View>(android.R.id.content) as? ViewGroup
    protected var baseFrame: FrameLayout? = null
    protected var contentMain: View? = null
    protected var errorView: View? = null

    protected val disposableBag by lazy { CompositeDisposable() }

    private var progressDialog: ProgressDialog? = null
    private var finishDisposable: Disposable? = null
    private val mainHandler = BaseHandler(this)

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
        baseFrame = FrameLayout(this)
        contentMain = inflater.inflate(layoutResID, baseFrame, false)
        errorView = inflater.inflate(R.layout.vt__error_content, baseFrame, false)
                .apply { visibility = View.GONE }
        baseFrame!!.addView(contentMain)
        baseFrame!!.addView(errorView)
        super.setContentView(baseFrame)
    }

    override fun setContentView(view: View) {
        val inflater = LayoutInflater.from(this)
        baseFrame = FrameLayout(this)
        contentMain = view
        errorView = inflater.inflate(R.layout.vt__error_content, baseFrame, false)
                .apply { visibility = View.GONE }
        baseFrame!!.addView(contentMain)
        baseFrame!!.addView(errorView)

        super.setContentView(baseFrame)
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        val inflater = LayoutInflater.from(this)
        baseFrame = FrameLayout(this)
        contentMain = view
        errorView = inflater.inflate(R.layout.vt__error_content, baseFrame, false)
                .apply { visibility = View.GONE }
        baseFrame!!.addView(contentMain)
        baseFrame!!.addView(errorView)
        super.setContentView(baseFrame, params)
    }

    protected fun setContentFragment(@IdRes containerId: Int,
                                  forceNewInstance: Boolean = false,
                                  builder: (Bundle?) -> Fragment) {
        val fragmentManager = supportFragmentManager
        var fragment = fragmentManager.findFragmentByTag(TAG_CONTAINER)
        if (forceNewInstance || fragment == null) {
            val transaction = fragmentManager.beginTransaction()
            if (fragment != null) {
                transaction.remove(fragment)
            }
            fragment = builder.invoke(intent.extras)
            transaction
                    .add(containerId, fragment, TAG_CONTAINER)
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
        hideProgressDialog()
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

    open protected fun onViewForeground() {
    }

    open protected fun onViewBackground() {
    }

    open protected fun <T> bindUntilViewDestroy(observable: Observable<T>): Observable<T> =
            observable.compose(bindUntilEvent<T>(ActivityEvent.DESTROY))

    open protected fun <T> bindUntilViewForeground(observable: Observable<T>): Observable<T> =
            observable.compose(bindUntilEvent<T>(ActivityEvent.PAUSE))

    @UiThread
    open fun showProgressDialog() {
        showProgressDialog(null)
    }

    @UiThread
    open fun showProgressDialog(message: String?) {
        showProgressDialog(null, message)
    }

    open fun showProgressDialog(title: String?, message: String?) {
        if (isAvailable) {
            if (progressDialog == null) {
                progressDialog = ProgressDialog(this)
            } else if (progressDialog!!.isShowing) {
                progressDialog!!.setTitle(title)
                progressDialog!!.setMessage(message)
                return
            }
            progressDialog!!.setCancelable(false)
            progressDialog!!.setTitle(title)
            progressDialog!!.setMessage(message)
            UiUtil.showDialog<ProgressDialog>(progressDialog!!)
        }
    }

    @UiThread
    open fun hideProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
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
    override fun onAlertDialogResult(tag: String, resultCode: Int, resultData: Bundle) {
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
                    .subscribe({ safeFinish(muteAnimation) })
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
        val TAG_CONTAINER = "body"
    }
}