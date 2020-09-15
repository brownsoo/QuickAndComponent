package com.hansoolabs.and.app

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Point
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.hansoolabs.and.R
import com.hansoolabs.and.utils.HLog
import com.hansoolabs.and.utils.StringUtil
import com.hansoolabs.and.utils.UiUtil

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */


@Suppress("MemberVisibilityCanBePrivate")
open class QuickDialogFragment : DialogFragment() {

    companion object {

        val EXTRA_CANCELABLE = QuickDialog.EXTRA_CANCELABLE
        val EXTRA_TITLE = QuickDialog.EXTRA_TITLE
        val EXTRA_MESSAGE = QuickDialog.EXTRA_MESSAGE
        val EXTRA_POSITIVE_BUTTON = QuickDialog.EXTRA_POSITIVE_BUTTON
        val EXTRA_NEGATIVE_BUTTON = QuickDialog.EXTRA_NEGATIVE_BUTTON
        val EXTRA_ALT_BUTTON = QuickDialog.EXTRA_ALT_BUTTON
        // val EXTRA_THEME_RES_ID = QuickDialog.EXTRA_THEME_RES_ID
        val EXTRA_CUSTOM_VIEW_RES_ID = QuickDialog.EXTRA_CUSTOM_VIEW_RES_ID
        val EXTRA_DEFAULT_RESULT_DATA = QuickDialog.EXTRA_DEFAULT_RESULT_DATA
        const val EXTRA_WHICH = QuickDialog.EXTRA_WHICH
        const val BUTTON_POSITIVE = QuickDialog.BUTTON_POSITIVE
        const val BUTTON_NEGATIVE = QuickDialog.BUTTON_NEGATIVE
        const val BUTTON_ALTERNATIVE = QuickDialog.BUTTON_ALTERNATIVE
        const val RESULT_OK = QuickDialog.RESULT_OK
        const val RESULT_CANCELED = QuickDialog.RESULT_CANCELED
    }
    
    private var titleView: TextView? = null
    private var messageView: TextView? = null
    private var positiveBtn: Button? = null
    private var alternativeBtn: Button? = null
    private var negativeBtn: Button? = null
    private var customViewFrame: ScrollView? = null
    private var listener: QuickDialogListener? = null
    protected var customView: View? = null
    protected val resultData = Bundle()
    private var resultCode: Int = 0
    private var positiveRunnable: Runnable? = null
    private var altRunnable: Runnable? = null
    private var negativeRunnable: Runnable? = null


    /**
     * 기본 레이아웃
     *
     * 기본 다이어로그를 사용할 때 사용하는 레이아웃 값이다.
     *
     * (customView 와는 별개로 커스텀뷰를 사용할때는 적용되지 않는다.)
     *
     * 기본값: R.layout.and__alert_dialog
     */
    @LayoutRes open val baseLayoutRes: Int = R.layout.and__alert_dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val style = arguments?.getInt(
//            EXTRA_THEME_RES_ID,
//            R.style.AndTheme_Dialog
//        )
//        style?.let { setStyle(STYLE_NO_TITLE, style) }
        
        val defaultResultData = arguments?.getBundle(EXTRA_DEFAULT_RESULT_DATA)
        if (defaultResultData != null) {
            addDefaultResultData(defaultResultData)
        }
    }
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        setResult(RESULT_CANCELED)
        
