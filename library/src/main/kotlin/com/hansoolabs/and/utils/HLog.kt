package com.hansoolabs.and.utils

import android.util.Log

import com.hansoolabs.and.BuildConfig

object HLog {

    private val logLevel = LogLevel.VERBOSE

    private val isLogPrintMode: Boolean
        get() = BuildConfig.DEBUG

    private enum class LogLevel {
        NONE,
        ERROR,
        WARNING,
        INFO,
        DEBUG,
        VERBOSE
    }

    fun getStackTraceString(name: String, e: Throwable?) =
            name + "\n" + Log.getStackTraceString(e)

    fun e(TAG: String, CLASS: String, e: Throwable?) {
        e(TAG, CLASS, Log.getStackTraceString(e))
    }

    fun e(TAG: String, CLASS: String, msg: String) {
        if (isLogPrintMode && LogLevel.ERROR.ordinal <= logLevel.ordinal) {
            val thr = Thread.currentThread().name
            val text = "[$thr] $CLASS $msg"
            Log.e(TAG, text)
        }
    }

    fun w(TAG: String, CLASS: String, msg: String) {
        if (isLogPrintMode && LogLevel.WARNING.ordinal <= logLevel.ordinal) {
            val thr = Thread.currentThread().name
            val text = "[$thr] $CLASS $msg"
            Log.w(TAG, text)
        }
    }

    fun i(TAG: String, CLASS: String, msg: String) {
        if (isLogPrintMode && LogLevel.INFO.ordinal <= logLevel.ordinal) {
            val thr = Thread.currentThread().name
            val text = "[$thr] $CLASS $msg"
            Log.i(TAG, text)
        }
    }

    fun d(TAG: String, CLASS: String, msg: String) {
        if (isLogPrintMode && LogLevel.DEBUG.ordinal <= logLevel.ordinal) {
            val the = Thread.currentThread().name
            val text = "[$the] $CLASS $msg"
            Log.d(TAG, text)
        }
    }

    fun v(TAG: String, CLASS: String, msg: String) {
        if (isLogPrintMode && LogLevel.VERBOSE.ordinal <= logLevel.ordinal) {
            val thr = Thread.currentThread().name
            val text = "[$thr] $CLASS $msg"
            Log.v(TAG, text)
        }
    }
}
