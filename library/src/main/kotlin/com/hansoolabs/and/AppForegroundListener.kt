package com.hansoolabs.and

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */

interface AppForegroundListener {

    fun onAppBecomeForeground():Unit
    fun onAppBecomeBackground():Unit
}
