package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

data class AmmaSpecial(
    var id: String = "",
    var foodName: String = "",
    var thumbnailUrl: String = "",
    var foodTime: String = "",
    var foodDay: String = "",
    var price: Double = 0.0,
    var discountedPrice: Double = 0.0,
    var description: String = "",
    var ingredients: ArrayList<String> = arrayListOf(),
    var displayOrder: Int = 0
)