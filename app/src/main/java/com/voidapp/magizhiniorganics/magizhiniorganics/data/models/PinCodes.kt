package com.voidapp.magizhiniorganics.magizhiniorganics.data.modelsdata class PinCodes (    var id: String = "",    var areaCode: String = "",    var deliveryCharge: Int = 0,    var deliveryAvailable: Boolean = false,    var extras: ArrayList<String> = arrayListOf())data class FreeDeliveryLimit (    val limit: Float = 0f)