package com.voidapp.magizhiniorganics.magizhiniorganics.data.modelsdata class Wallet(    var id: String = "",    var amount: Float = 0F,    var lastTransaction: Long = 0L,    var lastRecharge: Long = 0L,    var transactionHistory: List<TransactionHistory> = listOf(),    var extras: ArrayList<String> = arrayListOf())data class RefundEntry(    var id: String = "",    var customerID: String = "",    var customerName: String = "",    var orderID: String = "",    var cancelledDateTime: Long = 0,    var refundAmount: String = "",    var refunded: Boolean = false,    var modeOfRefund: String = "",    var refundFor: String = "")