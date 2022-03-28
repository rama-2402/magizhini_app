package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

data class Career(
    val name: String = "",
    val qualification: String = "",
    val vacancy: Int = 0,
    val url: String = "",
    val thumbnail: String = ""
)


data class NewPartner (
    var id: String = "",
    val partnerName: String = "",
    val business: String = "",
    val phone: String = "",
    val mail: String = "",
    val social: String? = "",
    val description: String = ""
)