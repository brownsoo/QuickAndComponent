package com.hansoolabs.quicksample

import android.app.Dialog
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.google.android.material.textfield.TextInputEditText
import com.hansoolabs.and.app.QuickActivity
import com.hansoolabs.and.app.QuickBottomSheetDialogFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : QuickActivity() {

    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_bottom_dialog.setOnClickListener {
            BottomBuilder(this, R.style.Theme_MaterialComponents_BottomSheetDialog)
                .setView(R.layout.bottom_input_dialog)
                .build()
                .show(supportFragmentManager, "tag-bottom-dialog")
        }

        btn_progress_show.setOnClickListener {
            count ++
            showProgressMsg("$count")

            getMainHandler()?.postDelayed({ hideProgressMsg() }, 2000)
        }
    }
}

class BottomBuilder(context: Context,
                   themeResId: Int = 0)
    : QuickBottomSheetDialogFragment.Builder<QuickBottomSheetDialogFragment>(context, themeResId) {
    override fun newInstance(): BottomDialog {
        return BottomDialog()
    }

}

class BottomDialog: QuickBottomSheetDialogFragment() {

    private var keyboardVisibility: Boolean = false
    set(value) {
        if (value != field) {
            field = value
            Log.d("quick_test", "keyboardVisibility=$keyboardVisibility")
        }
    }

    override fun setupDialogWindow(dialog: Dialog) {
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    override fun initLayout(view: View) {
        super.initLayout(view)

        val contentView = view.rootView
        Log.d("quick_test", "contentView=$contentView")
        Log.d("quick_test", "contentView.rootView=${contentView.rootView}")
        contentView.viewTreeObserver.addOnGlobalLayoutListener {
            val dialog = this.dialog ?: return@addOnGlobalLayoutListener
            val r = Rect()
            contentView.getWindowVisibleDisplayFrame(r)
            val screenHeight = dialog.window!!.decorView.height
            val keypadHeight = screenHeight - r.bottom
            Log.d("quick_test", "screenHeight=$screenHeight   /  keypadHeight=$keypadHeight")
            keyboardVisibility = keypadHeight > screenHeight * 0.15
        }

        val titleEt: TextInputEditText = view.findViewById(R.id.title_et)
        titleEt.setOnFocusChangeListener { _, hasFocus ->
            Log.d("quick_test", "hasFocus=$hasFocus")
        }
    }
}
