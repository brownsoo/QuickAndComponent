package com.hansoolabs.and.widget

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import com.hansoolabs.and.R

class MessageProgressDialog(
    context: Context,
    private var message: String? = null
) : Dialog(context) {

    private var textView: TextView? = null
    private var progressBar: ProgressBar? = null

    fun setMessage(text: String?) {
        this.message = text
        textView?.text = text
        textView?.visibility = if (text == null) View.GONE else View.VISIBLE
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

        setMessage(message)
        progressBar?.max = maxProgress
        progressBar?.progress = progress
        progressBar?.isIndeterminate = isIndeterminate
        setCancelable(false)
    }
}