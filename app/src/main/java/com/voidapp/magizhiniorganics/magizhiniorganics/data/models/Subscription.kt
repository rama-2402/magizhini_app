package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

data class Subscription(
    var id: String = "",
    var productID: String = "",
    var productName: String = "",
    var variantName: String = "",
    var phoneNumber: String = "",
    var customerID: String = "",
    var address: Address = Address(),
    var monthYear: String = "",
    var startDate: Long = 0L,
    var endDate: Long = 0L,
    var autoPay: Boolean = false,
    var paymentMode: String = "Wallet",
    var estimateAmount: Float = 0f,
    var subType: String = "Single Purchase",
    var status: String = "",
    var deliveredDates: ArrayList<Long> = arrayListOf(),
    var cancelledDates: ArrayList<Long> = arrayListOf(),
    var notDeliveredDates: ArrayList<Long> = arrayListOf()
)
