package com.hansoolabs.and

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
import android.support.annotation.StyleRes
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.hansoolabs.and.utils.HLog
import com.hansoolabs.and.utils.StringUtil
import kotlinx.android.synthetic.main.and__alert_dialog.*

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */


@Suppress("MemberVisibilityCanBePrivate")
open class BaseDialogFragment : DialogFragment() {

    interface OnBaseDialogListener {
        fun onBaseDialogResult(tag: String, resultCode: Int, resultData: Bundle)
    }

    companion object {

        val EXTRA_CANCELABLE = StringUtil.constant("EXTRA_CANCELABLE")
        val EXTRA_TITLE = StringUtil.constant("EXTRA_TITLE")
        val EXTRA_MESSAGE = StringUtil.constant("EXTRA_MESSAGE")
        val EXTRA_POSITIVE_BUTTON = StringUtil.constant("EXTRA_POSITIVE_BUTTON")
        val EXTRA_NEGATIVE_BUTTON = StringUtil.constant("EXTRA_NEGATIVE_BUTTON")
        val EXTRA_NEUTRAL_BUTTON = StringUtil.constant("EXTRA_NEUTRAL_BUTTON")
        val EXTRA_THEME_RES_ID = StringUtil.constant("EXTRA_THEME_RES_ID")
        val EXTRA_CUSTOM_VIEW_RES_ID = StringUtil.constant("EXTRA_CUSTOM_VIEW_RES_ID")
        val EXTRA_DEFAULT_RESULT_DATA = StringUtil.constant("EXTRA_DEFAULT_RESULT_DATA")

        const val EXTRA_WHICH = "which"
        const val BUTTON_POSITIVE = -1
        const val BUTTON_NEGATIVE = -2
        const val BUTTON_ALTERNATIVE = -3
        const val RESULT_OK = -1
        const val RESULT_CANCELED = 0

        fun isPositiveClick(bundle: Bundle): Boolean =
                bundle.getInt(EXTRA_WHICH, BUTTON_NEGATIVE) == BUTTON_POSITIVE

        @Suppress("LiftReturnOrAssignment")
        @SuppressLint("ResourceType")
        protected fun resolveDialogTheme(context: Context, @StyleRes resId: Int): Int {
            if (resId >= 0x01000000) {   // start of real resource IDs.
                return resId
            } else {
                val outValue = TypedValue()
                context.theme.resolveAttribute(R.attr.dialogTheme, outValue, true)
                return outValue.resourceId
            }
        }
    }

    private var titleView: TextView? = null
    private var messageView: TextView? = null
    private var positiveBtn: Button? = null
    private var alternativeBtn: Button? = null
    private var negativeBtn: Button? = null
    private var customViewFrame: ScrollView? = null
    private var listener: OnBaseDialogListener? = null
    @Suppress("MemberVisibilityCanBePrivate")
    protected var customView: View? = null
    private val resultData = Bundle()
    private var resultCode :Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        val style = args?.getInt(EXTRA_THEME_RES_ID, R.style.AndTheme_Dialog)
        style?.let { setStyle(DialogFragment.STYLE_NO_TITLE, style) }

