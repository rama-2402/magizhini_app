package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuickOrder(
    var customerID: String = "",
    var customerName: String = "",
    var phoneNumber: String = "",
    var orderID: String = "",
    var mailID: String = "",
    var timeStamp: Long = 0,
    var imageUrl: ArrayList<String> = arrayListOf(),
    var cart: ArrayList<Cart> = arrayListOf(),
    var note: String = "",
    var orderPlaced: Boolean = false,
    var extras: ArrayList<String> = arrayListOf()

): Parcelable
