package com.hansoolabs.and.error

import android.content.Context
import android.content.Intent
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import com.cherrytree.skinnyyoga.util.isAvailable
import java.lang.ref.WeakReference

/**
 * Created by brownsoo on 2017. 8. 3..
 */

class WeakActivityDelegate(activity: AppCompatActivity) : ContextDelegate {
    private val ref: WeakReference<AppCompatActivity> = WeakReference(activity)

    override val context: Context?
        get() = ref.get()

    override val fragmentManager: FragmentManager?
        get() = ref.get()?.supportFragmentManager
    override fun startActivity(intent: Intent) {
        ref.get()?.let {
            if (!it.isFinishing) {
                it.startActivity(intent)
            }
        }
    }

    override fun finishActivity() {
        val activity = ref.get()
        if (activity?.isAvailable() == true) {
            activity.finish()
        }
    }
}
