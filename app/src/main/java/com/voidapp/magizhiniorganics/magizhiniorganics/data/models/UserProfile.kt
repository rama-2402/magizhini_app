package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserProfile(
    var id: String = "",
    var name: String = "",
    var phNumber: String = "",
    var alternatePhNumber: String = "",
    var dob: String = "",
    var address: ArrayList<Address> = arrayListOf(),
    var mailId: String = "",
    var profilePicUrl: String = "",
    var referrerNumber: String = "",
    var defaultProductVariant: ArrayList<DefaultVariant> = ArrayList(),
    var favorites: ArrayList<String> = ArrayList(),
    var purchaseHistory: ArrayList<String> = arrayListOf(),
): Parcelable