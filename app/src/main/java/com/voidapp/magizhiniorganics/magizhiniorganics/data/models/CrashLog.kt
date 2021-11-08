package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

data class CrashLog(
    var id: String,
    var details: String,
    var timeStamp: Long,
    var location: String,
    var error: String
)
