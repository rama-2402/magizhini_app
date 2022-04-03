package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

data class ProductCategory (
    var id: String = "",
    var name: String = "",
    val items: Int = 0,
    var thumbnailUrl: String = "",
    var thumbnailName: String = "",
    var isDiscounted: Boolean = false,
    var discountType: String = "percent",
    var discountAmount: Int = 0,
    var products: ArrayList<String> = ArrayList(),
    var activated: Boolean = true,
    var extras: ArrayList<String> = arrayListOf(),
    var status: String = "Available"
)