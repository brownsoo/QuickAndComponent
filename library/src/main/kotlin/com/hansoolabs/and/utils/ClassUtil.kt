package com.hansoolabs.and.utils

import android.os.Parcel
import android.os.Parcelable

@Suppress("unused", "UNUSED_PARAMETER")
/**
 *
 * Created by brownsoo on 2017. 5. 19..
 */

object ClassUtil {
    
    fun areObjectsEqual(obj1: Any?, obj2: Any?): Boolean {
        if (obj1 == null) {
            return obj2 == null
        }
        return obj1 == obj2
    }

    fun allNotNull(vararg objs: Any?): Boolean {
        return objs.all { it != null }
    }

    @JvmStatic
    inline fun <reified T : Parcelable> createParcelable(crossinline createFromParcel: (Parcel) -> T?) =
            object : Parcelable.Creator<T> {
                override fun createFromParcel(source: Parcel?): T? = createFromParcel(source)
                override fun newArray(size: Int): Array<out T?> = arrayOfNulls(size)
            }


}
