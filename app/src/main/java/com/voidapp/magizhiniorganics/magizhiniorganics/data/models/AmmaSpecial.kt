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

data class AmmaSpecialOrder(
    var id: String = "",
    var customerID: String = "",
    var orderDate: String = "",
    var price: Double = 0.0,
    var paymentMode: String = "",
    var userName: String = "",
    var addressOne: String = "",
    var addressTwo: String = "",
    var city: String = "",
    var code: String = "",
    var phoneNumber: String = "",
    var mailID: String = "",
    var orderType: String = "month",
    var orderCount: Int = 1,
    var leafNeeded: Boolean = false,
    var status: String = "",
    var deliveryDates: ArrayList<Long> = arrayListOf(),
    var cancelledDates: ArrayList<Long> = arrayListOf()
)

data class AmmaSpecialDeliveryStatus(
    var id: String = "",
    var status: String = ""
)