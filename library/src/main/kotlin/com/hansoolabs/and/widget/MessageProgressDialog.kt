package com.hansoolabs.and.widget

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import com.hansoolabs.and.R

class MessageProgressDialog(
        context: Context,
        private var msg: String? = null
) : Dialog(context) {

    private var textView: TextView? = null
    private var progressBar: ProgressBar? = null

    var message: String? = null
        set(value) {
            field = value
            textView?.text = value
        }

    var progress: Int = 0
        set(value) {
            field = value
            progressBar?.progress = progress
        }

    var maxProgress: Int = 100
        set(value) {
            field = value
            progressBar?.max = value
        }

    var isIndeterminate: Boolean = true
        set(value) {
            field = value
            progressBar?.isIndeterminate = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.quick__message_dialog)
        textView = findViewById(R.id.message)
        progressBar = findViewById(R.id.message_progressbar)

        textView?.text = msg
        progressBar?.max = maxProgress
        progressBar?.progress = progress
        progressBar?.isIndeterminate = isIndeterminate
        setCancelable(false)
    }
}