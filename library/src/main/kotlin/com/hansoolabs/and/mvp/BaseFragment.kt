package com.hansoolabs.and.mvp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.annotation.CallSuper
import android.support.annotation.UiThread
import android.support.v4.widget.ContentLoadingProgressBar
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.hansoolabs.and.*
import com.hansoolabs.and.utils.UiUtil
import com.hansoolabs.and.error.BaseExceptionHandler
import com.hansoolabs.and.widget.MessageProgressView
import com.trello.rxlifecycle2.components.support.RxFragment
import io.reactivex.disposables.CompositeDisposable

/**
 *
 * Created by brownsoo on 2017. 5. 12..
 */


open class BaseFragment : RxFragment(),
        Available,
        AppForegroundListener, AlertDialogFragment.Listener {

    protected var resumed = false
    protected var appForeground = true
    protected val mainHandler = Handler()
    protected lateinit var progressBar: ContentLoadingProgressBar
    private var progressMsgView: MessageProgressView? = null
    private var baseFrame: FrameLayout? = null
    private var contentMain: View? = null
    private var errorView: View? = null

    private var viewForeground = false

    val viewForegrounded: Boolean
        get() = viewForeground

    override val isAvailable: Boolean
        get() = !(activity?.isFinishing ?: true)

    private val disposableBack by lazy { CompositeDisposable() }

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
    override fun onAttach(activity: Activity?) {
        super.onAttach(activity)
        onAttachCompat(activity)
    }

    @CallSuper
    override fun onAttach(context: Context?) {
        super.onAttach(context)
        onAttachCompat(context)
    }

    @Suppress("UseExpressionBody", "MemberVisibilityCanPrivate")
    @CallSuper
    protected fun onAttachCompat(context: Context?) {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // 프레임 구조를 크게 4단 레이어로 구성
        // baseFrame -> baseFrame -> progressBar -> errorView
        context!!
        baseFrame = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT)
        }
        contentMain = createContentView(inflater, baseFrame, savedInstanceState)
        contentMain?.let {
            baseFrame?.addView(it)
        }

        progressBar = ContentLoadingProgressBar(context!!).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, UiUtil.dp2px(5f))
        }
        baseFrame?.addView(progressBar)
        progressBar.visibility = View.GONE

        errorView = inflater.inflate(R.layout.vt__error_content, baseFrame, false)
        errorView?.let { baseFrame?.addView(it) }
        return baseFrame
    }

    open fun createContentView(inflater: LayoutInflater?,
                               container: ViewGroup?,
                               savedInstanceState: Bundle?): View? = null

    @CallSuper
    override fun onStart() {
        super.onStart()
        BrownComponent.registerAppForegroundListener(this)
    }

    @CallSuper
    override fun onStop() {
        BrownComponent.unregisterAppForegroundListener(this)
        hideProgressDialog()
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

    override fun onAppBecomeForeground() {
        appForeground = true
        notifyViewForegroundChanged()
    }

    override fun onAppBecomeBackground() {
        appForeground = false
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

    open protected fun onViewForeground() {
    }

    open protected fun onViewBackground() {
    }

    protected fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
    }

    protected fun hideProgressBar() {
        progressBar.visibility = View.GONE
    }

    @UiThread
    open fun showProgressDialog() {
        showProgressDialog(null)
    }

    @UiThread
    open fun showProgressDialog(message: String?) {
        showProgressDialog(null, message)
    }

    open fun showProgressDialog(title: String?, message: String?) {
        if (isAvailable && context != null) {
            if (progressMsgView == null) {
                progressMsgView = MessageProgressView(context!!)
                progressMsgView?.apply {
                    layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT)
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
    open fun hideProgressDialog() {
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

    override fun onAlertDialogResult(tag: String, resultCode: Int, resultData: Bundle) {
        exceptionHandler.onAlertDialogResult(tag, resultCode, resultData)
    }

}