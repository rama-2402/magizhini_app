package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.DefaultVariant

@Entity
data class UserProfileEntity (
    @PrimaryKey(autoGenerate = false)
    var id: String = "",
    @ColumnInfo
    var name: String = "",
    @ColumnInfo
    var phNumber: String = "",
    @ColumnInfo
    var alternatePhNumber: String = "",
    @ColumnInfo
    var dob: String = "",
    @ColumnInfo
    var mailId: String = "",
    @ColumnInfo
    var address: ArrayList<Address> = arrayListOf(),
    @ColumnInfo
    var profilePicUrl: String = "",
    @ColumnInfo
    var referralId: String = "",
    @ColumnInfo
    var defaultProductVariant: ArrayList<DefaultVariant> = ArrayList(),
    @ColumnInfo
    var favorites: ArrayList<String> = ArrayList(),
    @ColumnInfo
    var purchaseHistory: ArrayList<String> = arrayListOf(),
    @ColumnInfo
    var purchasedMonths: ArrayList<String> = arrayListOf(),
    @ColumnInfo
    var member: Boolean = false,
    @ColumnInfo
    var membershipType: String = ""
)