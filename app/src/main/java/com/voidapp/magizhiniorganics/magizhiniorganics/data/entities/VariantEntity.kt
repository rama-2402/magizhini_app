package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant

@Entity
data class VariantEntity(
    @PrimaryKey(autoGenerate = true)
    var primId: Int,
    @ColumnInfo
    var userId: String = "",
    @ColumnInfo
    var productId: String = "",
    @ColumnInfo
    var productName: String,
    @ColumnInfo
    var variantName: String,
    @ColumnInfo
    var variantPosition: Int,
    @ColumnInfo
    var isDefault: Boolean = false
)
