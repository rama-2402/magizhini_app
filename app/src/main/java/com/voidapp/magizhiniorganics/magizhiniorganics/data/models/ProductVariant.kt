package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductVariant(
    var variantName: String = "",
    var variantType: String = "",
    var variantPrice: String = "0",
    var variantDiscount: Boolean = false,
    var discountPercent: Int = 0,
    var discountType: String = "Percentage",
    var inventory: Int = 0,
    var status: String = Constants.NO_LIMIT
): Parcelable
