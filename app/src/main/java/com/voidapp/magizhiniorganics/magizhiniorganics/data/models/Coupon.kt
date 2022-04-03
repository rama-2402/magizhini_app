package com.voidapp.magizhiniorganics.magizhiniorganics.data.models


import android.os.Parcelable
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.parcelize.Parcelize

@Parcelize
data class Coupon(
    var id: String = "",
    val name: String = "",
    val code: String = "",
    val description: String = "",
    val status: String = "active",
    val type: String = "percent",
    val amount: Float = 0f,
    val purchaseLimit: Float = 0f,
    val maxDiscount: Float = 0f,
    val from: String = Constants.DATE_CODE,
    val expiryDate: String = Constants.DATE_CODE,
    val categories: ArrayList<String> = arrayListOf(),
    val extras: ArrayList<String> = arrayListOf(),
    var thumbnailUrl: String = ""
): Parcelable
