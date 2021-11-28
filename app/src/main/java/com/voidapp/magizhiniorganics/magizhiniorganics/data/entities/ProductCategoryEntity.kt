package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ProductCategoryEntity(
    @PrimaryKey(autoGenerate = false)
    var id: String = "",
    @ColumnInfo
    var name: String = "",
    @ColumnInfo
    var items: Int = 0,
    @ColumnInfo
    var thumbnailUrl: String = "",
    @ColumnInfo
    var thumbnailName: String = "",
    @ColumnInfo
    var isDiscounted: Boolean = false,
    @ColumnInfo
    var discountType: String = "percent",
    @ColumnInfo
    var discountAmount: Int = 0,
    @ColumnInfo
    var products: ArrayList<String> = arrayListOf(),
    @ColumnInfo
    var activated: Boolean = true
)