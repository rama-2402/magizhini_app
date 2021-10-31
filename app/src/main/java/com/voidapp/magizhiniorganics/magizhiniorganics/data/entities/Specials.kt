package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BestSellers(
    @PrimaryKey(autoGenerate = false)
    var name: String = "",
    @ColumnInfo
    var id: ArrayList<String> = arrayListOf()
)
@Entity
data class SpecialsOne(
    @PrimaryKey(autoGenerate = false)
    var name: String = "",
    @ColumnInfo
    var id: ArrayList<String> = arrayListOf()
)
@Entity
data class SpecialsTwo(
    @PrimaryKey(autoGenerate = false)
    var name: String = "",
    @ColumnInfo
    var id: ArrayList<String> = arrayListOf()
)
@Entity
data class SpecialsThree(
    @PrimaryKey(autoGenerate = false)
    var name: String = "",
    @ColumnInfo
    var id: ArrayList<String> = arrayListOf()
)
@Entity
data class SpecialBanners(
    @PrimaryKey(autoGenerate = false)
    var id: Int = 0,
    @ColumnInfo
    var url: String = ""
)
