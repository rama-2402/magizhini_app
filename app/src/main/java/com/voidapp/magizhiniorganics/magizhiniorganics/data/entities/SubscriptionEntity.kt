package com.voidapp.magizhiniorganics.magizhiniorganics.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address

@Entity
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = false)
    var id: String = "",
    @ColumnInfo
    var productID: String = "",
    @ColumnInfo
    var productName: String = "",
    @ColumnInfo
    var variantName: String = "",
    @ColumnInfo
    var phoneNumber: String = "",
    @ColumnInfo
    var customerID: String = "",
    @ColumnInfo
    var address: Address = Address(),
    @ColumnInfo
    var monthYear: String = "",
    @ColumnInfo
    var startDate: Long = 0L,
    @ColumnInfo
    var endDate: Long = 0L,
    @ColumnInfo
    var basePay: Float = 0f,
    @ColumnInfo
    var paymentMode: String = "Wallet",
    @ColumnInfo
    var estimateAmount: Float = 0f,
    @ColumnInfo
    var subType: String = "Monthly",
    @ColumnInfo
    var status: String = "Active",
    @ColumnInfo
    var customDates: ArrayList<String> = arrayListOf(),
    @ColumnInfo
    var deliveredDates: ArrayList<Long> = arrayListOf(),
    @ColumnInfo
    var cancelledDates: ArrayList<Long> = arrayListOf(),
    @ColumnInfo
    var notDeliveredDates: ArrayList<Long> = arrayListOf(),
    @ColumnInfo
    var extras: ArrayList<String> = arrayListOf()
)
