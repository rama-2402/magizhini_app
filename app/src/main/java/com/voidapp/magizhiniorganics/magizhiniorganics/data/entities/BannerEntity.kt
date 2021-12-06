package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BannerEntity(
    @PrimaryKey(autoGenerate = false)
    var id: String = "",
    @ColumnInfo
    var url: String = "",
    @ColumnInfo
    var order: Int = 1,
    @ColumnInfo
    var type: String = "",
    @ColumnInfo
    var description: String = ""
)
