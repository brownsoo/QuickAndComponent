package com.cherrytree.skinnyyoga.view

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView

import com.hansoolabs.and.R

import java.util.Timer
import java.util.TimerTask

/**
 * Created by vulpes on 2017. 6. 12..
 */

open class IndexIndicator(private val frame: View, private val adapter: Adapter)
    : IndexScrollerView.IndexIndicator {

    private val parent: ViewGroup
    private val indexView: TextView
    private val totalView: TextView
    private val handler: Handler

    @Volatile
    private var lastTouchedTime: Long = 0
    @Volatile
    private var timer: Timer? = null

    private var animation: Animation? = null

    override val isShown: Boolean
        get() = frame.isShown

    override val isAvailable: Boolean
        get() = adapter.isVisible && adapter.totalSize > 0

    interface Adapter {
        val totalSize: Int
        val isVisible: Boolean
        fun getCurrentIndex(adapterPosition: Int, totalAdapterSize: Int): Int
    }

    init {
        this.parent = frame.parent as ViewGroup
        this.indexView = frame.findViewById<View>(R.id.index) as TextView
        this.totalView = frame.findViewById<View>(R.id.total) as TextView
        handler = Handler(Looper.getMainLooper())
    }

    override fun moveToPosition(scrollProgress: Float) {
        val height = parent.height - frame.height
        val y = scrollProgress * height
        frame.y = y
        lastTouchedTime = System.currentTimeMillis()
    }

    private fun createAnimation(from: Float, to: Float): AlphaAnimation {
        val anim = AlphaAnimation(from, to)
        anim.duration = FADE_ANIMATION_DURATION
        return anim
    }

    override fun show() {
        lastTouchedTime = System.currentTimeMillis()
        startTimer()
        if (frame.visibility != View.VISIBLE) {
            if (animation != null) {
                animation!!.cancel()
            }
            animation = createAnimation(frame.alpha, 1.0f)
            animation!!.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {

                }

                override fun onAnimationEnd(animation: Animation) {
                    frame.alpha = 1.0f
                    this@IndexIndicator.animation = null
                }

                override fun onAnimationRepeat(animation: Animation) {

                }
            })
            frame.visibility = View.VISIBLE
            frame.startAnimation(animation)
        }
    }

    override fun hide() {
        stopTimerIfExist()
        if (frame.visibility != View.GONE) {
            if (animation != null) {
                animation!!.cancel()
            }
            frame.visibility = View.VISIBLE
            animation = createAnimation(frame.alpha, 0.0f)
            animation!!.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {

                }

                override fun onAnimationEnd(animation: Animation) {
                    frame.visibility = View.GONE
                    this@IndexIndicator.animation = null
                }

                override fun onAnimationRepeat(animation: Animation) {

                }
            })
            frame.startAnimation(animation)
        }
    }

    override fun updateIndex(current: Int, total: Int) {
        val index = adapter.getCurrentIndex(current, total)
        val size = adapter.totalSize
        indexView.text = index.toString()
        totalView.text = size.toString()
    }

    @Synchronized
    private fun startTimer() {
        stopTimerIfExist()
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                if (lastTouchedTime + VISIBLE_TIMEOUT < System.currentTimeMillis()) {
                    stopTimerIfExist()
                    handler.post { hide() }
                }
            }
        }, 0, 100)
    }

    @Synchronized
    private fun stopTimerIfExist() {
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    companion object {

        private val FADE_ANIMATION_DURATION: Long = 150
        private val VISIBLE_TIMEOUT: Long = 400
    }
}
