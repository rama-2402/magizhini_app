package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
class TimeUtil {

    fun getCustomDate(dateFormat: String = "dd/MM/yyyy", dateLong: Long): String {
        val simpleDateFormat = SimpleDateFormat(dateFormat)
        return simpleDateFormat.format(dateLong)
    }

    fun getTimeInHMS(dateFormat: String = "HH:mm:ss", dateLong: Long): String {
        val simpleDateFormat = SimpleDateFormat(dateFormat)
        return simpleDateFormat.format(dateLong)
    }

    fun getCurrentDate(): String {
        val timeLong =  System.currentTimeMillis()
        return getCustomDate(dateLong = timeLong)
    }

    fun timeStamp(timeLong: Long): String {
        val simpleDateFormat = SimpleDateFormat("HH:mm:ss")
        return simpleDateFormat.format(timeLong)
    }

    fun getCurrentDateNumber(): String {
        val dateFormat = SimpleDateFormat("dd")
        return dateFormat.format(System.currentTimeMillis())
    }

    fun getMonth(): String {
        val monthFormat = SimpleDateFormat("MM")
        val id = monthFormat.format(System.currentTimeMillis())
        return monthNameFromNumber(id)
    }

    fun getMonthNumber(): Int {
        val monthFormat = SimpleDateFormat("MM")
        return monthFormat.format(System.currentTimeMillis()).toInt()
    }

    fun getYear(): String {
        val year = SimpleDateFormat("yyyy")
        return year.format(System.currentTimeMillis())
    }

    fun getCustomDateFromDifference(
        startDate: Long,
        difference: Int
    ): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = startDate
        cal.add(Calendar.DATE, difference)
        return cal.timeInMillis
    }

    fun getTimeAgo(time: Long): String? {

        var time = time

        if (time < 1000000000000L) {
            time *= 1000
        }
        val now = System.currentTimeMillis()
        if (time > now || time <= 0) {
            return null
        }

        val diff = now - time

        return when {
            diff < Constants.MINUTE_MILLIS -> {
                "just now"
            }
            diff < 2 * Constants.MINUTE_MILLIS -> {
                "a minute ago"
            }
            diff < 50 * Constants.MINUTE_MILLIS -> {
                (diff / Constants.MINUTE_MILLIS).toString() + " minutes ago"
            }
            diff < 90 * Constants.MINUTE_MILLIS -> {
                "an hour ago"
            }
            diff < 24 * Constants.HOUR_MILLIS -> {
                (diff / Constants.HOUR_MILLIS).toString() + " hours ago"
            }
            diff < 48 * Constants.HOUR_MILLIS -> {
                "yesterday"
            }
            else -> {
                (diff / Constants.DAY_MILLIS).toString() + " days ago"
            }
        }
    }

    private fun monthNameFromNumber(number: String) : String {
        return when(number) {
            "01" -> "January"
            "02" -> "February"
            "03" -> "March"
            "04" -> "April"
            "05" -> "May"
            "06" -> "June"
            "07" -> "July"
            "08" -> "August"
            "09" -> "September"
            "10" -> "October"
            "11" -> "November"
            "12" -> "December"
            else -> "Month"
        }
    }
}