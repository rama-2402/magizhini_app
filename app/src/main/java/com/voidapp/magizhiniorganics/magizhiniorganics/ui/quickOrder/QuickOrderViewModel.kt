package com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Operation
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.QuickOrderUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.UserProfileDao
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.QuickOrder
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ORDER_ESTIMATE_PATH
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.toOrderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class QuickOrderViewModel(
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository
): ViewModel() {

    val orderListUri: MutableList<Uri> = mutableListOf()
    var userProfile: UserProfileEntity? = null
    var addressContainer: Address? = null
    var quickOrder: QuickOrder? = null

    var mCheckedAddressPosition: Int = 0
    var addressPosition: Int = 0

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(
        NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    fun setEmptyStatus() {
        _status.value = NetworkResult.Empty
    }

    fun addNewImageUri(data: Uri) {
        orderListUri.add(data)
    }

    fun getAddress() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val profile = dbRepository.getProfileData()
            profile?.let {
                userProfile = it
                _status.value = NetworkResult.Success("address", it.address)
            }
//            checkForPreviousEstimate()
        } catch (e: IOException) {
            _status.value = NetworkResult.Failed("address", "Failed to fetch address. Please try again later")
        }
    }

    fun deleteAddress(position: Int) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val localUpdate = async {
                userProfile?.let {
                    it.address.removeAt(position)
                    dbRepository.upsertProfile(it)
                }
            }
            val cloudUpdate = async {
                userProfile?.let {
                    updateAddress(it.id, it.address,"update")
                }
            }

            localUpdate.await()
            cloudUpdate.await()
            _status.value = NetworkResult.Success("addressUpdate", "Address Deleted")
        } catch (e: IOException) {
            _status.value = NetworkResult.Success("addressUpdate", "Failed to delete address. Try later")
            e.message?.let { fbRepository.logCrash("checkout: update address to profile from db", it) }
        }
    }

    private suspend fun updateAddress(id: String, address: ArrayList<Address>, status: String) {
        if(status == "add") {
            fbRepository.addAddress(id, address[0])
        } else {
            fbRepository.updateAddress(id, address)
        }
    }

    fun addAddress(newAddress: Address) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val list = arrayListOf<Address>(newAddress)

            val localUpdate = async {
                userProfile?.let {
                    it.address.add(newAddress)
                    dbRepository.upsertProfile(it)
                }
            }

            val cloudUpdate = async {
                    userProfile?.let {
                        updateAddress(it.id, list,"add")
                    }
                }

            localUpdate.await()
            cloudUpdate.await()
            _status.value = NetworkResult.Success("addressUpdate", userProfile!!.address)
        } catch (e: IOException) {
            _status.value = NetworkResult.Success("addressUpdate", "Failed to add address. Try later")
            e.message?.let { fbRepository.logCrash("checkout: add address to profile from db", it) }
        }
    }

    fun updateAddress(address: Address) = viewModelScope.launch (Dispatchers.IO){
        try {
            userProfile?.let { profile ->
                profile.address[addressPosition].apply {
                    userId = address.userId
                    addressLineOne = address.addressLineOne
                    addressLineTwo = address.addressLineTwo
                    LocationCode = address.LocationCode
                    LocationCodePosition = address.LocationCodePosition
                }.run {
                    val localUpdate = async {
                        dbRepository.upsertProfile(profile)
                    }
                    val cloudUpdate = async {
                        updateAddress(profile.id, profile.address, "update")
                    }

                    localUpdate.await()
                    cloudUpdate.await()
                    _status.value = NetworkResult.Success("addressUpdate", userProfile!!.address)
                }
            }
        } catch (e: IOException) {
            _status.value = NetworkResult.Success("addressUpdate", "Failed to update address. Try later")
            e.message?.let { fbRepository.logCrash("checkout: update address to profile from db", it) }
        }
    }

    fun sendGetEstimateRequest(imageExtensions: MutableList<String>) = viewModelScope.launch(Dispatchers.IO) {
        QuickOrderUseCase()
            .sendGetEstimateRequest(
                orderListUri,
                imageExtensions,
                userProfile!!.id,
                userProfile!!.name,
                userProfile!!.phNumber
            )
            .onEach { result ->
                when(result) {
                    is NetworkResult.Loading -> {
                        _status.value = NetworkResult.Success("starting", null)
                    }
                    is NetworkResult.Success -> {
                        when(result.message) {
                            "uploading" -> {
                                _status.value = NetworkResult.Success("uploading", result.data)
                            }
                            "complete" -> {
                                _status.value = NetworkResult.Success("complete", result.data)
                            }
                        }
                    }
                    is NetworkResult.Failed -> {
                        _status.value = NetworkResult.Failed("complete", result.data)
                    }
                    else -> Unit
                }
            }.launchIn(this)
    }

    fun sendOrderPlaceRequest() = viewModelScope.launch(Dispatchers.IO) {
        val orderMap: HashMap<String, Any> = hashMapOf()
        orderMap["customerId"] = userProfile!!.id
        orderMap["deliveryPreference"] = ""
        orderMap["customerId"] = userProfile!!.id
        orderMap["customerId"] = userProfile!!.id
        orderMap["customerId"] = userProfile!!.id
        orderMap["customerId"] = userProfile!!.id
        orderMap["customerId"] = userProfile!!.id
    }

    fun checkForPreviousEstimate() = viewModelScope.launch {
        _status.value = NetworkResult.Loading("")
         _status.value = QuickOrderUseCase()
            .checkForPreviousEstimate(userID = userProfile!!.id)
//            .onEach { result ->
//                when(result) {
//                    is NetworkResult.Loading -> _status.value = NetworkResult.Loading("")
//                    is NetworkResult.Success -> {
//                        if (result.message == "estimate") {
//                            _status.value = NetworkResult.Success("estimate", data = result.data)
//                        } else {
//                            _status.value = NetworkResult.Success("empty", null)
//                        }
//                    }
//                    is NetworkResult.Failed -> _status.value = NetworkResult.Failed("estimate", data = result.data)
//                    else -> Unit
//                }
//            }.launchIn(this)
    }

    fun getTotalCartPrice(): Float {
        var cartPrice: Float = 0f
        quickOrder?.let {
            for (item in it.cart) {
                cartPrice += item.price
            }
        }
        return cartPrice
    }
}