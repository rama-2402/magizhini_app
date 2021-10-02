package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import java.text.SimpleDateFormat

class Time {

    fun getCustomDate(dateFormat: String = "dd/MM/yyyy", dateLong: Long): String {
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

    fun getYear(): String {
        val year = SimpleDateFormat("yyyy")
        return year.format(System.currentTimeMillis())
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
            "01" -> "january"
            "02" -> "february"
            "03" -> "march"
            "04" -> "april"
            "05" -> "may"
            "06" -> "june"
            "07" -> "july"
            "08" -> "august"
            "09" -> "september"
            "10" -> "october"
            "11" -> "november"
            "12" -> "december"
            else -> "month"
        }
    }
}