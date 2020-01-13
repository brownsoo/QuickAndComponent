package com.hansoolabs.and.widget

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.ContentLoadingProgressBar
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.hansoolabs.and.R

/**
 *
 * Created by brownsoo on 2017-12-14.
 */

class MessageProgressView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    var isIndeterminate: Boolean
        get() = progressBar.isIndeterminate
        set(value) {
            progressBar.isIndeterminate = value
        }
    var isBackClickable: Boolean = false
        set(value) {
            field = value
            bg.isClickable = !isBackClickable
        }
    var isShowing: Boolean
        set(value) {
            this.visibility = if (value) View.VISIBLE else View.GONE
        }
        get() {
            return this.visibility == View.VISIBLE
        }

    private var progressBar: ContentLoadingProgressBar
    private var textView: TextView
    private var bg: View

    init {
        View.inflate(context, R.layout.and__message_progress, this)
        progressBar = findViewById(R.id.message_loading_progressbar)
        textView = findViewById(R.id.message)
        bg = findViewById(R.id.background)
        bg.isClickable = true
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
