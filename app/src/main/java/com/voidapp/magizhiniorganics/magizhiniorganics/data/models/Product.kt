package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    var id: String = "",
    var name: String = "",
    var category: String = "",
    var thumbnailUrl: String = "",
    var thumbnailName: String = "",
    var rating: Int = 0,
    var description: String = "",
    var descType: String = "none",
    var variants: ArrayList<ProductVariant> = ArrayList(),
    var status: String = "Available",
    var discountAvailable: Boolean = false,
    var defaultVariant: Int = 0,
    var productType: String="",
    var activated: Boolean = true,
    var reviews: ArrayList<Review> = ArrayList()
): Parcelable