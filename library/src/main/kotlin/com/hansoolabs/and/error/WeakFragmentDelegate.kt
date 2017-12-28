package com.hansoolabs.and.error

import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.cherrytree.skinnyyoga.util.isAvailable
import java.lang.ref.WeakReference

/**
 * Created by brownsoo on 2017. 8. 3..
 */

class WeakFragmentDelegate(fragment: Fragment) : ContextDelegate {
    private val ref: WeakReference<Fragment> = WeakReference(fragment)

    override val context: Context?
        get() = ref.get()?.context

    override val fragmentManager: FragmentManager?
        get() = ref.get()?.childFragmentManager

    override fun startActivity(intent: Intent) {
        ref.get()?.startActivity(intent)
    }

    override fun finishActivity() {
        val activity = ref.get()?.activity
        if (activity?.isAvailable() == true) {
            activity.finish()
        }
    }

}
