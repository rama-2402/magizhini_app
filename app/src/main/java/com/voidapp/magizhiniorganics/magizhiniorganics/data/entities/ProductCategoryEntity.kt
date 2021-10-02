package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ProductCategoryEntity(
    @PrimaryKey(autoGenerate = false)
    var id: String,
    @ColumnInfo
    var name: String = "",
    @ColumnInfo
    val items: Int,
    @ColumnInfo
    var thumbnailUrl: String = "",
    @ColumnInfo
    var thumbnailName: String = "",
    @ColumnInfo
    var isDiscounted: Boolean,
    @ColumnInfo
    var discountType: String = "percent",
    @ColumnInfo
    var discountAmount: Int = 0,
    @ColumnInfo
    var products: ArrayList<String>,
    @ColumnInfo
    var activated: Boolean = true
)