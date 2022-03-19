package com.voidapp.magizhiniorganics.magizhiniorganics.utils

object Utils {
    fun String.addCharAtIndex(char: Char, index: Int) =
        StringBuilder(this).apply { insert(index, char) }.toString()
}