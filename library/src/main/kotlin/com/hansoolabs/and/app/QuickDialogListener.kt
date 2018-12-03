package com.hansoolabs.and.app

import android.os.Bundle
import com.hansoolabs.and.utils.StringUtil

class QuickDialog {
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
        const val BUTTON_POSITIVE = -10
        const val BUTTON_NEGATIVE = -20
        const val BUTTON_ALTERNATIVE = -30
        const val RESULT_OK = -1
        const val RESULT_CANCELED = 0

        fun isPositiveClick(bundle: Bundle): Boolean =
            bundle.getInt(EXTRA_WHICH, 0) == BUTTON_POSITIVE

        fun isNegativeClick(bundle: Bundle): Boolean =
            bundle.getInt(EXTRA_WHICH, 0) == BUTTON_NEGATIVE

        fun isAlternativeClick(bundle: Bundle): Boolean =
            bundle.getInt(EXTRA_WHICH, 0) == BUTTON_ALTERNATIVE

    }
}

interface QuickDialogListener {
    fun onQuickDialogResult(tag: String, resultCode: Int, resultData: Bundle)
}