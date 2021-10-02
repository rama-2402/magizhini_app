package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DefaultVariant(
    var productId: String = "",
    var productName: String = "",
    var variantName: String = "",
    var variantPosition: Int = 0
): Parcelable
