package com.hansoolabs.and.utils;

import android.util.Log;

import com.hansoolabs.and.BuildConfig;

public class HLog {

	private enum LogLevel {
		NONE,
		ERROR,
		WARNING,
		INFO,
		DEBUG,
		VERBOSE
	}
	
	private static LogLevel logLevel = LogLevel.VERBOSE;
	
	public static String getStackTraceString(String name, Throwable e) {
		return name + "\n" + Log.getStackTraceString(e);
	}
    
    private static boolean isLogPrintMode() {
        return BuildConfig.DEBUG;
    }

	public static void e(String TAG, String CLASS, Throwable e) {
        e(TAG, CLASS, Log.getStackTraceString(e));
    }
	public static void e(String TAG, String CLASS, String msg) {
		if(isLogPrintMode() && (LogLevel.ERROR.ordinal() <= logLevel.ordinal())) {
			String THREAD = Thread.currentThread().getName();
			String text = "[" + THREAD + "] " + CLASS + " " + msg;
			Log.e(TAG, text);
		}
	}

	public static void w(String TAG, String CLASS, String msg) {
		if(isLogPrintMode() && (LogLevel.WARNING.ordinal() <= logLevel.ordinal())) {
			String THREAD = Thread.currentThread().getName();
			String text = "[" + THREAD + "] " + CLASS + " " + msg;
			Log.w(TAG, text);
		}
	}

	public static void i(String TAG, String CLASS, String msg) {
		if(isLogPrintMode() && (LogLevel.INFO.ordinal() <= logLevel.ordinal())) {
			String THREAD = Thread.currentThread().getName();
			String text = "[" + THREAD + "] " + CLASS + " " + msg;
			Log.i(TAG, text);
		}
	}

	public static void d(String TAG, String CLASS, String msg) {
		if(isLogPrintMode() && (LogLevel.DEBUG.ordinal() <= logLevel.ordinal())) {
			String THREAD = Thread.currentThread().getName();
			String text = "[" + THREAD + "] " + CLASS + " " + msg;
			Log.d(TAG, text);
		}
	}

	public static void v(String TAG, String CLASS, String msg) {
		if(isLogPrintMode() && (LogLevel.VERBOSE.ordinal() <= logLevel.ordinal())) {
			String THREAD = Thread.currentThread().getName();
			String text = "[" + THREAD + "] " + CLASS + " " + msg;
			Log.v(TAG, text);
		}
	}
}
