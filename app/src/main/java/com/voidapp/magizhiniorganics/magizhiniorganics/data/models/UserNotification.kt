package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import java.sql.Timestamp

data class UserNotification (
    var id: String = "",
    var userID: String = "",
    var timestamp: Long = 0L,
    var title: String = "",
    var message: String = "",
    var imageUrl: String = "",
    var clickType: String = "",
    var clickContent: String = ""
        )