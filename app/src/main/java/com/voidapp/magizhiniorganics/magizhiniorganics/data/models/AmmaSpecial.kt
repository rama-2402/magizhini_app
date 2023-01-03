package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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

data class OldAmmaSpecialOrder(
    var id: String = "",
    var customerID: String = "",
    var orderDate: String = "",
    var startDate: Long = 0,
    var endDate: Long = 0,
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
    var orderFoodTime: ArrayList<String> = arrayListOf(),
    var orderCount: Int = 1,
    var leafNeeded: Boolean = false,
    var status: String = "waiting",
    var deliveryDates: ArrayList<String> = arrayListOf(),
    var cancelledDates: ArrayList<Long> = arrayListOf()
)

data class AmmaSpecialOrder(
    var id: String = "",
    var customerID: String = "",
    var orderDate: String = "",
    var startDate: Long = 0,
    var endDate: Long = 0,
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
    var orderFoodTime: ArrayList<String> = arrayListOf(),
    var orderCount: Int = 1,
    var leafNeeded: Int = 0,
    var status: String = "waiting",
    var deliveryDates: ArrayList<String> = arrayListOf(),
    var cancelledDates: ArrayList<Long> = arrayListOf()
)

data class AmmaSpecialDeliveryStatus(
    var id: String = "",
    var status: String = "waiting",
    var refund: String = "no"
)

data class NonDeliveryDates(
    val dates: ArrayList<Long> = arrayListOf()
)

@Parcelize
data class MenuImage(
    var id: String = "",
    var name: String = "",
    var thumbnailUrl: String = "",
    var displayOrder: Int = 0,
    var price: Double = 0.0
): Parcelable


fun OldAmmaSpecialOrder.toNewAmmaSpecial() = AmmaSpecialOrder(
    id = id,
    customerID = customerID,
    orderDate = orderDate,
    startDate = startDate,
    endDate = endDate,
    price = price,
    paymentMode = paymentMode,
    userName = userName,
    addressOne = addressOne,
    addressTwo = addressTwo,
    city = city,
    code = code,
    phoneNumber = phoneNumber,
    mailID = mailID,
    orderType = orderType,
    orderFoodTime = orderFoodTime,
    orderCount = orderCount,
    leafNeeded = if (leafNeeded) 1 else 0,
    status = status,
    deliveryDates = deliveryDates,
    cancelledDates = cancelledDates
)

