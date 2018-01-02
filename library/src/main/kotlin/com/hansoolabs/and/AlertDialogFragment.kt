package com.hansoolabs.and

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
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
import com.hansoolabs.and.utils.StringUtil
import com.hansoolabs.and.utils.UiUtil
import kotlinx.android.synthetic.main.vt__alert_dialog.*

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */


open class AlertDialogFragment : BaseDialogFragment() {

    interface Listener {
        fun onAlertDialogResult(tag: String, resultCode: Int, resultData: Bundle)
    }

    companion object {

        val EXTRA_CANCELABLE = UiUtil.constant("EXTRA_CANCELABLE")
        val EXTRA_TITLE = UiUtil.constant("EXTRA_TITLE")
        val EXTRA_MESSAGE = UiUtil.constant("EXTRA_MESSAGE")
        val EXTRA_POSITIVE_BUTTON = UiUtil.constant("EXTRA_POSITIVE_BUTTON")
        val EXTRA_NEGATIVE_BUTTON = UiUtil.constant("EXTRA_NEGATIVE_BUTTON")
        val EXTRA_NEUTRAL_BUTTON = UiUtil.constant("EXTRA_NEUTRAL_BUTTON")
        val EXTRA_THEME_RES_ID = UiUtil.constant("EXTRA_THEME_RES_ID")
        protected val EXTRA_CUSTOM_VIEW_RES_ID = UiUtil.constant("EXTRA_CUSTOM_VIEW_RES_ID")
        protected val EXTRA_DEFAULT_RESULT_DATA = UiUtil.constant("EXTRA_DEFAULT_RESULT_DATA")

        const val EXTRA_WHICH = "which"
        const val BUTTON_POSITIVE = -1
        const val BUTTON_NEGATIVE = -2
        const val BUTTON_NEUTRAL = -3
        const val RESULT_OK = -1
        const val RESULT_CANCELED = 0

        fun isPositiveClick(bundle: Bundle): Boolean {
            return bundle.getInt(EXTRA_WHICH, BUTTON_NEGATIVE) == BUTTON_POSITIVE
        }

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
    private var neutralBtn: Button? = null
    private var negativeBtn: Button? = null
    private var customViewFrame: ScrollView? = null

    private var listener: Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        val style = args?.getInt(EXTRA_THEME_RES_ID, R.style.BrownTheme_Dialog)
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
            val fragment = parentFragment
            if (fragment != null && fragment is Listener) {
                listener = fragment
            } else {
                val activity = activity
                if (activity != null && activity is Listener) {
                    listener = activity
                }
            }
        }
        isCancelable = arguments?.getBoolean(EXTRA_CANCELABLE, false) ?: true
    }

    override fun onCreateView(inflater: LayoutInflater,
                     container: ViewGroup?,
                     savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.vt__alert_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout(view)
    }

    protected open fun initLayout(view: View) {
        titleView = alert_dialog_title
        messageView = alert_dialog_message
        positiveBtn = btn_positive
        negativeBtn = btn_negative
        neutralBtn = btn_neutral
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
            inflater.inflate(customLayoutResId, customViewFrame, true)
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
            positiveBtn!!.setOnClickListener { v -> onButtonClicked(BUTTON_POSITIVE) }
        }

        if (TextUtils.isEmpty(negative)) {
            negativeBtn!!.visibility = View.GONE
            negativeBtn!!.setOnClickListener(null)
        } else {
            negativeBtn!!.text = negative
            negativeBtn!!.visibility = View.VISIBLE
            negativeBtn!!.setOnClickListener { v -> onButtonClicked(BUTTON_NEGATIVE) }
        }

        if (TextUtils.isEmpty(neutral)) {
            neutralBtn!!.visibility = View.GONE
            neutralBtn!!.setOnClickListener(null)
        } else {
            neutralBtn!!.text = neutral
            neutralBtn!!.visibility = View.VISIBLE
            neutralBtn!!.setOnClickListener { v -> onButtonClicked(BUTTON_NEUTRAL) }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        val tag = tag
        if (listener != null && tag != null) {
            listener!!.onAlertDialogResult(tag, getResultCode(), getResultData())
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        setResult(RESULT_CANCELED)
        super.onCancel(dialog)
    }

    protected fun onButtonClicked(which: Int) {
        onButtonClicked(which, null)
    }

    protected fun onButtonClicked(which: Int, extra: Bundle?) {
        val data = Bundle()
        if (extra != null) {
            data.putAll(extra)
        }
        data.putInt(EXTRA_WHICH, which)
        setResult(RESULT_OK, data)
        dismiss()
    }



    class Builder @JvmOverloads constructor(private val context: Context, themeResId: Int = 0) {

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

        fun setTitle(@StringRes titleId: Int): Builder {
            this.title = context.getText(titleId)
            return this
        }

        fun setTitle(title: CharSequence?): Builder {
            this.title = title
            return this
        }

        fun setMessage(@StringRes messageId: Int): Builder {
            this.message = context.getText(messageId)
            return this
        }

        fun setMessage(message: CharSequence): Builder {
            this.message = message
            return this
        }

        fun setPositiveButton(@StringRes textId: Int): Builder {
            this.positiveButtonText = context.getText(textId)
            return this
        }

        fun setPositiveButton(text: CharSequence): Builder {
            this.positiveButtonText = text
            return this
        }

        fun setNegativeButton(@StringRes textId: Int): Builder {
            this.negativeButtonText = context.getText(textId)
            return this
        }

        fun setNegativeButton(text: CharSequence): Builder {
            this.negativeButtonText = text
            return this
        }

        fun setNeutralButton(@StringRes textId: Int): Builder {
            this.neutralButtonText = context.getText(textId)
            return this
        }

        fun setNeutralButton(text: CharSequence): Builder {
            this.neutralButtonText = text
            return this
        }

        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        fun setView(layoutResId: Int): Builder {
            this.customViewResId = layoutResId
            return this
        }

        fun setDefaultResultData(data: Bundle): Builder {
            this.defaultResultData = data
            return this
        }

        private fun buildArguments(): Bundle {
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

        fun create(): AlertDialogFragment {
            val fragment = AlertDialogFragment()
            fragment.arguments = buildArguments()
            return fragment
        }

        fun show(fragmentManager: FragmentManager): AlertDialogFragment {
            return show(fragmentManager, StringUtil.randomAlphaNumeric(20))
        }

        fun show(fragmentManager: FragmentManager, tag: String): AlertDialogFragment {
            val fragment = create()
            fragment.show(fragmentManager, tag)
            return fragment
        }

        fun show(transaction: FragmentTransaction): AlertDialogFragment {
            return show(transaction, StringUtil.randomAlphaNumeric(20))
        }

        fun show(transaction: FragmentTransaction, tag: String): AlertDialogFragment {
            val fragment = create()
            fragment.show(transaction, tag)
            return fragment
        }
    }

}