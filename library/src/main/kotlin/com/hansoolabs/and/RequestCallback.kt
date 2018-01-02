package com.hansoolabs.and

/**
 *
 * Created by brownsoo on 2017. 5. 10..
 */

interface RequestCallback<in T> {

    fun onSuccess(result: T?)

    fun onFailure(e: Exception)
}