package com.hansoolabs.and

import android.app.Dialog
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v4.app.DialogFragment

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */

open class BaseDialogFragment : DialogFragment() {

    private val resultData = Bundle()
    private var resultCode :Int = 0

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            setupDialogWindow(dialog)
        }
    }

    protected fun setupDialogWindow(dialog: Dialog) = Unit

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

}
