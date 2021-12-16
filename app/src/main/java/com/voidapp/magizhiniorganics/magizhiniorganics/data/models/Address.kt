package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Address(
    var userId: String = "",
    var addressLineOne: String = "",
    var addressLineTwo: String = "",
    var LocationCode: String = "",
    var LocationCodePosition: Int = 0,
    var city: String = "",
    var gpsLatitude: String = "",
    var gpsLongitude: String = "",
    var gpsAddress: String = ""
): Parcelable
