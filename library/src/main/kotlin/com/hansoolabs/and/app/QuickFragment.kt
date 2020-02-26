package com.hansoolabs.and.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.annotation.CallSuper
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.hansoolabs.and.AppForegroundObserver
import com.hansoolabs.and.R
import com.hansoolabs.and.error.BaseExceptionHandler
import com.hansoolabs.and.utils.HLog
import com.hansoolabs.and.utils.UiUtil
import com.hansoolabs.and.utils.isLive
import com.hansoolabs.and.widget.MessageProgressView
import io.reactivex.disposables.CompositeDisposable

/**
 *
 * Created by brownsoo on 2017. 5. 12..
 */


@Suppress("UseExpressionBody", "MemberVisibilityCanBePrivate")
open class QuickFragment : Fragment(),
    QuickDialogListener, AppForegroundObserver.AppForegroundListener {

    protected var resumed = false

    protected val mainHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    private var progressMsgView: MessageProgressView? = null
    private var errorView: View? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        return setupContentView(view, container)
    }

    protected open fun setupContentView(view: View?, container: ViewGroup?): View? {
        HLog.d("quick", "onCreatingView")
        // 프레임 구조를 크게 4단 레이어로 구성
        // baseFrame -> baseFrame -> loadingBar -> errorView
        val context = this.context ?: return null
        val baseFrame: FrameLayout
        if (view == null) return null
        if (view !is FrameLayout || view is ScrollView) {
            baseFrame = FrameLayout(context)
            baseFrame.layoutParams = ViewGroup.LayoutParams(-1, -1)
            view.layoutParams = FrameLayout.LayoutParams(-1, -1)
            baseFrame.addView(view)
        } else {
            baseFrame = view
        }

        val errorFrame = errorView ?: layoutInflater.inflate(R.layout.and__error_content, baseFrame, false)
        if (!errorFrame.isInLayout) {
            errorFrame.layoutParams = ViewGroup.LayoutParams(-1, -1)
            errorFrame.visibility = View.GONE
            baseFrame.addView(errorFrame)
        }
        errorView = errorFrame

        return baseFrame
    }


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
    open fun showProgressMsg() {
        showProgressMsg(null)
    }

    @UiThread
    open fun showProgressMsg(message: String?) {
        showProgressMsg(null, message)
    }

    open fun showProgressMsg(title: String?, message: String?) {

        val root = view as? FrameLayout
        if (root == null) {
            HLog.e("quick", "root view need to be FRAME LAYOUT to show message view")
            return
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { showProgressMsg(title, message) }
            return
        }

        if (isLive() && context != null) {
            var msgView = progressMsgView
            if (msgView == null) {
                msgView = MessageProgressView(context!!)
                msgView.layoutParams = FrameLayout.LayoutParams(-1, -1)
                progressMsgView = msgView
                root.addView(msgView)
            } else {
                msgView.setMessage(message)
                msgView.visibility = View.VISIBLE
            }
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
        val target = focusView ?: activity?.currentFocus
        if (target != null) {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(target.windowToken, 0)
        } else {
            UiUtil.hideKeyboard(this)
        }
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

    override fun onQuickDialogResult(tag: String, resultCode: Int, resultData: Bundle) {
        exceptionHandler.onAlertDialogResult(tag, resultCode, resultData)
    }

}