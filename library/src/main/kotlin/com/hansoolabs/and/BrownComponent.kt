package com.hansoolabs.and

/**
 * Created by brownsoo on 2017. 8. 14..
 */

object BrownComponent {

    private val foregroundObservable: AppForegroundObservable = AppForegroundObservable()

    fun registerAppForegroundListener(listener: AppForegroundListener) {
        foregroundObservable.registerObserver(listener)
    }

    fun unregisterAppForegroundListener(listener: AppForegroundListener) {
        foregroundObservable.unregisterObserver(listener)
    }

    fun clearAllAppForegroundListeners() {
        foregroundObservable.unregisterAll()
    }
}