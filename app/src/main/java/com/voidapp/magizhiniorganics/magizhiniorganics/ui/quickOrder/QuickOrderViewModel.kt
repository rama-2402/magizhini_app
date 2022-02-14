package com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Operation
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.QuickOrderUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.UserProfileDao
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BEST_SELLERS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ORDER_ESTIMATE_PATH
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PURCHASE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SHORT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.toCartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.toOrderEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException

class QuickOrderViewModel(
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository,
    private val quickOrderUseCase: QuickOrderUseCase
): ViewModel() {

    val orderListUri: MutableList<Uri> = mutableListOf()
    var userProfile: UserProfileEntity? = null
    var addressContainer: Address? = null
    var quickOrder: QuickOrder? = null
    var wallet: Wallet? = null

    var mCheckedAddressPosition: Int = 0
    var addressPosition: Int = 0

//    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(
//        NetworkResult.Empty
//    )
//    val status: StateFlow<NetworkResult> = _status.asStateFlow()

    private val _uiEvent: MutableLiveData<UiEvent> = MutableLiveData()
    val uiEvent: LiveData<UiEvent> = _uiEvent

//    private val _uiEvent: MutableStateFlow<UiEvent> = MutableStateFlow<UiEvent>(UiEvent.Empty)
//    val uiEvent: StateFlow<UiEvent> = _uiEvent.asStateFlow()

    private val _displayMessage: MutableSharedFlow<UiEvent> = MutableSharedFlow()
    val displayMessage = _displayMessage.asSharedFlow()

    fun setEmptyStatus() {
//        _status.value = NetworkResult.Empty
        _uiEvent.value = UiEvent.Empty
    }

    fun addNewImageUri(data: Uri) {
        orderListUri.add(data)
    }

    fun getAddress() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val profile = dbRepository.getProfileData()
            profile?.let {
                userProfile = it
//                _status.value = NetworkResult.Success("address", it.address)
                withContext(Dispatchers.Main) {
                    _uiEvent.value = UiEvent.AddressUpdate("address", it.address, true)
                }
            }
            checkForPreviousEstimate()
            getWallet()
        } catch (e: IOException) {
//            _status.value = NetworkResult.Failed("address", "Failed to fetch address. Please try again later")
            _uiEvent.value = UiEvent.AddressUpdate(e.message.toString(), null, false)
        }
    }

    fun deleteAddress(position: Int) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val localUpdate = async {
                userProfile?.let {
                    it.address.removeAt(position)
                    dbRepository.upsertProfile(it)
                }
            }
            val cloudUpdate = async {
                userProfile?.let {
                    updateAddress(it.id, it.address, "update")
                }
            }

            localUpdate.await()
            cloudUpdate.await()
            _uiEvent.value = UiEvent.AddressUpdate("update", userProfile!!.address, true)
//            _status.value = NetworkResult.Success("addressUpdate", "Address Deleted")
        } catch (e: IOException) {
            _uiEvent.value = UiEvent.AddressUpdate(e.message.toString(), null, false)
//            _status.value = NetworkResult.Success("addressUpdate", "Failed to delete address. Try later")
            e.message?.let {
                fbRepository.logCrash(
                    "checkout: update address to profile from db",
                    it
                )
            }
        }
    }

    private suspend fun updateAddress(id: String, address: ArrayList<Address>, status: String) {
        if (status == "add") {
            fbRepository.addAddress(id, address[0])
        } else {
            fbRepository.updateAddress(id, address)
        }
    }

    fun addAddress(newAddress: Address) = viewModelScope.launch(Dispatchers.IO) {
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
                    updateAddress(it.id, list, "add")
                }
            }

            localUpdate.await()
            cloudUpdate.await()
            _uiEvent.value = UiEvent.AddressUpdate("update", userProfile!!.address, true)
