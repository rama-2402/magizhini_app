package com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.QuickOrderUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CouponEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PURCHASE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.toCartEntity
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

    var orderID: String? = null
    var placeOrderByCOD: Boolean = false
    var couponAppliedPrice: Float? = 0f
    var deliveryCharge: Float = 0f

    var mCheckedAddressPosition: Int = 0
    var addressPosition: Int = 0

    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate
    private val _uiEvent: MutableLiveData<UIEvent> = MutableLiveData()
    val uiEvent: LiveData<UIEvent> = _uiEvent

    fun setEmptyUiEvent() {
        _uiEvent.value = UIEvent.EmptyUIEvent
    }

    fun setEmptyStatus() {
        _uiUpdate.value = UiUpdate.Empty
    }

    fun addNewImageUri(data: Uri) {
        orderListUri.add(data)
    }

    fun getAddress() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val profile = dbRepository.getProfileData()
            profile?.let {
                userProfile = it
                withContext(Dispatchers.Main) {
                    _uiEvent.value = UIEvent.ProgressBar(true)
                    _uiUpdate.value = UiUpdate.AddressUpdate("address", it.address, true)
                }
            }
            checkForPreviousEstimate()
            getWallet(userProfile!!.id)
        } catch (e: IOException) {
            _uiUpdate.value = UiUpdate.AddressUpdate(e.message.toString(), null, false)
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
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.AddressUpdate("update", userProfile!!.address, true)
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.AddressUpdate(e.message.toString(), null, false)
            }
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
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.AddressUpdate("update", userProfile!!.address, true)
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.AddressUpdate(e.message.toString(), null, false)
            }
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
                    withContext(Dispatchers.Main) {
                        _uiUpdate.value = UiUpdate.AddressUpdate("update", userProfile!!.address, true)
                    }
                }
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.AddressUpdate(e.message.toString(), null, false)
            }
            e.message?.let {
                fbRepository.logCrash(
                    "checkout: update address to profile from db",
                    it
                )
            }
        }
    }

    private fun checkForPreviousEstimate() = viewModelScope.launch {
        val result = QuickOrderUseCase(fbRepository)
            .checkForPreviousEstimate(userID = userProfile!!.id)
        when(result) {
            is NetworkResult.Success -> _uiUpdate.value = UiUpdate.EstimateData("", result.data as QuickOrder, true)
            is NetworkResult.Failed -> _uiUpdate.value = UiUpdate.EstimateData(result.message, null, false)
            else -> Unit
        }
    }

    private fun getWallet(userID: String) = viewModelScope.launch {
        when(val result = fbRepository.getWallet(userID)) {
            is NetworkResult.Success -> {
                _uiEvent.value = UIEvent.ProgressBar(false)
                wallet = result.data as Wallet
            }
            is NetworkResult.Failed -> {
                _uiEvent.value = UIEvent.ProgressBar(false)
                _uiEvent.value = UIEvent.SnackBar(result.data.toString(), true)
            }
            else -> Unit
        }
    }

    fun getTotalCartPrice(): Float {
            var cartPrice: Float = 0f
            quickOrder?.let {
                for (item in it.cart) {
                    cartPrice += (item.price * item.quantity)
                }
            }
            return cartPrice
        }

    suspend fun getDeliveryCharge(): Float = withContext(Dispatchers.IO){
        return@withContext userProfile?.let {
            dbRepository.getDeliveryCharge(it.address[mCheckedAddressPosition].LocationCode).deliveryCharge.toFloat()
        } ?: 30f
    }

    fun sendGetEstimateRequest(imageExtensions: MutableList<String>) {
        viewModelScope.launch {
            orderID = fbRepository.generateOrderID()
            val detailsMap: HashMap<String, String> = hashMapOf()
            userProfile?.let {
                detailsMap["id"] = it.id
                detailsMap["name"] = it.name
                detailsMap["phNumber"] = it.phNumber
                detailsMap["orderID"] = orderID!!
            }
            quickOrderUseCase
                .sendGetEstimateRequest(
                    orderListUri,
                    imageExtensions,
                    detailsMap
                )
                .collect { result ->
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is NetworkResult.Loading -> {
                            }
                            is NetworkResult.Success -> {
                                when (result.message) {
                                    "starting" -> {
                                        _uiUpdate.value = UiUpdate.BeginningUpload("")
                                    }
                                    "uploading" -> {
                                        _uiUpdate.value =
                                            UiUpdate.UploadingImage("Uploading Page ${result.data}...")
                                    }
                                    "complete" -> {
                                        _uiUpdate.value =
                                            UiUpdate.UploadComplete("Files Upload Complete!")
                                    }
                                }
                            }
                            is NetworkResult.Failed -> {
                                _uiUpdate.value = UiUpdate.UploadFailed(result.data.toString())
                            }
                            else -> Unit
                        }
                    }
                }
        }
    }

    fun proceedForWalletPayment(
        orderDetailsMap: HashMap<String, Any>
    ) {
        viewModelScope.launch {
            val mrp = (couponAppliedPrice ?: getTotalCartPrice()) + getDeliveryCharge()
            val cartEntity = quickOrder!!.cart.map { it.toCartEntity() }
            wallet?.let {
                if (mrp > it.amount) {
                    _uiEvent.value =
                        UIEvent.SnackBar("Insufficient Wallet Balance. Please choose any other payment method", true)
                    return@launch
                } else {
                    quickOrderUseCase
                        .initiateWalletTransaction(
                            orderDetailsMap,
                            mrp,
                            PURCHASE,
                            cart = cartEntity as ArrayList<CartEntity>
                            )
                        .collect { result ->
                            withContext(Dispatchers.Main) {
                                when (result) {
                                    is NetworkResult.Loading -> {}
                                    is NetworkResult.Success -> {
                                        when (result.message) {
                                            "transaction" -> _uiUpdate.value =
                                                UiUpdate.StartingTransaction(
                                                    result.data.toString()
                                                )
                                            "order" -> _uiUpdate.value = UiUpdate.PlacingOrder("Placing your Order...")
                                            "success" -> _uiUpdate.value = UiUpdate.OrderPlaced("Order Placed Successfully...!")
                                        }
                                    }
                                    is NetworkResult.Failed -> {
                                        when (result.message) {
                                            "wallet" -> _uiUpdate.value = UiUpdate.WalletTransactionFailed(
                                                    result.data.toString()
                                                )
                                            "order" -> _uiUpdate.value = UiUpdate.OrderPlacementFailed(
                                                    result.data.toString()
                                                )
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                }
            }
        }

    }

    fun placeCashOnDeliveryOrder(
        orderDetailsMap: HashMap<String, Any>
    ) {
        viewModelScope.launch {
            val mrp = (couponAppliedPrice ?: getTotalCartPrice()) + getDeliveryCharge()
            val cartEntity = quickOrder?.cart?.map { it.toCartEntity() } ?: arrayListOf()
            quickOrderUseCase
                .placeCashOnDeliveryOrder(
                    orderDetailsMap,
                    cartEntity as ArrayList<CartEntity>,
                    mrp
                ).onEach { result ->
                    when(result) {
                        is NetworkResult.Success -> {
                            if (result.message == "placing") {
                                _uiUpdate.value = UiUpdate.PlacingOrder("Placing your Order...")
                            } else {
                                _uiUpdate.value = UiUpdate.OrderPlaced("Order Placed Successfully...!")
                            }
                        }
                        is NetworkResult.Failed -> {
                            _uiUpdate.value = UiUpdate.OrderPlacementFailed("Server Error! failed to place order. Try again")
                        }
                        else -> Unit
                    }
                }.launchIn(this)
        }
    }

    fun placeOrderWithOnlinePayment(orderDetailsMap: HashMap<String, Any>) = viewModelScope.launch {
        val mrp = (couponAppliedPrice ?: getTotalCartPrice()) + getDeliveryCharge()
        val cartEntity = quickOrder!!.cart.map { it.toCartEntity() }
        quickOrderUseCase
            .placeOnlinePaymentOrder(
                orderDetailsMap,
                mrp,
                PURCHASE,
                "Product Purchase Online Transaction",
                cartEntity as ArrayList<CartEntity>
            ).collect { result ->
                when(result) {
                    is NetworkResult.Success -> {
                        when (result.message) {
                            "validation" -> _uiUpdate.value = UiUpdate.ValidatingPurchase("")
                            "placing" -> _uiUpdate.value = UiUpdate.PlacingOrder("Placing Order...")
                            "placed" -> _uiUpdate.value = UiUpdate.OrderPlaced("Order Placed Successfully...!")
                        }
                    }
                    is NetworkResult.Failed -> {
                        _uiUpdate.value = UiUpdate.OrderPlacementFailed("")
                    }
                    else -> Unit
                }
            }
    }

    fun verifyCoupon(couponCode: String) = viewModelScope.launch(Dispatchers.IO) {
        dbRepository.getCouponByCode(couponCode)?.let { coupon ->
            val cartPrice = getTotalCartPrice()
            if (!coupon.categories.contains(ALL)) {
                withContext(Dispatchers.Main) {
                    _uiEvent.value = UIEvent.Toast("Coupon Applies only for few product categories")
                }
                return@launch
            }
            if (cartPrice > coupon.purchaseLimit) {
                couponAppliedPrice = cartPrice - couponDiscount(coupon, cartPrice)
                withContext(Dispatchers.Main) {
                    _uiUpdate.value = UiUpdate.CouponApplied(
                        "Coupon Applied Successfully! Your updated cart price is Rs: $couponAppliedPrice"
                    )
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiEvent.value = UIEvent.Toast("Coupon Applies only for Purchase more than Rs: ${coupon.purchaseLimit}")
                }
                return@launch
            }
        } ?: withContext(Dispatchers.Main) {
            _uiEvent.value = UIEvent.Toast("Coupon Code does not exist.")
        }
    }

    private fun couponDiscount(coupon: CouponEntity, cartPrice: Float): Float {
        var discountPrice = when (coupon.type) {
            "percent" -> (cartPrice * coupon.amount / 100)
            "rupees" -> coupon.amount
            else -> 0f
        }

        if (discountPrice > coupon.maxDiscount) {
            discountPrice = coupon.maxDiscount
        }

        return discountPrice
    }

    sealed class UiUpdate {
        data class WalletData(val wallet: Wallet): UiUpdate()

        //uploading List
        data class BeginningUpload(val message: String): UiUpdate()
        data class UploadingImage(val message: String): UiUpdate()
        data class UploadComplete(val message: String): UiUpdate()
        data class UploadFailed(val message: String): UiUpdate()

        //address
        data class AddressUpdate(val message: String, val data: ArrayList<Address>?, val isSuccess: Boolean): UiUpdate()

        //estimateData
        data class EstimateData(val message: String, val data: QuickOrder?, val isSuccess: Boolean): UiUpdate()

        //coupon
        data class CouponApplied(val message: String): UiUpdate()

        //order
        data class ValidatingPurchase(val message: String): UiUpdate()
        data class StartingTransaction(val message: String): UiUpdate()
        data class PlacingOrder(val message: String): UiUpdate()
        data class OrderPlaced(val message: String): UiUpdate()
        data class WalletTransactionFailed(val message: String): UiUpdate()
        data class OrderPlacementFailed(val message: String): UiUpdate()
        object Empty: UiUpdate()
    }
}