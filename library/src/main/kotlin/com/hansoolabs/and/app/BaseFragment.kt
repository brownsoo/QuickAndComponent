package com.hansoolabs.and.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.annotation.CallSuper
import androidx.annotation.UiThread
import androidx.core.widget.ContentLoadingProgressBar
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.hansoolabs.and.*
import com.hansoolabs.and.utils.UiUtil
import com.hansoolabs.and.error.BaseExceptionHandler
import com.hansoolabs.and.utils.isLive
import com.hansoolabs.and.widget.MessageProgressView
import com.trello.rxlifecycle3.components.support.RxFragment
import io.reactivex.disposables.CompositeDisposable

/**
 *
 * Created by brownsoo on 2017. 5. 12..
 */


@Suppress("UseExpressionBody", "MemberVisibilityCanBePrivate")
open class BaseFragment : RxFragment(),
        QuickDialogListener, AppForegroundObserver.AppForegroundListener {

    protected var resumed = false

    protected val mainHandler = Handler()
    protected var loadingBar: ContentLoadingProgressBar? = null
    private var progressMsgView: MessageProgressView? = null
    private var baseFrame: FrameLayout? = null
    private var contentMain: View? = null
    private var errorView: View? = null

    private var viewForeground = false
    
    protected val appForeground: Boolean
        get() = !AppForegroundObserver.instance.isAppInBackground

    val viewForegrounded: Boolean
        get() = viewForeground

    val disposableBack by lazy { CompositeDisposable() }

    protected val exceptionHandler: BaseExceptionHandler by lazy {
        createCommonExceptionHandler()
    }

    protected open fun createCommonExceptionHandler(): BaseExceptionHandler = BaseExceptionHandler(this)

    open fun onNewArgument(data: Bundle) {
        arguments?.let {
            it.clear()
            it.putAll(data)
        }
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION")
    @CallSuper
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        onAttachCompat(activity)
    }

    @CallSuper
    override fun onAttach(context: Context) {
        super.onAttach(context)
        onAttachCompat(context)
    }

    protected open fun onAttachCompat(context: Context?) {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // 프레임 구조를 크게 4단 레이어로 구성
        // baseFrame -> baseFrame -> loadingBar -> errorView
        // 0
        if (baseFrame == null) {
            baseFrame = FrameLayout(context!!).apply {
                layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }
        baseFrame?.removeAllViews()
        // 1
        contentMain = createContentView(inflater, baseFrame, savedInstanceState)
        if (contentMain != null) {
            baseFrame?.addView(contentMain)
        }
        // 2
        if (loadingBar == null) {
            loadingBar = ContentLoadingProgressBar(context!!).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, UiUtil.dp2px(5f))
            }
        }
        baseFrame?.addView(loadingBar)
        loadingBar?.visibility = View.GONE
        // 3
        if (errorView == null) {
            errorView = inflater.inflate(R.layout.and__error_content, baseFrame, false)
        }
        errorView?.let { baseFrame?.addView(it) }
        return baseFrame
    }

    open fun createContentView(inflater: LayoutInflater?,
                               container: ViewGroup?,
                               savedInstanceState: Bundle?): View? = null

    @CallSuper
    override fun onStart() {
        super.onStart()
        AppForegroundObserver.instance.registerObserver(this)
    }

    @CallSuper
    override fun onStop() {
        AppForegroundObserver.instance.unregisterObserver(this)
        hideProgressMsg()
        hideKeyboard()
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
        disposableBack.clear()
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

    protected fun showLoadingBar() {
        loadingBar?.visibility = View.VISIBLE
    }

    protected fun hideLoadingBar() {
        loadingBar?.visibility = View.GONE
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
            mainHandler.post { showProgressMsg(title, message) }
            return
        }
        if (isLive() && context != null) {
            if (progressMsgView == null) {
                progressMsgView = MessageProgressView(context!!)
                progressMsgView?.apply {
                    layoutParams = FrameLayout.LayoutParams(-1, -1)
                }
                baseFrame?.addView(progressMsgView)
            }
            progressMsgView?.setMessage(message)
            progressMsgView?.visibility = View.VISIBLE

        }
    }

    @UiThread
    open fun hideProgressMsg() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { hideProgressMsg() }
            return
        }
        progressMsgView?.visibility = View.GONE
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
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val target = focusView ?: activity?.currentFocus
        if (target != null) {
            imm.hideSoftInputFromWindow(target.windowToken, 0)
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

    override fun onQuickDialogResult(tag: String, resultCode: Int, resultData: Bundle) {
        exceptionHandler.onAlertDialogResult(tag, resultCode, resultData)
    }

}