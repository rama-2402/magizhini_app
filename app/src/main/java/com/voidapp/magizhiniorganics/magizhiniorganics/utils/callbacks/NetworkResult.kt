package com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks

sealed class NetworkResult {
    data class Success(val message: String, val data: Any?): NetworkResult()
    data class Failed(val message: String, val data: Any?): NetworkResult()
    data class Loading(val message: String, val data: String = ""): NetworkResult()
    object Empty: NetworkResult()
}
