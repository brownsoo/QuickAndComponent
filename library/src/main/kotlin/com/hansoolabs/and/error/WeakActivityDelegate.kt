package com.hansoolabs.and.error

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.appcompat.app.AppCompatActivity
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
        ref.get()?.let { activity ->
            if (!activity.isFinishing) {
                activity.finish()
            }
        }
    }
}
