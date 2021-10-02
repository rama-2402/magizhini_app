package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PinCodesEntity (
    @PrimaryKey(autoGenerate = false)
    var id: String = "",
    @ColumnInfo
    var areaCode: String = "",
    @ColumnInfo
    var deliveryCharge: Int = 0
)