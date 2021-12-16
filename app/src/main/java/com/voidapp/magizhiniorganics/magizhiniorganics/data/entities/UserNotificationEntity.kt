package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UserNotificationEntity (
    @PrimaryKey(autoGenerate = false)
    var id: String = "",
    @ColumnInfo
    var userID: String = "",
    @ColumnInfo
    var timestamp: Long = 0L,
    @ColumnInfo
    var title: String = "",
    @ColumnInfo
    var message: String = "",
    @ColumnInfo
    var imageUrl: String = "",
    @ColumnInfo
    var clickType: String = "",
    @ColumnInfo
    var clickContent: String = ""
        )