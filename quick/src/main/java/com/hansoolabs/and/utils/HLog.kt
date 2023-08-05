package com.hansoolabs.and.utils

import android.util.Log

object HLog {

    private val logLevel = LogLevel.VERBOSE

    private enum class LogLevel {
        NONE,
        ERROR,
        WARNING,
        INFO,
        DEBUG,
        VERBOSE
    }

    @JvmStatic
    fun e(tag: String, className: String, e: Throwable?) {
        e(tag, className, Log.getStackTraceString(e))
    }

    @JvmStatic
    fun e(tag: String, className: String, msg: String? = null) {
        if (LogLevel.ERROR.ordinal <= logLevel.ordinal) {
            val thr = Thread.currentThread().name
            val text = "[$thr] $className ${msg ?: ""}"
            Log.e(tag, text)
        }
    }

    @JvmStatic
    fun w(tag: String, className: String, vararg args: Any?) {
        if (LogLevel.WARNING.ordinal <= logLevel.ordinal) {
            val thr = Thread.currentThread().name
            val msg = args.joinToString()
            val text = "[$thr] $className $msg"
            Log.w(tag, text)
        }
    }

    @JvmStatic
    fun i(tag: String, className: String, msg: Any? = null) {
        val thr = Thread.currentThread().name
        val text = "[$thr] $className $msg"
        Log.i(tag, text)
    }

    @JvmStatic
    fun i(tag: String, className: String, vararg args: Any?) {
        val thr = Thread.currentThread().name
        val msg = args.joinToString()
        val text = "[$thr] $className $msg"
        Log.i(tag, text)
    }

    @JvmStatic
    fun d(tag: String, className: String, msg: Any? = null) {
        if (LogLevel.DEBUG.ordinal <= logLevel.ordinal) {
            val the = Thread.currentThread().name
            val text = "[$the] $className ${msg?.toString() ?: ""}"
            Log.d(tag, text)
        }
    }

    @JvmStatic
    fun d(tag: String, className: String, vararg args: Any?) {
        if (LogLevel.DEBUG.ordinal <= logLevel.ordinal) {
            val the = Thread.currentThread().name
            val msg = args.joinToString()
            val text = "[$the] $className $msg"
            Log.d(tag, text)
        }
    }

    @JvmStatic
    fun v(tag: String, className: String, msg: Any? = null) {
        if (LogLevel.VERBOSE.ordinal <= logLevel.ordinal) {
            val thr = Thread.currentThread().name
            val text = "[$thr] $className ${msg?.toString() ?: ""}"
            Log.v(tag, text)
        }
    }

    @JvmStatic
    fun v(tag: String, className: String, vararg args: Any?) {
        if (LogLevel.VERBOSE.ordinal <= logLevel.ordinal) {
            val thr = Thread.currentThread().name
            val msg = args.joinToString()
            val text = "[$thr] $className $msg"
            Log.v(tag, text)
        }
    }
}



fun Any.hlogee(TAG: String, className: String, b: () -> Throwable?) {
    HLog.e(TAG, className, b.invoke())
}

fun Any.hloges(TAG: String, className: String, b: ()-> String?) {
    HLog.e(TAG, className, b.invoke())
}
fun Any.hlogi(TAG: String, className: String, b: ()-> String?) {
    HLog.i(TAG, className, b.invoke())
}

fun Any.hlogd(TAG: String, className: String, b: ()-> String?) {
    HLog.d(TAG, className, b.invoke())
}

fun Any.hlogv(TAG: String, className: String, b: ()-> String?) {
    HLog.v(TAG, className, b.invoke())
}

fun Any.hlogw(TAG: String, className: String, b: ()-> String?) {
    HLog.w(TAG, className, b.invoke())
}