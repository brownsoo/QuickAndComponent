package com.hansoolabs.and.utils

import android.os.Build
import android.os.Parcel
import java.io.Serializable

inline fun <reified T: Serializable> Parcel.readSerializableCompat(): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.readSerializable(null, T::class.java)
    } else {
        this.readSerializable() as? T
    }
}

inline fun <reified T: Serializable> Parcel.readParcelableCompat(): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.readParcelable(null, T::class.java)
    } else {
        this.readParcelable(T::class.java.classLoader)
    }
}