package com.hansoolabs.and.utils

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable


fun Uri.isContentUri(): Boolean {
    return scheme == "content"
}

fun Uri.isFileUri(): Boolean {
    return scheme == "file"
}

fun<T: Parcelable> Intent.getParcelableArrayListExtraCompat(key: String, kl: Class<T>): ArrayList<T>? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("DEPRECATION")
        return this.getParcelableArrayListExtra<T>(key)
    } else {
        return this.getParcelableArrayListExtra(key, kl)
    }
}

fun<T: Parcelable> Intent.getParcelableArrayExtraCompat(key: String, kl: Class<T>): Array<T>? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        return this.getParcelableArrayExtra(key) as? Array<T>
    } else {
        return this.getParcelableArrayExtra(key, kl)
    }
}

fun<T: java.io.Serializable> Intent.getSerializableExtraCompat(key: String, kl: Class<T>): T? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        return this.getSerializableExtra(key) as T?
    }
    return this.getSerializableExtra(key, kl)
}

fun<T: Parcelable> Intent.getParcelableExtraCompat(key: String, kl: Class<T>): T? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("DEPRECATION")
        return this.getParcelableExtra(key) as T?
    }
    return this.getParcelableExtra(key, kl)
}

// Bundle


fun<T: Parcelable> Bundle.getParcelableArrayListCompat(key: String, kl: Class<T>): ArrayList<T>? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("DEPRECATION")
        return this.getParcelableArrayList<T>(key)
    } else {
        return this.getParcelableArrayList(key, kl)
    }
}

fun<T: Parcelable> Bundle.getParcelableArrayCompat(key: String, kl: Class<T>): Array<T>? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        return this.getParcelableArray(key) as? Array<T>
    } else {
        return this.getParcelableArray(key, kl)
    }
}

fun<T: java.io.Serializable> Bundle.getSerializableCompat(key: String, kl: Class<T>): T? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        return this.getSerializable(key) as T?
    }
    return this.getSerializable(key, kl)
}

fun<T: Parcelable> Bundle.getParcelableCompat(key: String, kl: Class<T>): T? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("DEPRECATION")
        return this.getParcelable(key) as T?
    }
    return this.getParcelable(key, kl)
}