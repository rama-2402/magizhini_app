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
    var autoPay: Boolean = false,
    @ColumnInfo
    var paymentMode: String = "Wallet",
    @ColumnInfo
    var estimateAmount: Float = 0f,
    @ColumnInfo
    var subType: String = "Single Purchase",
    @ColumnInfo
    var status: String = "Active",
    @ColumnInfo
    var deliveredDates: ArrayList<Long> = arrayListOf(),
    @ColumnInfo
    var cancelledDates: ArrayList<Long> = arrayListOf(),
    @ColumnInfo
    var notDeliveredDates: ArrayList<Long> = arrayListOf()
)
