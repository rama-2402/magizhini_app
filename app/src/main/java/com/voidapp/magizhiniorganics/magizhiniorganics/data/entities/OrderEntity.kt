package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Cart

@Entity
data class OrderEntity(
    @PrimaryKey(autoGenerate = false)
    var orderId: String = "",
    @ColumnInfo
    var customerId: String = "",
    @ColumnInfo
    var transactionID: String = "",
    @ColumnInfo
    var cart: List<CartEntity> = listOf(),
    @ColumnInfo
    var purchaseDate: String = "",
    @ColumnInfo
    var isPaymentDone: Boolean = false,
    @ColumnInfo
    var paymentMethod: String = "",
    @ColumnInfo
    var deliveryPreference: String = "",
    @ColumnInfo
    var deliveryNote: String = "",
    @ColumnInfo
    var appliedCoupon: String = "",
    @ColumnInfo
    var address: Address = Address(),
    @ColumnInfo
    var price: Float = 0F,
    @ColumnInfo
    var orderStatus: String = "pending",
    @ColumnInfo
    var monthYear: String = ""
)
