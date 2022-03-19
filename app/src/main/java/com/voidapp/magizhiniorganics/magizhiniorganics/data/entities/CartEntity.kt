package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity
@Parcelize
data class CartEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    @ColumnInfo
    var variant: String = "",
    @ColumnInfo
    var productId: String = "",
//    @ColumnInfo
//    var product: ProductEntity = ProductEntity(),
    @ColumnInfo
    var productName: String = "",
    @ColumnInfo
    var thumbnailUrl: String = "",
    @ColumnInfo
    var quantity: Int = 0,
    @ColumnInfo
    var maxOrderQuantity: Int = 0,
    @ColumnInfo
    var price: Float = 0F,
    @ColumnInfo
    var originalPrice: Float = 0F,
    @ColumnInfo
    var couponName: String = "",
    @ColumnInfo
    var variantIndex: Int = 0
): Parcelable
