package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ActiveSubscriptions(
    @PrimaryKey(autoGenerate = false)
    var id: String = ""
)
