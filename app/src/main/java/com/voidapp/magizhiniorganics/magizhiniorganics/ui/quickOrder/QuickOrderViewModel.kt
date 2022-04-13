package com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.QuickOrderUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Cart
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.QuickOrder
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PURCHASE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.toCartEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.io.IOException

//delete the quick order data when placing order after getting the estimate
//delete the images in quick order as well

class QuickOrderViewModel(
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository,
    private val quickOrderUseCase: QuickOrderUseCase
): ViewModel() {

    val tempFilesList: MutableList<File> = mutableListOf()
    val orderListUri: MutableList<Uri> = mutableListOf()
    var userProfile: UserProfileEntity? = null
    var addressContainer: Address? = null
    var quickOrder: QuickOrder? = null
    var wallet: Wallet? = null
    var appliedCoupon: CouponEntity? = null

    var orderID: String? = null
    var placeOrderByCOD: Boolean = false
    var couponAppliedPrice: Float? = null
    var deliveryCharge: Float = 0f

    private val _deliveryNotAvailableDialog: MutableLiveData<Long> = MutableLiveData()
    val deliveryNotAvailableDialog: LiveData<Long> = _deliveryNotAvailableDialog
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
                    _uiUpdate.value = UiUpdate.AddressUpdate("address", it.address[0], true)
                }
            }
            // while getting the address data we also check for previous estimate request and get the user wallet
            checkForPreviousEstimate()
            getWallet(userProfile!!.id)
            getDeliveryCharge()
        } catch (e: IOException) {
            _uiUpdate.value = UiUpdate.AddressUpdate(e.message.toString(), null, false)
        }
    }

    //this func is to update the address change to the firestore

    fun updateAddress(address: Address) = viewModelScope.launch(Dispatchers.IO) {
        try {
            userProfile?.let { profile ->
                profile.address[0].apply {
                    userId = address.userId
                    addressLineOne = address.addressLineOne
                    addressLineTwo = address.addressLineTwo
                    LocationCode = address.LocationCode
                    LocationCodePosition = address.LocationCodePosition
                }.let { address ->
                    val localUpdate = async {
                        dbRepository.upsertProfile(profile)
                    }
                    val cloudUpdate = async {
                        fbRepository.updateAddress(profile.id, arrayListOf(address))
                    }

                    localUpdate.await()
                    cloudUpdate.await()
                    withContext(Dispatchers.Main) {
                        _uiUpdate.value = UiUpdate.AddressUpdate("update", userProfile!!.address[0], true)
                    }
                    getDeliveryCharge()
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
            is NetworkResult.Success -> {
                /*
                * If there is quick order data available then we pass it to populate it's data
                * Then we check if order is placed for that quick order and if placed then we
                * pass only the delivery address details and rest of the order details to be populated
                * */
                result.data?.let { it ->
                    it as QuickOrder
                    _uiUpdate.value =
                        UiUpdate.EstimateData("", it, true)
                    delay(1500)
                    if (it.orderPlaced) {
                        //incase order is placed we override estimate data with this new ones and hide progress dialog
                        updateAddressToOrderAddress(it.orderID)
                    } else {
                        //if no order placed then we hide the progressbar immediately
                        _uiEvent.value = UIEvent.ProgressBar(false)
                    }
                } ?:let {
                    _uiUpdate.value =
                        UiUpdate.EstimateData("", null, true)
                }
            }
            is NetworkResult.Failed -> {
                _uiUpdate.value = UiUpdate.EstimateData(result.data?.let { it as String }
                    ?: "Server Error! Try later", null, false)
                delay(1500)
                _uiEvent.value = UIEvent.ProgressBar(false)
            }
            else -> Unit
        }
    }

    private fun updateAddressToOrderAddress(orderID: String) = viewModelScope.launch(Dispatchers.IO) {
        dbRepository.getOrderByID(orderID)?.let { order ->
            withContext(Dispatchers.Main) {
                _uiUpdate.value =
                    UiUpdate.PopulateOrderDetails(order)
            }
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
            dbRepository.getDeliveryCharge(it.address[0].LocationCode)?.let { pinCodes ->
                if (pinCodes.isNullOrEmpty()) {
                    deliveryAvailability(null)
                    30f
                } else {
                    deliveryAvailability(pinCodes[0])
                    pinCodes[0].deliveryCharge.toFloat()
                }
            } ?:let {
                deliveryAvailability(null)
                30f
            }
        } ?: 30f
    }

    private suspend fun deliveryAvailability(pinCodesEntity: PinCodesEntity?) = withContext(Dispatchers.Main){
        pinCodesEntity?.let {
            if(!pinCodesEntity.deliveryAvailable) {
                _deliveryNotAvailableDialog.value = System.currentTimeMillis()
            }
        } ?:let { _deliveryNotAvailableDialog.value = System.currentTimeMillis() }
    }

    fun sendGetEstimateRequest(tempFileUriList: MutableList<Uri>) {
        viewModelScope.launch {
            orderID = generateOrderID()
            val detailsMap: HashMap<String, String> = hashMapOf()
            userProfile?.let {
                detailsMap["id"] = it.id
                detailsMap["name"] = it.name
                detailsMap["phNumber"] = it.phNumber
                detailsMap["orderID"] = orderID!!
            }
            quickOrderUseCase
                .sendGetEstimateRequest(
                    tempFileUriList,
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
                                        for (file in tempFilesList) {
                                            file.delete()
                                        }
                                        tempFilesList.clear()
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

    private fun generateOrderID(): String {
        return userProfile?.let {
            TimeUtil().getOrderIDFormat(it.phNumber.takeLast(4))
        } ?: TimeUtil().getOrderIDFormat("${TimeUtil().getMonthNumber()}${TimeUtil().getDateNumber(0L)}")
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
                            cart = cartEntity as ArrayList<CartEntity>,
                            true
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
                    mrp,
                    true
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
                cartEntity as ArrayList<CartEntity>,
                true
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
            appliedCoupon = coupon
            val cartPrice = getTotalCartPrice()
            /*
            * If the coupon is for all categories we accept it else throw error
            * */
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

    fun getCartItemsQuantity(cartItems: ArrayList<Cart>): Int {
        var quantities = 0
        cartItems.forEach {
            quantities += it.quantity
        }
        return quantities
    }

    fun deleteQuickOrder() = viewModelScope.launch {
        quickOrder?.let {
            quickOrderUseCase
                .deleteQuickOrder(it)
                .collect { result ->
                    when(result) {
                        is NetworkResult.Success -> {
                           when(result.data) {
                               "image" -> _uiUpdate.value = UiUpdate.DeletingImages(result.message, result.data.toString())
                               else -> _uiUpdate.value = UiUpdate.DeletingQuickOrder(result.message, result.data.toString())
                           }
                        }
                        is NetworkResult.Failed -> {
                           when(result.data) {
                               "image" -> _uiUpdate.value = UiUpdate.DeletingImages(result.message, "failed")
                               else -> _uiUpdate.value = UiUpdate.DeletingQuickOrder(result.message, "failed")
                           }
                        }
                    }
                }
        }
    }

    fun updateCartItem(position: Int, count: Int) {
        if (quickOrder?.orderPlaced == true) {
            _uiEvent.value = UIEvent.Toast("Can't change cart. Order placed already")
        } else {
            quickOrder?.let {
                it.cart[position].quantity = count
                /*
                * Here we are updating the checkout text with updated price for the changed cart value
                * We are checking if the updated cart price still holds to the coupon's minimum purchase limit values
                * If not we remove the coupon automatically and notify user
                * */
                appliedCoupon?.let { coupon ->
                    val cartPrice = getTotalCartPrice()
                    if (cartPrice >= coupon.purchaseLimit) {
                        couponAppliedPrice = cartPrice - couponDiscount(coupon, cartPrice)
                    } else {
                        _uiUpdate.value =
                            UiUpdate.CouponApplied(null)
                        _uiEvent.value =
                            UIEvent.Toast("Coupon Discount Removed. Total Cart Price is less than Coupon limit.", LONG)
                    }
                }
                _uiUpdate.value = UiUpdate.UpdateCartData(position, count)
            }
        }
    }

    fun deleteItemFromCart(position: Int) {
        if (quickOrder?.orderPlaced == true) {
            _uiEvent.value = UIEvent.Toast("Can't change cart. Order placed already")
        } else {
            quickOrder?.let {
                it.cart.removeAt(position)
                appliedCoupon?.let { coupon ->
                    val cartPrice = getTotalCartPrice()
                    if (cartPrice >= coupon.purchaseLimit) {
                        couponAppliedPrice = cartPrice - couponDiscount(coupon, cartPrice)
                    } else {
                        _uiUpdate.value =
                            UiUpdate.CouponApplied(null)
                        _uiEvent.value =
                            UIEvent.Toast("Coupon Discount Removed. Total Cart Price is less than Coupon limit.", LONG)
                    }
                }
                _uiUpdate.value = UiUpdate.UpdateCartData(position, null)
            }
        }
    }

    sealed class UiUpdate {
        data class WalletData(val wallet: Wallet): UiUpdate()

        //uploading List
        data class BeginningUpload(val message: String): UiUpdate()
        data class UploadingImage(val message: String): UiUpdate()
        data class UploadComplete(val message: String): UiUpdate()
        data class UploadFailed(val message: String): UiUpdate()

        //address
        data class AddressUpdate(val message: String, val address: Address?, val isSuccess: Boolean): UiUpdate()

        //estimateData
        data class EstimateData(val message: String, val data: QuickOrder?, val isSuccess: Boolean): UiUpdate()
        data class PopulateOrderDetails(val order: OrderEntity): UiUpdate()

        //coupon
        data class CouponApplied(val message: String?): UiUpdate()

        //order
        data class ValidatingPurchase(val message: String): UiUpdate()
        data class StartingTransaction(val message: String): UiUpdate()
        data class PlacingOrder(val message: String): UiUpdate()
        data class OrderPlaced(val message: String): UiUpdate()
        data class WalletTransactionFailed(val message: String): UiUpdate()
        data class OrderPlacementFailed(val message: String): UiUpdate()

        //update cart
        data class UpdateCartData(val position: Int, val count: Int?): UiUpdate()

        //deleting quick order
        data class DeletingImages(val message: String, val data: String?): UiUpdate()
        data class DeletingQuickOrder(val message: String, val data: String?): UiUpdate()

        object Empty: UiUpdate()
    }
}