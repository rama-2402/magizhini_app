package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import java.text.SimpleDateFormat

object Utils {
    fun String.addCharAtIndex(char: Char, index: Int) =
        StringBuilder(this).apply { insert(index, char) }.toString()
}