        if (tag != null) {
            if (parentFragment != null && (parentFragment as? QuickDialogListener) != null) {
                listener = parentFragment as QuickDialogListener
                HLog.d("quick", "base-dialog", "onAttach : parentFragment")
            } else if (targetFragment != null && (targetFragment as? QuickDialogListener) != null) {
                listener = targetFragment as QuickDialogListener
                HLog.d("quick", "base-dialog", "onAttach : targetFragment")
            } else {
                val activity = activity
                if (activity != null && activity is QuickDialogListener) {
                    listener = activity
                    HLog.d("quick", "base-dialog", "onAttach : activity")
                } else {
                    HLog.d("quick", "base-dialog", "onAttach : no listener")
                }
            }
        }
        isCancelable = arguments?.getBoolean(EXTRA_CANCELABLE, false) ?: true
    }
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
        inflater.inflate(baseLayoutRes, container, false)
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout(view)
    }
    
    override fun onStart() {
        super.onStart()
        this.dialog?.let {
            setupDialogWindow(it)
        }
    }
    
    protected open fun setupDialogWindow(dialog: Dialog) {
        var width = -1
        activity?.windowManager?.defaultDisplay?.let { display ->
            val size = Point()
            display.getSize(size)
            width = size.x
        }
        HLog.d("quick", "setupDialogWindow", width)
        dialog.window?.let {
            val params = it.attributes
            if (width < 0) {
                params.width = UiUtil.dp2px(280f)
            } else {
                params.width = Math.min(UiUtil.dp2px(400f), (width * 0.8).toInt())
            }
            it.attributes = params
        }
    }
    
    protected open fun initLayout(view: View) {
        titleView = view.findViewById(R.id.alert_dialog_title)
        messageView = view.findViewById(R.id.alert_dialog_message)
        positiveBtn = view.findViewById(R.id.btn_positive)
        negativeBtn = view.findViewById(R.id.btn_negative)
        alternativeBtn = view.findViewById(R.id.btn_alternative)
        customViewFrame = view.findViewById(R.id.custom_view_frame)
        
        var args = arguments
        if (args == null) {
            args = Bundle()
        }
        val title = args.getCharSequence(EXTRA_TITLE)
        val message = args.getCharSequence(EXTRA_MESSAGE)
        val customLayoutResId = args.getInt(EXTRA_CUSTOM_VIEW_RES_ID, -1)
        val positive = args.getCharSequence(EXTRA_POSITIVE_BUTTON)
        val negative = args.getCharSequence(EXTRA_NEGATIVE_BUTTON)
        val alt = args.getCharSequence(EXTRA_ALT_BUTTON)
        
        if (TextUtils.isEmpty(title)) {
            titleView?.visibility = View.GONE
        } else {
            titleView?.visibility = View.VISIBLE
            titleView?.text = title
        }
        if (TextUtils.isEmpty(message)) {
            messageView?.visibility = View.GONE
        } else {
            messageView?.visibility = View.VISIBLE
            messageView?.text = message
        }
        if (customLayoutResId > -1) {
            val inflater = LayoutInflater.from(context)
            customView = inflater.inflate(customLayoutResId, customViewFrame, true)
            customViewFrame?.visibility = View.VISIBLE
        } else {
            customViewFrame?.visibility = View.GONE
        }

        positiveBtn?.let { btn ->
            if (TextUtils.isEmpty(positive)) {
                btn.visibility = View.GONE
                btn.setOnClickListener(null)
            } else {
                btn.text = positive
                btn.visibility = View.VISIBLE
                btn.setOnClickListener { onPositiveButtonClicked() }
            }
        }

        negativeBtn?.let { btn ->
            if (TextUtils.isEmpty(negative)) {
                btn.visibility = View.GONE
                btn.setOnClickListener(null)
            } else {
                btn.text = negative
                btn.visibility = View.VISIBLE
                btn.setOnClickListener { onNegativeButtonClicked() }
            }
        }

        alternativeBtn?.let { btn ->
            if (TextUtils.isEmpty(alt)) {
                btn.visibility = View.GONE
                btn.setOnClickListener(null)
            } else {
                btn.text = alt
                btn.visibility = View.VISIBLE
                btn.setOnClickListener { onAlternativeButtonClicked() }
            }
        }
    }

    fun show(fragmentManager: FragmentManager) =
        show(fragmentManager, StringUtil.randomAlphaNumeric(20))
    
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (listener != null && tag != null) {
            listener!!.onQuickDialogResult(tag!!, resultCode, resultData)
        }
    }
    
    /**
     * Fix the bug dialog dismissed when screen rotate, although retainInstance set true
     */
    @CallSuper
    override fun onDestroyView() {
        if (dialog != null && retainInstance) {
            dialog?.setDismissMessage(null)
        }
        super.onDestroyView()
    }
    
    override fun onCancel(dialog: DialogInterface) {
        setResult(RESULT_CANCELED)
        super.onCancel(dialog)
    }
    
    protected open fun onPositiveButtonClicked(extra: Bundle? = null) {
        onButtonClicked(
            BUTTON_POSITIVE,
            RESULT_OK, extra)
    }
    
    protected open fun onNegativeButtonClicked(extra: Bundle? = null) {
        onButtonClicked(
            BUTTON_NEGATIVE,
            RESULT_CANCELED, extra)
    }
    
    protected open fun onAlternativeButtonClicked(extra: Bundle? = null) {
        onButtonClicked(
            BUTTON_ALTERNATIVE,
            RESULT_OK, extra)
    }
    
    protected open fun onButtonClicked(which: Int, resultCode: Int, extra: Bundle?) {
        val data = Bundle()
        if (extra != null) {
            data.putAll(extra)
        }
        data.putInt(EXTRA_WHICH, which)
        setResult(resultCode, data)
        dismiss()
        when (which) {
            BUTTON_POSITIVE -> positiveRunnable?.run()
            BUTTON_ALTERNATIVE -> altRunnable?.run()
            BUTTON_NEGATIVE -> negativeRunnable?.run()
        }
    }

    fun setResult(resultCode: Int) {
        setResult(resultCode, null)
    }
    
    fun setResult(resultCode: Int, resultData: Bundle?) {
        if (resultData != null) {
            this.resultData.putAll(resultData)
        }
        this.resultCode = resultCode
    }
    
    fun addDefaultResultData(defaultResult: Bundle?) {
        if (defaultResult != null) {
            resultData.putAll(defaultResult)
        }
    }
    
    class BasicBuilder(context: Context) : Builder<QuickDialogFragment>(context) {
        override fun newInstance() = QuickDialogFragment()
    }
    
    abstract class Builder<T : QuickDialogFragment> constructor(private val context: Context) {

//        @SuppressLint("ResourceType")
//        private fun resolveDialogTheme(context: Context, @StyleRes resId: Int): Int {
//            HLog.i("QuickDialogFragment","resolveDialogTheme", resId)
//            return if (resId > 0) {   // start of real resource IDs.
//                resId
//            } else {
//                val outValue = TypedValue()
//                context.theme.resolveAttribute(R.attr.dialogTheme, outValue, true)
//                outValue.resourceId
//            }
//        }

//        @StyleRes
//        private val themeResId: Int =
//            resolveDialogTheme(context, themeResId)
        
        private var title: CharSequence? = null
        private var message: CharSequence? = null
        private var positiveButtonText: CharSequence? = null
        private var altButtonText: CharSequence? = null
        private var negativeButtonText: CharSequence? = null
        private var defaultResultData: Bundle? = null
        private var cancelable = true
        private var targetFragment: Fragment? = null
        private var requestCode: Int = 0
        private var customViewResId = -1
        private var positiveRunnable: Runnable? = null
        private var altRunnable: Runnable? = null
        private var negativeRunnable: Runnable? = null
        
        fun setTitle(@StringRes titleId: Int): Builder<T> {
            this.title = context.getText(titleId)
            return this
        }
        
        fun setTitle(title: CharSequence?): Builder<T> {
            this.title = title
            return this
        }
        
        fun setMessage(@StringRes messageId: Int): Builder<T> {
            this.message = context.getText(messageId)
            return this
        }
        
        fun setMessage(message: CharSequence): Builder<T> {
            this.message = message
            return this
        }
        
        fun setPositiveButton(@StringRes textId: Int, runnable: Runnable? = null): Builder<T> {
            this.positiveButtonText = context.getText(textId)
            this.positiveRunnable = runnable
            return this
        }
        
        fun setPositiveButton(text: CharSequence, runnable: Runnable? = null): Builder<T> {
            this.positiveButtonText = text
            this.positiveRunnable = runnable
            return this
        }
        
        fun setNegativeButton(@StringRes textId: Int, runnable: Runnable? = null): Builder<T> {
            this.negativeButtonText = context.getText(textId)
            this.negativeRunnable = runnable
            return this
        }
        
        fun setNegativeButton(text: CharSequence, runnable: Runnable? = null): Builder<T> {
            this.negativeButtonText = text
            this.negativeRunnable = runnable
            return this
        }
        
        fun setAltButton(@StringRes textId: Int, runnable: Runnable? = null): Builder<T> {
            this.altButtonText = context.getText(textId)
            this.altRunnable = runnable
            return this
        }
        
        fun setAltButton(text: CharSequence, runnable: Runnable? = null): Builder<T> {
            this.altButtonText = text
            this.altRunnable = runnable
            return this
        }
        
        fun setCancelable(cancelable: Boolean): Builder<T> {
            this.cancelable = cancelable
            return this
        }
        
        fun setView(@LayoutRes layoutResId: Int): Builder<T> {
            this.customViewResId = layoutResId
            return this
        }
        
        fun setDefaultResultData(data: Bundle): Builder<T> {
            this.defaultResultData = data
            return this
        }
        
        /**
         * Optional target for this fragment.  This may be used, for example,
         * if this fragment is being started by another, and when done wants to
         * give a result back to the first.  The target set here is retained
         * across instances via {@link FragmentManager#putFragment
         * FragmentManager.putFragment()}.
         *
         * @param fragment The fragment that is the target of this one.
         * @param requestCode Optional request code, for convenience if you
         * are going to call back with {@link Fragment#onActivityResult(int, int, Intent)}.
         */
        fun setTargetFragment(fragment: Fragment?, requestCode: Int): Builder<T> {
            this.targetFragment = fragment
            this.requestCode = requestCode
            return this
        }
        
        protected open fun buildArguments(): Bundle {
            val args = Bundle()
            args.putBoolean(EXTRA_CANCELABLE, cancelable)
            // args.putInt(EXTRA_THEME_RES_ID, themeResId)
            if (customViewResId > -1) {
                args.putInt(EXTRA_CUSTOM_VIEW_RES_ID, customViewResId)
            }
            args.putCharSequence(EXTRA_TITLE, title)
            args.putCharSequence(EXTRA_MESSAGE, message)
            args.putCharSequence(EXTRA_POSITIVE_BUTTON, positiveButtonText)
            args.putCharSequence(EXTRA_NEGATIVE_BUTTON, negativeButtonText)
            args.putCharSequence(EXTRA_ALT_BUTTON, altButtonText)
            if (defaultResultData != null) {
                args.putBundle(EXTRA_DEFAULT_RESULT_DATA, defaultResultData)
            }
            return args
        }
        
        abstract fun newInstance(): T
        
        open fun build(): T {
            val dialog = newInstance()
            dialog.arguments = buildArguments()
            if (targetFragment != null) dialog.setTargetFragment(targetFragment, requestCode)
            dialog.altRunnable = this.altRunnable
            dialog.positiveRunnable = this.positiveRunnable
            dialog.negativeRunnable = this.negativeRunnable
            return dialog
        }
        
        fun show(fragmentManager: FragmentManager) =
            show(fragmentManager, StringUtil.randomAlphaNumeric(20))
        
        fun show(fragmentManager: FragmentManager, tag: String) {
            if (fragmentManager.isStateSaved) return
            val dialog = build()
            dialog.show(fragmentManager, tag)
        }
        
        fun show(transaction: FragmentTransaction) =
            show(transaction, StringUtil.randomAlphaNumeric(20))
        
        fun show(transaction: FragmentTransaction, tag: String) {
            val dialog = build()
            dialog.show(transaction, tag)
        }
    }
}