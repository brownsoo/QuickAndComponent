package com.hansoolabs.and.app


import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.CallSuper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hansoolabs.and.utils.HLog


/**
 * {@link BottomSheetDialogFragment} 를 상속받고,
 * {@link QuickDialogFragment.OnBaseDialogListener} 를 같이 사용한다.
 */
open class QuickBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var listener: QuickDialogFragment.OnBaseDialogListener? = null
    private val resultData = Bundle()
    private var resultCode :Int = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        setResult(QuickDialogFragment.RESULT_CANCELED)

        if (tag != null) {
            if (parentFragment != null && (parentFragment as? QuickDialogFragment.OnBaseDialogListener) != null) {
                listener = parentFragment as QuickDialogFragment.OnBaseDialogListener
                HLog.d("quick", "base-dialog", "onAttach : parentFragment")
            }
            else if (targetFragment != null && (targetFragment as? QuickDialogFragment.OnBaseDialogListener) != null) {
                listener = targetFragment as QuickDialogFragment.OnBaseDialogListener
                HLog.d("quick", "base-dialog", "onAttach : targetFragment")
            }
            else {
                val activity = activity
                if (activity != null && activity is QuickDialogFragment.OnBaseDialogListener) {
                    listener = activity
                    HLog.d("quick", "base-dialog", "onAttach : activity")
                } else {
                    HLog.d("quick", "base-dialog", "onAttach : no listener")
                }
            }
        }
        isCancelable = arguments?.getBoolean(QuickDialogFragment.EXTRA_CANCELABLE, false) ?: true
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        val tag = tag
        if (listener != null && tag != null) {
            listener!!.onBaseDialogResult(tag, resultCode, resultData)
        }
    }

    override fun onCancel(dialog: DialogInterface?) {
        setResult(QuickDialogFragment.RESULT_CANCELED)
        super.onCancel(dialog)
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

    protected fun onPositiveButtonClicked(extra: Bundle? = null) {
        onButtonClicked(QuickDialogFragment.BUTTON_POSITIVE, QuickDialogFragment.RESULT_OK, extra)
    }

    protected fun onNegativeButtonClicked(extra: Bundle? = null) {
        onButtonClicked(QuickDialogFragment.BUTTON_NEGATIVE, QuickDialogFragment.RESULT_CANCELED, extra)
    }

    protected fun onAlternativeButtonClicked(extra: Bundle? = null) {
        onButtonClicked(QuickDialogFragment.BUTTON_ALTERNATIVE, QuickDialogFragment.RESULT_OK, extra)
    }

    protected open fun onButtonClicked(which: Int, resultCode: Int, extra: Bundle?) {
        val data = Bundle()
        if (extra != null) {
            data.putAll(extra)
        }
        data.putInt(QuickDialogFragment.EXTRA_WHICH, which)
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
}