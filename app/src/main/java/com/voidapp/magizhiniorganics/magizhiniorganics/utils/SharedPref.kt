package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.content.Context
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.INT

class SharedPref(context: Context) {

    private val sPref = context.getSharedPreferences(Constants.USERS, Context.MODE_PRIVATE)

    fun getData(key: String, type: String, defValue: Any) : Any {
        val data = when(type) {
            Constants.STRING -> {
                sPref.getString(key, defValue.toString())
            }
            Constants.BOOLEAN -> sPref.getBoolean(key, defValue as Boolean)
            else -> ""
        }
        return data!!
    }

    fun putData(key: String, type: String, data: Any) {
        val edit = sPref.edit()
        when(type) {
            Constants.STRING -> edit.putString(key, data.toString()).apply()
            Constants.BOOLEAN -> edit.putBoolean(key, data.toString().toBoolean()).apply()
            INT -> edit.putInt(key, data.toString().toInt()).apply()
        }
    }

    fun clearAllData() {
        sPref.edit().clear().apply()
    }

}