//            _status.value = NetworkResult.Success("addressUpdate", userProfile!!.address)
        } catch (e: IOException) {
            _uiEvent.value = UiEvent.AddressUpdate(e.message.toString(), null, false)
//            _status.value = NetworkResult.Success("addressUpdate", "Failed to add address. Try later")
            e.message?.let { fbRepository.logCrash("checkout: add address to profile from db", it) }
        }
    }

    fun updateAddress(address: Address) = viewModelScope.launch(Dispatchers.IO) {
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
                    _uiEvent.value = UiEvent.AddressUpdate("update", userProfile!!.address, true)
//                    _status.value = NetworkResult.Success("addressUpdate", userProfile!!.address)
                }
            }
        } catch (e: IOException) {
            _uiEvent.value = UiEvent.AddressUpdate(e.message.toString(), null, false)
//            _status.value = NetworkResult.Success("addressUpdate", "Failed to update address. Try later")
            e.message?.let {
                fbRepository.logCrash(
                    "checkout: update address to profile from db",
                    it
                )
            }
        }
    }

    fun sendGetEstimateRequest(imageExtensions: MutableList<String>) {
        viewModelScope.launch {
            quickOrderUseCase
                .sendGetEstimateRequest(
                    orderListUri,
                    imageExtensions,
                    userProfile!!.id,
                    userProfile!!.name,
                    userProfile!!.phNumber
                )
                .collect { result ->
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is NetworkResult.Loading -> {
//                        _uiEvent.value = UiEvent.BeginningUpload("BeginningUpload")
                            }
                            is NetworkResult.Success -> {
                                when (result.message) {
                                    "starting" -> {
                                        Log.e("qw", "startvm",)
                                        _uiEvent.value = UiEvent.BeginningUpload("")
//                                  _uiEvent.value = UiEvent.BeginningUpload("")
                                    }
                                    "uploading" -> {
                                        Log.e("qw", "${result.data}")
                                        _uiEvent.value =
                                            UiEvent.UploadingImage(result.data.toString())
                                    }
                                    "complete" -> {
                                        Log.e("qw", "completevm",)
                                        _uiEvent.value =
                                            UiEvent.UploadComplete(result.data.toString())
                                    }
                                }
                            }
                            is NetworkResult.Failed -> {
//                            _status.value = NetworkResult.Failed("complete", result.data)
                                _displayMessage.emit(UiEvent.SnackBar(result.data.toString(), true))
                            }
                            else -> Unit
                        }
                    }
                }
        }
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

    private fun checkForPreviousEstimate() = viewModelScope.launch {
        val result = QuickOrderUseCase(fbRepository)
            .checkForPreviousEstimate(userID = userProfile!!.id)
        when(result) {
            is NetworkResult.Success -> _uiEvent.value = UiEvent.EstimateData("", result.data as QuickOrder, true)
            is NetworkResult.Failed -> _uiEvent.value = UiEvent.EstimateData(result.message, null, false)
        }
    }

    private fun getWallet() = viewModelScope.launch {
        //todo
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

    fun proceedForWalletPayment(
        orderDetailsMap: HashMap<String, Any>
    ) {
        viewModelScope.launch {
            val mrp = getTotalCartPrice()
            userProfile?.let {
                orderDetailsMap["userID"] = it.id
                orderDetailsMap["phoneNumber"] = it.phNumber
                orderDetailsMap["address"] = addressContainer ?: it.address[0]
            }
            val cartEntity = quickOrder!!.cart.map { it.toCartEntity() }
            wallet?.let {
                if (mrp > it.amount) {
                    _displayMessage.emit(
                        UiEvent.SnackBar("Insufficient Wallet Balance. Please choose any other payment method", true)
                    )
                    return@launch
                } else {
                    quickOrderUseCase
                        .initiateWalletTransaction(
                            orderDetailsMap,
                            mrp,
                            PURCHASE,
                            cart = cartEntity as ArrayList<CartEntity>
                            )
                        .onEach { result ->
                            withContext(Dispatchers.Main) {
                                when (result) {
                                    is NetworkResult.Loading -> {

                                    }
                                    is NetworkResult.Success -> {
                                        when (result.message) {
//                                            "transaction" -> _uiEvent.emit(
//                                                UiEvent.StartingTransaction(
//                                                    result.data.toString()
//                                                )
//                                            )
//                                            "order" -> _uiEvent.emit(UiEvent.PlacingOrder(result.data.toString()))
//                                            "success" -> _uiEvent.emit(UiEvent.OrderPlaced(result.data.toString()))
                                        }
                                    }
                                    is NetworkResult.Failed -> {
                                        when (result.message) {
//                                            "wallet" -> _uiEvent.emit(
//                                                UiEvent.WalletTransactionFailed(
//                                                    result.data.toString()
//                                                )
//                                            )
//                                            "order" -> _uiEvent.emit(
//                                                UiEvent.OrderPlacementFailed(
//                                                    result.data.toString()
//                                                )
//                                            )
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }.launchIn(this)
                }
            }
        }

    }

    sealed class UiEvent {
        data class WalletData(val wallet: Wallet): UiEvent()
        data class Toast(val data: String, val duration: String = SHORT): UiEvent()
        data class SnackBar(val data: String, val isError: Boolean) : UiEvent()
        data class ProgressBar(val visibility: Boolean): UiEvent()

        //uploading List
        data class BeginningUpload(val message: String): UiEvent()
        data class UploadingImage(val pageNumber: String): UiEvent()
        data class UploadComplete(val message: String): UiEvent()

        //address
        data class AddressUpdate(val message: String, val data: ArrayList<Address>?, val isSuccess: Boolean): UiEvent()

        //estimateData
        data class EstimateData(val message: String, val data: QuickOrder?, val isSuccess: Boolean): UiEvent()

        //order
        data class WalletTransactionFailed(val message: String): UiEvent()
        data class OrderPlacementFailed(val message: String): UiEvent()
        data class StartingTransaction(val message: String): UiEvent()
        data class PlacingOrder(val message: String): UiEvent()
        data class OrderPlaced(val message: String): UiEvent()
        object Empty: UiEvent()
    }
}