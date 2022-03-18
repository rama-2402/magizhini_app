package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CWMFood(
    var id: String = "",
    var dishName: String = "",
    var videoID: String = "",
    var thumbnailUrl: String = "",
    var ingredients: ArrayList<Cart> = arrayListOf<Cart>(),
    var totalPrice: Float = 0f,
    var discountedPrice: Float = 0f,
    var extras: ArrayList<String> = arrayListOf()
): Parcelable
