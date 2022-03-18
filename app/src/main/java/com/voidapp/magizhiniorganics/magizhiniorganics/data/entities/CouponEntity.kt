package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import android.os.Parcelable
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class CouponEntity(
    @PrimaryKey(autoGenerate = false)
    var id: String = "",
    @ColumnInfo
    var name: String = "",
    @ColumnInfo
    var code: String = "",
    @ColumnInfo
    var description: String = "",
    @ColumnInfo
    var status: String = "active",
    @ColumnInfo
    var type: String = "percent",
    @ColumnInfo
    var amount:  Float = 0f,
    @ColumnInfo
    var purchaseLimit: Float = 0f,
    @ColumnInfo
    var maxDiscount:Float = 0f,
    @ColumnInfo
    var from: String = Constants.DATE_CODE,
    @ColumnInfo
    var expiryDate: String = Constants.DATE_CODE,
    @ColumnInfo
    var categories: ArrayList<String> = arrayListOf(),
    @ColumnInfo
    var extras: ArrayList<String> = arrayListOf()
): Parcelable