package com.hansoolabs.and

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Observable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.*

import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.util.Log

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */

class AppForegroundObserver : Observable<AppForegroundObserver.AppForegroundListener>() {

    interface AppForegroundListener {
        fun onAppDidForeground()
        fun onAppDidBackground()
    }

    private object Holder { val INSTANCE = AppForegroundObserver() }

    companion object {
        private const val MAX_ACTIVITY_TRANSITION_TIME_MS : Long = 2000L
        private const val TAG = "AppForegroundObserver"

        val instance: AppForegroundObserver by lazy { Holder.INSTANCE }
    }

    private val handler : Handler = Handler(Looper.getMainLooper())
    var isAppInBackground:Boolean = false
        private set
    private var activityTransitionTimer : Timer? = null
    private var activityTransitionTimerTask : TimerTask? = null
    private var bound = false

    @Synchronized fun bind(app: Application) {
        if (!bound) {
            app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
            val screenOffFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            app.registerReceiver(receiver, screenOffFilter)
            bound = true
        }
    }

    @Synchronized fun unbind(app: Application) {
        if (bound) {
            app.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
            app.unregisterReceiver(receiver)
            bound = false
        }
    }

    @Synchronized fun onTrimMemory(level: Int) {
        if (bound && level == TRIM_MEMORY_UI_HIDDEN) {
            onAppBecomeBackground()
        }
    }

    private fun startActivityTransitionTimer() {
        activityTransitionTimer = Timer()
        activityTransitionTimerTask = object : TimerTask() {
            override fun run() {
                handler.post { this@AppForegroundObserver.onAppBecomeBackground() }
            }
        }

        activityTransitionTimer?.schedule(activityTransitionTimerTask,
                MAX_ACTIVITY_TRANSITION_TIME_MS)
    }

    private fun stopActivityTransitionTimer() {
        this.activityTransitionTimerTask?.cancel()
        this.activityTransitionTimer?.cancel()
        onAppBecomeForeground()
    }

    @Synchronized private fun onAppBecomeBackground() {
        if (!isAppInBackground) {
            isAppInBackground = true
            Log.d(TAG, "app is background")
            handler.post {
                synchronized(mObservers) {
                    for (i in mObservers.indices.reversed()) {
                        mObservers[i].onAppDidBackground()
                    }
                }
            }
        }
    }

    @Synchronized private fun onAppBecomeForeground() {
        if (isAppInBackground) {
            isAppInBackground = false
            Log.d(TAG, "app is foreground")

            handler.post {
                synchronized(mObservers) {
                    for (i in mObservers.indices.reversed()) {
                        mObservers[i].onAppDidForeground()
                    }
                }
            }
        }
    }


    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        }

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
            stopActivityTransitionTimer()
        }

        override fun onActivityPaused(activity: Activity) {
            startActivityTransitionTimer()
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle?) {
        }

        override fun onActivityDestroyed(activity: Activity) {
        }
    }

    private val receiver = object : BroadcastReceiver() {
        @Synchronized override fun onReceive(context: Context, intent: Intent) {
            onAppBecomeBackground()
        }
    }

}