        val defaultResultData = args?.getBundle(EXTRA_DEFAULT_RESULT_DATA)
        if (defaultResultData != null) {
            addDefaultResultData(defaultResultData)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setResult(RESULT_CANCELED)

        if (tag != null) {
            if (parentFragment != null && (parentFragment as? OnBaseDialogListener) != null) {
                listener = parentFragment as OnBaseDialogListener
                HLog.d("quick", "base-dialog", "onAttach : parentFragment")
            }
            else if (targetFragment != null && (targetFragment as? OnBaseDialogListener) != null) {
                listener = targetFragment as OnBaseDialogListener
                HLog.d("quick", "base-dialog", "onAttach : targetFragment")
            }
            else {
                val activity = activity
                if (activity != null && activity is OnBaseDialogListener) {
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
            inflater.inflate(R.layout.and__alert_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout(view)
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            setupDialogWindow(dialog)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun setupDialogWindow(@Suppress("UNUSED_PARAMETER") dialog: Dialog) = Unit

    protected open fun initLayout(view: View) {
        titleView = alert_dialog_title
        messageView = alert_dialog_message
        positiveBtn = btn_positive
        negativeBtn = btn_negative
        alternativeBtn = btn_alternative
        customViewFrame = custom_view_frame

        var args = arguments
        if (args == null) {
            args = Bundle()
        }
        val title = args.getCharSequence(EXTRA_TITLE)
        val message = args.getCharSequence(EXTRA_MESSAGE)
        val customLayoutResId = args.getInt(EXTRA_CUSTOM_VIEW_RES_ID, -1)
        val positive = args.getCharSequence(EXTRA_POSITIVE_BUTTON)
        val negative = args.getCharSequence(EXTRA_NEGATIVE_BUTTON)
        val neutral = args.getCharSequence(EXTRA_NEUTRAL_BUTTON)

        if (TextUtils.isEmpty(title)) {
            titleView!!.visibility = View.GONE
        } else {
            titleView!!.visibility = View.VISIBLE
            titleView!!.text = title
        }
        if (TextUtils.isEmpty(message)) {
            messageView!!.visibility = View.GONE
        } else {
            messageView!!.visibility = View.VISIBLE
            messageView!!.text = message
        }
        if (customLayoutResId > -1) {
            val inflater = LayoutInflater.from(context)
            customView = inflater.inflate(customLayoutResId, customViewFrame, true)
            customViewFrame!!.visibility = View.VISIBLE
        } else {
            customViewFrame!!.visibility = View.GONE
        }

        if (TextUtils.isEmpty(positive)) {
            positiveBtn!!.visibility = View.GONE
            positiveBtn!!.setOnClickListener(null)
        } else {
            positiveBtn!!.text = positive
            positiveBtn!!.visibility = View.VISIBLE
            positiveBtn!!.setOnClickListener { onPositiveButtonClicked() }
        }

        if (TextUtils.isEmpty(negative)) {
            negativeBtn!!.visibility = View.GONE
            negativeBtn!!.setOnClickListener(null)
        } else {
            negativeBtn!!.text = negative
            negativeBtn!!.visibility = View.VISIBLE
            negativeBtn!!.setOnClickListener { onNegativeButtonClicked() }
        }

        if (TextUtils.isEmpty(neutral)) {
            alternativeBtn!!.visibility = View.GONE
            alternativeBtn!!.setOnClickListener(null)
        } else {
            alternativeBtn!!.text = neutral
            alternativeBtn!!.visibility = View.VISIBLE
            alternativeBtn!!.setOnClickListener { onAlternativeButtonClicked() }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val tag = tag
        if (listener != null && tag != null) {
            listener!!.onBaseDialogResult(tag, getResultCode(), getResultData())
        }
    }

    /**
     * Fix the bug dialog dismissed when screen rotate, although retainInstance set true
     */
    @CallSuper
    override fun onDestroyView() {
        if (dialog != null && retainInstance) {
            dialog.setDismissMessage(null)
        }
        super.onDestroyView()
    }

    override fun onCancel(dialog: DialogInterface) {
        setResult(RESULT_CANCELED)
        super.onCancel(dialog)
    }

    protected open fun onPositiveButtonClicked(extra: Bundle? = null) {
        onButtonClicked(BUTTON_POSITIVE, RESULT_OK, extra)
    }

    protected open fun onNegativeButtonClicked(extra: Bundle? = null) {
        onButtonClicked(BUTTON_NEGATIVE, RESULT_CANCELED, extra)
    }

    protected open fun onAlternativeButtonClicked(extra: Bundle? = null) {
        onButtonClicked(BUTTON_ALTERNATIVE, RESULT_OK, extra)
    }

    protected open fun onButtonClicked(which: Int, resultCode: Int, extra: Bundle?) {
        val data = Bundle()
        if (extra != null) {
            data.putAll(extra)
        }
        data.putInt(EXTRA_WHICH, which)
        setResult(resultCode, data)
        dismiss()
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

    fun getResultData() = resultData

    fun getResultCode() = resultCode


    class BasicBuilder(context: Context,
                       themeResId: Int = 0): BaseDialogFragment.Builder<BaseDialogFragment>(context, themeResId) {
        override fun newInstance() = BaseDialogFragment()
    }

    abstract class Builder<T: BaseDialogFragment> @JvmOverloads constructor(private val context: Context, themeResId: Int = 0) {

        @StyleRes
        private val themeResId: Int = resolveDialogTheme(context, themeResId)

        private var title: CharSequence? = null
        private var message: CharSequence? = null

        private var positiveButtonText: CharSequence? = null
        private var neutralButtonText: CharSequence? = null
        private var negativeButtonText: CharSequence? = null
        private var defaultResultData: Bundle? = null
        private var cancelable = true

        private var customViewResId = -1

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

        fun setPositiveButton(@StringRes textId: Int): Builder<T> {
            this.positiveButtonText = context.getText(textId)
            return this
        }

        fun setPositiveButton(text: CharSequence): Builder<T> {
            this.positiveButtonText = text
            return this
        }

        fun setNegativeButton(@StringRes textId: Int): Builder<T> {
            this.negativeButtonText = context.getText(textId)
            return this
        }

        fun setNegativeButton(text: CharSequence): Builder<T> {
            this.negativeButtonText = text
            return this
        }

        fun setAlternativeButton(@StringRes textId: Int): Builder<T> {
            this.neutralButtonText = context.getText(textId)
            return this
        }

        fun setAlternativeButton(text: CharSequence): Builder<T> {
            this.neutralButtonText = text
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

        protected open fun buildArguments(): Bundle {
            val args = Bundle()
            args.putBoolean(EXTRA_CANCELABLE, cancelable)
            args.putInt(EXTRA_THEME_RES_ID, themeResId)
            if (customViewResId > -1) {
                args.putInt(EXTRA_CUSTOM_VIEW_RES_ID, customViewResId)
            }
            args.putCharSequence(EXTRA_TITLE, title)
            args.putCharSequence(EXTRA_MESSAGE, message)
            args.putCharSequence(EXTRA_POSITIVE_BUTTON, positiveButtonText)
            args.putCharSequence(EXTRA_NEGATIVE_BUTTON, negativeButtonText)
            args.putCharSequence(EXTRA_NEUTRAL_BUTTON, neutralButtonText)
            if (defaultResultData != null) {
                args.putBundle(EXTRA_DEFAULT_RESULT_DATA, defaultResultData)
            }
            return args
        }

        abstract fun newInstance(): T

        open fun build(): T {
            val fragment = newInstance()
            fragment.arguments = buildArguments()
            return fragment
        }

        fun show(fragmentManager: FragmentManager): T =
                show(fragmentManager, StringUtil.randomAlphaNumeric(20))

        fun show(fragmentManager: FragmentManager, tag: String): T {
            val fragment = build()
            fragment.show(fragmentManager, tag)
            return fragment
        }

        fun show(transaction: FragmentTransaction): T =
                show(transaction, StringUtil.randomAlphaNumeric(20))

        fun show(transaction: FragmentTransaction, tag: String): T {
            val fragment = build()
            fragment.show(transaction, tag)
            return fragment
        }
    }

}