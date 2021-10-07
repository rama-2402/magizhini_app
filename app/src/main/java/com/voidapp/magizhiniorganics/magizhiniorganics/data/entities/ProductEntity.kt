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
    var status: String = Constants.AVAILABLE,
    @ColumnInfo
    var discountAvailable: Boolean = false,
    @ColumnInfo
    var discountType: String = "Percentage",
    @ColumnInfo
    var discountAmt: Int = 0,
    @ColumnInfo
    var defaultVariant: Int = 0,
    @ColumnInfo
    var productType: String="",
    @ColumnInfo
    var variants: ArrayList<ProductVariant> = arrayListOf(),
    @ColumnInfo
    var activated: Boolean = true,
    @ColumnInfo
    var reviews: ArrayList<Review> = arrayListOf(),
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
): Parcelable


