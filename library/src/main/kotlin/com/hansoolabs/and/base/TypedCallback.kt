package com.hansoolabs.and.base

/**
 *
 * Created by brownsoo on 2018. 1. 30..
 */

interface TypedCallback<in T> {
    fun call(result: T)
}