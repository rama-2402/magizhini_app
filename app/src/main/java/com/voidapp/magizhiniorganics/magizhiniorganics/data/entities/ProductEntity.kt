package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class ProductEntity(
    @PrimaryKey(autoGenerate = false)
    var id: String = "",
    @ColumnInfo
    var name: String = "",
    @ColumnInfo
    var category: String = "",
    @ColumnInfo
    var thumbnailUrl: String = "",
    @ColumnInfo
    var thumbnailName: String = "",
    @ColumnInfo
    var rating: Int = 0,
    @ColumnInfo
    var description: String = "",
    @ColumnInfo
    var descType: String = "none",
    @ColumnInfo
    var variants: ArrayList<ProductVariant> = ArrayList(),
    @ColumnInfo
    var status: String = "Available",
    @ColumnInfo
    var discountAvailable: Boolean = false,
    @ColumnInfo
    var defaultVariant: Int = 0,
    @ColumnInfo
    var productType: String="",
    @ColumnInfo
    var activated: Boolean = true,
    @ColumnInfo
    var favorite: Boolean = false,
    @ColumnInfo
    var inCart: Boolean = false,
    @ColumnInfo
    var variantInCart: ArrayList<String> = arrayListOf(),
    @ColumnInfo
    var coupon: Boolean = false,
    @ColumnInfo
    var appliedCoupon: String = "",
    @ColumnInfo
    var reviews: ArrayList<Review> = arrayListOf(),
    @ColumnInfo
    var labels: ArrayList<String> = arrayListOf(),
    @ColumnInfo
    var extras: ArrayList<String> = arrayListOf()
): Parcelable


