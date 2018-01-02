package com.hansoolabs.and.utils

import java.text.DateFormat

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtil {

    @Synchronized fun getCalendar(locale: Locale): Calendar {
        return Calendar.getInstance(locale)
    }

    @Synchronized fun getCalendar(timezone: TimeZone): Calendar {
        return Calendar.getInstance(timezone)
    }

    @Synchronized fun getCalendar(timezone: TimeZone, locale: Locale): Calendar {
        return Calendar.getInstance(timezone, locale)
    }

    @Synchronized fun getTimeFormat(timeMilli: Long, template: String): String {
        val dateFormat: SimpleDateFormat = SimpleDateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault()) as SimpleDateFormat
        dateFormat.applyPattern(template)
        return dateFormat.format(Date(timeMilli))
    }

    /**
     * yyyy-MM-dd style
     * @param date
     * *
     * @return yyyy-MM-dd
     */
    fun getTimeFormat(date: Calendar): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        return dateFormat.format(date.time)
    }

    @JvmStatic
    fun getTimeFormat(timeMilli: Long): String {
        return getTimeFormat(timeMilli, "yyyy-MM-dd kk:mm:ss")
    }

    fun getCurrentTime(template: String): String {
        return getTimeFormat(System.currentTimeMillis(), template)
    }

    val currentTime: String
        get() = getCurrentTime("yyyy-MM-dd kk:mm:ss")

    @JvmStatic
    fun getTimeDiffFormat(timeMilli: Long): String {
        val time = timeMilli / 1000
        val format = String.format("%%0%dd", 2)
        val seconds = String.format(format, time % 60)
        val minutes = String.format(format, time % 3600 / 60)
        val hours = String.format(format, time / 3600)
        val text = "$hours:$minutes:$seconds"
        return text
    }

    fun getTimeDiffFormatKor(timeMilli: Long): String {
        val time = timeMilli / 1000
        val format = String.format("%%0%dd", 2)
        val seconds = String.format(format, time % 60)
        val minutes = String.format(format, time % 3600 / 60)
        val hours = String.format(format, time / 3600)

        val sb = StringBuilder()
        if (hours != "00") sb.append(hours).append("시간 ")
        if (minutes != "00") sb.append(minutes).append("분 ")
        sb.append(seconds).append("초")

        return sb.toString()
    }

    fun daysPassed(currTime: Long, lastTime: Long): Boolean {
        val curDate = Date(currTime)
        val lastDate = Date(lastTime)
        val curCal = Calendar.getInstance()
        curCal.time = curDate
        val lastCal = Calendar.getInstance()
        lastCal.time = lastDate

        val curYEAR = curCal.get(Calendar.YEAR)
        val lastYEAR = lastCal.get(Calendar.YEAR)
        val curDAY = curCal.get(Calendar.DAY_OF_YEAR)
        val lastDAY = lastCal.get(Calendar.DAY_OF_YEAR)

        if (curYEAR != lastYEAR || curDAY > lastDAY) {
            return true
        }
        /*
        if(curCal.get(Calendar.YEAR) > lastCal.get(Calendar.YEAR) ||
        		curCal.get(Calendar.DAY_OF_YEAR) > lastCal.get(Calendar.DAY_OF_YEAR)){
        	return true;
        }
        */
        return false
    }

    fun weeksPassed(currTime: Long, lastTime: Long): Boolean {
        val curDate = Date(currTime)
        val lastDate = Date(lastTime)
        val curCal = Calendar.getInstance()
        curCal.time = curDate
        val lastCal = Calendar.getInstance()
        lastCal.time = lastDate

        val curYear = curCal.get(Calendar.YEAR)
        val lastYear = lastCal.get(Calendar.YEAR)
        val curMonth = curCal.get(Calendar.MONTH)
        val lastMonth = lastCal.get(Calendar.MONTH)
        val curWeek = curCal.get(Calendar.WEEK_OF_YEAR)
        val lastWeek = lastCal.get(Calendar.WEEK_OF_YEAR)

        if (curYear != lastYear || curMonth != lastMonth ||
                curWeek > lastWeek) {
            return true
        }
        /*
        if(curCal.get(Calendar.YEAR) > lastCal.get(Calendar.YEAR) ||
        		curCal.get(Calendar.DAY_OF_YEAR) > lastCal.get(Calendar.DAY_OF_YEAR)){
        	return true;
        }
        */
        return false
    }

    fun monthsAgo(timestamp: Long): Int {
        var diff = 0
        if (timestamp != 0L) {
            val now = Calendar.getInstance()
            val last = Calendar.getInstance()
            last.timeInMillis = timestamp

            val year0 = last.get(Calendar.YEAR)
            val year1 = now.get(Calendar.YEAR)
            val month0 = last.get(Calendar.MONTH)
            val month1 = now.get(Calendar.MONTH)

            if (year0 == year1) {
                diff = month1 - month0
            } else if (year0 < year1) {
                diff = month1 + (12 - month0)
            }
        }
        return diff
    }
}
