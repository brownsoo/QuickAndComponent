package com.hansoolabs.and.utils

import android.content.Context

@Suppress("unused")
object AppUtil {
    fun getAppVersion(context: Context): String {
        return try {
            val manager = context.packageManager
            val info = manager.getPackageInfo(context.packageName, 0)
            info.versionName
        } catch (e: Throwable) {
            "0.0.0"
        }
    }
}