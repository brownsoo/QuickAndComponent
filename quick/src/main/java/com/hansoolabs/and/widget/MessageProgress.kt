package com.hansoolabs.and.widget

import androidx.annotation.UiThread

interface MessageProgress {
    @UiThread
    fun showMessageProgress()
    @UiThread
    fun showMessageProgress(message: String?)
    @UiThread
    fun showMessageProgress(title: String?, message: String?)
    @UiThread
    fun hideMessageProgress()
}