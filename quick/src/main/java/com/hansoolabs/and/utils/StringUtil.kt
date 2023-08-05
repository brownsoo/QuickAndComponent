package com.hansoolabs.and.utils

import java.util.regex.Pattern

/**
 * Created by brownsoo on 2017. 8. 14..
 */

object StringUtil {

    @JvmStatic
    fun constant(name: String) = "hansoo.labs." + name

    @JvmStatic
    fun isNullOrEmpty(str: String?): Boolean {
        if (str == null || str.isEmpty())
            return true
        return false
    }

    private const val ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun randomAlphaNumeric(count: Int): String {
        var c = count
        val builder = StringBuilder()
        while (c-- != 0) {
            val character = (Math.random() * ALPHA_NUMERIC_STRING.length).toInt()
            builder.append(ALPHA_NUMERIC_STRING[character])
        }
        return builder.toString()
    }

    @JvmStatic
    fun validateEmail(text: String?): Boolean {
        if (text == null || text.isEmpty())
            return false
        val pattern = Pattern.compile("^(([^<>()\\[\\]\\\\.,;:\\s@\"]" +
                "+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))" +
                "@" +
                "((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])" +
                "|(([a-zA-Z-0-9]+\\.)+[a-zA-Z]{2,}))$")
        return pattern.matcher(text).matches()
    }

    @JvmStatic
    fun hasSpecialChar(text: String): Boolean {
        val pattern = Pattern.compile("[*^\\s!@#$&()\\-`.+,/\"]+")
        return pattern.matcher(text).matches()
    }
}