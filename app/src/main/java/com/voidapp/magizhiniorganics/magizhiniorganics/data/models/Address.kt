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
): Parcelable {
    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) {
            return false
        }

        other as Address

        return when {
            userId != other.userId -> false
            addressLineOne != other.addressLineOne -> false
            addressLineTwo != other.addressLineTwo -> false
            LocationCode != other.LocationCode -> false
            LocationCodePosition != other.LocationCodePosition -> false
            city != other.city -> false
            gpsLatitude != other.gpsLatitude -> false
            gpsLongitude != other.gpsLongitude -> false
            gpsAddress != other.gpsAddress -> false
            else -> true
        }
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + addressLineOne.hashCode()
        result = 31 * result + addressLineTwo.hashCode()
        result = 31 * result + LocationCode.hashCode()
        result = 31 * result + LocationCodePosition
        result = 31 * result + city.hashCode()
        result = 31 * result + gpsLatitude.hashCode()
        result = 31 * result + gpsLongitude.hashCode()
        result = 31 * result + gpsAddress.hashCode()
        return result
    }
}


