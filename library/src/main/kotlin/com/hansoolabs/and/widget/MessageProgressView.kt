package com.hansoolabs.and.widget

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v4.widget.ContentLoadingProgressBar
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.hansoolabs.and.R

/**
 *
 * Created by brownsoo on 2017-12-14.
 */

class MessageProgressView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    var isIndeterminate: Boolean
        get() = progressBar.isIndeterminate
        set(value) {
            progressBar.isIndeterminate = value
        }

    var isShowing: Boolean = this.visibility == View.VISIBLE

    private lateinit var progressBar: ContentLoadingProgressBar
    private lateinit var textView: TextView

    init {
        View.inflate(context, R.layout.and__message_progress, this)
        progressBar = findViewById(R.id.message_loading_progressbar)
        textView = findViewById(R.id.message)
    }

    fun setMessage(msg: String?) {
        textView.text = msg
    }

    fun progress(progress: Int) {
        progressBar.progress = progress
    }

    fun maxProgress(max: Int) {
        progressBar.max = max
    }

}
