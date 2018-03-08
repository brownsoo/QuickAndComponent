package com.hansoolabs.and.base

/**
 * Optional
 *
 * refer: https://medium.com/@joshfein/handling-null-in-rxjava-2-0-10abd72afa0b
 * refer: java.util.Optional
 * Created by brownsoo on 2018. 3. 7..
 */

data class Optional<out T>(val value: T?) {
    
    companion object {
        val Empty: Optional<Any>
            get() = Optional(null)
    }
    
}