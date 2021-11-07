package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductVariant(
    var variantName: String = "",
    var variantType: String = "",
    var variantPrice: Double = 0.0,
    var discountPrice: Double = 0.0,
    var inventory: Int = 0,
    var status: String = Constants.NO_LIMIT
): Parcelable
