package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import java.text.SimpleDateFormat

object Utils {
    fun generateSubscriptionDates(type: String, date: Long): MutableList<String> {
        val dates: MutableList<String> = mutableListOf()
        when(type) {
            Constants.WEEKDAYS -> {

            }
            Constants.WEEKENDS -> {

            }
            Constants.MONTHLY -> {

            }
        }
        return dates
    }

    fun Long.toDateNumber(): Int {
        val date = SimpleDateFormat("dd")
        return date.format(this).toInt()
    }
    fun Long.toMonthNumber(): Int {
        val date = SimpleDateFormat("MM")
        return date.format(this).toInt()
    }
    fun String.addCharAtIndex(char: Char, index: Int) =
        StringBuilder(this).apply { insert(index, char) }.toString()
}