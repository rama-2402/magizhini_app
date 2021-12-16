package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Review(
    var id: String = "",
    var userName: String = "",
    var userProfilePicUrl: String = "",
    var timeStamp: Long = 0L,
    var rating: Int = 0,
    var review: String = "",
    var reviewImageUrl: String = ""
): Parcelable
