package com.hansoolabs.and.error

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager

/**
 * Created by brownsoo on 2017. 8. 3..
 */

interface ContextDelegate {
    val context: Context?
    val fragmentManager: FragmentManager?
    fun startActivity(intent: Intent): Unit
    fun finishActivity()
}