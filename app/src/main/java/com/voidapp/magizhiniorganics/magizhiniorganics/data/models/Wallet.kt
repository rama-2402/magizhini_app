package com.voidapp.magizhiniorganics.magizhiniorganics.data.modelsdata class Wallet(    var id: String = "",    var amount: Float = 0F,    var lastTransaction: Long = 0L,    var lastRecharge: Long = 0L,    var transactionHistory: List<TransactionHistory> = listOf(),    var extras: ArrayList<String> = arrayListOf())