package com.hansoolabs.and.utils

import android.telephony.PhoneNumberFormattingTextWatcher
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.TextWatcher
import java.util.*

/**
 * Created by vulpes on 2017. 7. 6..
 */
@Deprecated("내부 PhoneNumberFormattingTextWatcher 가 deprecated 되었습니다.")
class PhoneNumberFormattingCompatTextWatcher(private val locale: Locale = Locale.getDefault()) :
    TextWatcher {

    private val compatWatchers = HashMap<String, TextWatcher>()
    private val watcher = PhoneNumberFormattingTextWatcher()

    init {
        compatWatchers.put(Locale.KOREA.country, KoreaPhoneNumberFormattingTextWatcher())
    }

    override fun afterTextChanged(s: Editable?) {
        val watcher = compatWatchers[locale.country] ?: this.watcher
        watcher.afterTextChanged(s)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        val watcher = compatWatchers[locale.country] ?: this.watcher
        watcher.beforeTextChanged(s, start, count, after)
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val watcher = compatWatchers[locale.country] ?: this.watcher
        watcher.onTextChanged(s, start, before, count)
    }

    class KoreaPhoneNumberFormattingTextWatcher : TextWatcher {

        private var editing = false

        override fun afterTextChanged(s: Editable?) {
            s ?: return
            if (editing) {
                return
            }
            editing = true
            formatPhoneNumberForKorea(s)
            editing = false
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // NO OP
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // NO OP
        }
    }
}

fun formatPhoneNumber(number: String, locale: Locale = Locale.getDefault()): String {
    when (locale.country) {
        Locale.KOREA.country ->
            return Editable.Factory.getInstance().newEditable(number)
                .also { formatPhoneNumberForKorea(it) }
                .toString()
    }
    return PhoneNumberUtils.formatNumber(number, locale.country)
}

fun formatPhoneNumberForKorea(number: Editable) {
    val builder = StringBuilder()
    builder.append(number.toString().replace(Regex("\\D"), ""))

    if (builder.length > 3) {
        builder.insert(3, "-")
    }
    if (builder.length == 11) {
        builder.insert(7, "-")
    } else if (builder.length > 8){
        builder.insert(8, "-")
    }
    if (builder.length > 13) {
        builder.delete(13, builder.length)
    }
    number.replace(0, number.length, builder.toString())
}