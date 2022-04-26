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
    var orderType: String = "image",
    var timeStamp: Long = 0,
    var imageUrl: ArrayList<String> = arrayListOf(),
    val textItemsList: ArrayList<QuickOrderTextItem > = arrayListOf(),
    val audioFileUrl: String = "",
    var cart: ArrayList<Cart> = arrayListOf(),
    var note: String = "",
    var orderPlaced: Boolean = false,
    var extras: ArrayList<String> = arrayListOf()
): Parcelable

@Parcelize
data class QuickOrderTextItem (
    var productName: String = "",
    var variantName: String = "",
    var quantity: Int = 1
): Parcelable

