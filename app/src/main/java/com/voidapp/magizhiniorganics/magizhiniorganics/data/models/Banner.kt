package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Banner(
    val url: String = "",
    var order: Int = 1,
    val type: String = "",
    val description: String = "",
    var id: String = "",
    var imageId: String = ""
): Parcelable