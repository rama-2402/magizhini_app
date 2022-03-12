package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

data class Testimonials (
    var id: String = "",
    var title: String = "",
    var message: String = "",
    var thumbnailUrl: String = "",
    var videoUrl: String = "",
    var order: Int = 0
)

@Entity
data class TestimonialsEntity (
    @PrimaryKey(autoGenerate = false)
    var id: String = "",
    @ColumnInfo
    var title: String = "",
    @ColumnInfo
    var message: String = "",
    @ColumnInfo
    var thumbnailUrl: String = "",
    @ColumnInfo
    var videoUrl: String = "",
    @ColumnInfo
    var order: Int = 0
)

data class Partners(
    var id: String = "",
    var partnerName: String = "",
    var imageUrl: String = "",
    var clickAction: String = ""
)