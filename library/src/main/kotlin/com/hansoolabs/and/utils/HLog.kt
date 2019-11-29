package com.hansoolabs.and.utils

import android.util.Log
import com.hansoolabs.and.BuildConfig

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
    fun getStackTraceString(name: String, e: Throwable?) =
        name + "\n" + Log.getStackTraceString(e)

    @JvmStatic
    fun e(TAG: String, methodName: String, e: Throwable?) {
        e(TAG, methodName, Log.getStackTraceString(e))
    }

    @JvmStatic
    fun e(TAG: String, methodName: String, msg: String? = null) {
        if (LogLevel.ERROR.ordinal <= logLevel.ordinal) {
            val thr = Thread.currentThread().name
            val text = "[$thr] $methodName ${msg ?: ""}"
            Log.e(TAG, text)
        }
    }

    @JvmStatic
    fun w(TAG: String, methodName: String, msg: String? = null) {
        if (BuildConfig.DEBUG && LogLevel.WARNING.ordinal <= logLevel.ordinal) {
            val thr = Thread.currentThread().name
            val text = "[$thr] $methodName ${msg ?: ""}"
            Log.w(TAG, text)
        }
    }

    @JvmStatic
    fun i(TAG: String, methodName: String, msg: String? = null) {
        val thr = Thread.currentThread().name
        val text = "[$thr] $methodName ${msg ?: ""}"
        Log.i(TAG, text)
    }

    @JvmStatic
    fun i(TAG: String, methodName: String, msgs: Array<Any>) {
        val thr = Thread.currentThread().name
        val msg = msgs.joinToString { it.toString() }
        val text = "[$thr] $methodName $msg"
        Log.i(TAG, text)
    }

    @JvmStatic
    fun d(TAG: String, methodName: String, msg: Any? = null) {
        if (BuildConfig.DEBUG && LogLevel.DEBUG.ordinal <= logLevel.ordinal) {
            val the = Thread.currentThread().name
            val text = "[$the] $methodName ${msg?.toString() ?: ""}"
            Log.d(TAG, text)
        }
    }

    @JvmStatic
    fun d(TAG: String, methodName: String, msgs: Array<Any>) {
        if (BuildConfig.DEBUG && LogLevel.DEBUG.ordinal <= logLevel.ordinal) {
            val the = Thread.currentThread().name
            val msg = msgs.joinToString { it.toString() }
            val text = "[$the] $methodName $msg"
            Log.d(TAG, text)
        }
    }

    @JvmStatic
    fun v(TAG: String, methodName: String, msg: Any? = null) {
        if (BuildConfig.DEBUG && LogLevel.VERBOSE.ordinal <= logLevel.ordinal) {
            val thr = Thread.currentThread().name
            val text = "[$thr] $methodName ${msg?.toString() ?: ""}"
            Log.v(TAG, text)
        }
    }
}
