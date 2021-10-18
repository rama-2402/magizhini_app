package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class Order(
    var orderId: String = "",
    var customerId: String = "",
    var transactionID: String = "",
    var cart: List<CartEntity> = listOf(),
    var purchaseDate: String = "",
    var isPaymentDone: Boolean = false,
    var paymentMethod: String = "",
    var deliveryPreference: String = "",
    var deliveryNote: String = "",
    var appliedCoupon: String = "",
    var address: Address = Address(),
    var price: Float = 0F,
    var orderStatus: String = "Pending",
    var monthYear: String = ""
): Parcelable
