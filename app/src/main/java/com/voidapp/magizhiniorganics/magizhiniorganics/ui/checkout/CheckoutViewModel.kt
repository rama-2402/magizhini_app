package com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout

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
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PURCHASE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Utils
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.toUserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class CheckoutViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository,
    private val quickOrderUseCase: QuickOrderUseCase
) : ViewModel() {

    var userProfile: UserProfileEntity? = null
    var wallet: Wallet? = null
    var tempAddress: Address? = null

    var freeDeliveryLimit: Float? = null
    var deliveryAvailable: Boolean = true

    val totalCartItems: MutableList<CartEntity> = mutableListOf()
    val clearedProductIDs: MutableList<String> = mutableListOf()

    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate
    private val _uiEvent: MutableLiveData<UIEvent> = MutableLiveData()
    val uiEvent: LiveData<UIEvent> = _uiEvent

    var cwmDish: MutableList<CartEntity> = mutableListOf()
    var isCWMCart: Boolean = false

    var gstAmount: Float = 0f

    var couponPrice: Double? = null
    var currentCoupon: CouponEntity? = null

    private val _status: MutableStateFlow<NetworkResult> =
        MutableStateFlow<NetworkResult>(NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    fun setEmptyUiEvent() {
        _uiEvent.value = UIEvent.EmptyUIEvent
    }

    fun setEmptyStatus() {
        _uiUpdate.value = UiUpdate.Empty
    }

    fun getUserProfileData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            dbRepository.getProfileData()?.let { profile ->
                withContext(Dispatchers.Main) {
                    userProfile = profile
                    _uiUpdate.value =
                        UiUpdate.PopulateAddressData(profile.address.map { it.copy() } as MutableList<Address>)
                }
                getWallet(profile.id)
            } ?: let {
                withContext(Dispatchers.Main) {
                    _uiUpdate.value = UiUpdate.NoProfileFound
                }
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.PlaceOrderWithWhatsapp("Failed to fetch user profile.")
            }
            e.message?.let { fbRepository.logCrash("checkout: getting profile from db", it) }
        }
    }

    private fun getWallet(userID: String) = viewModelScope.launch {
        when (val result = fbRepository.getWallet(userID)) {
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
                        _uiUpdate.value = UiUpdate.AddressUpdate("update", 0, address, true)
                    }
                }
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.AddressUpdate(e.message.toString(), 0, null, false)
            }
            e.message?.let {
                fbRepository.logCrash(
                    "checkout: update address to profile from db",
                    it
                )
            }
        }
    }

    fun getAllCartItem(cartItems: MutableList<CartEntity>) = viewModelScope.launch(Dispatchers.IO) {
        /*
        * we are making two different copies where one copy is passed initially and hard copy is made which is later used for recycler view
        * */
        try {
         if (isCWMCart) {
            withContext(Dispatchers.Main) {
                cwmDish.addAll(cartItems.map { it.copy() }) //making a hard copy to recyclerview update recycler view using diffUtil
                _uiUpdate.value = UiUpdate.PopulateCartData(cartItems)
            }
            return@launch
        }
        dbRepository.getAllCartItem()?.let { cart ->
            withContext(Dispatchers.Main) {
                totalCartItems.addAll(cart.map { it.copy() })
                _uiUpdate.value = UiUpdate.PopulateCartData(cart)
            }
        } ?: withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.PopulateCartData(null)
        }
        } catch (e: IOException) {
             withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.PlaceOrderWithWhatsapp("Failed to fetch the items in your cart.")
        }
        }
   }

    fun deleteCartItem(id: Int, productId: String, variant: String, position: Int = 0) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isCWMCart) {
                    cwmDish.removeAt(position)
                } else {
                    totalCartItems.removeAt(position)
                    val delete = async { dbRepository.deleteCartItem(id) }
                    val updateProduct = async { updatingTheCartInProduct(productId, variant) }
                    delete.await()
                    updateProduct.await()
                }
                withContext(Dispatchers.Main) {
                    clearedProductIDs.add(productId)
                    _uiUpdate.value = UiUpdate.UpdateCartData("delete", position, null)
                }
            } catch (e: Exception) {
                e.message?.let { fbRepository.logCrash("checkout: deleting cart item in db", it) }
            }
        }

    fun updateCartItem(id: Int, updatedCount: Int, position: Int = 0) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isCWMCart) {
                    cwmDish[position].quantity = updatedCount
                } else {
                    dbRepository.updateCartItem(id, updatedCount)
                }
                totalCartItems[position].quantity = updatedCount
                withContext(Dispatchers.Main) {
//                    totalCartItems[position].quantity = updatedCount
                    _uiUpdate.value = UiUpdate.UpdateCartData("update", position, updatedCount)
                }
            } catch (e: Exception) {
                e.message?.let { fbRepository.logCrash("checkout: updating cart item in db", it) }
            }
        }

    private suspend fun updatingTheCartInProduct(productId: String, variant: String) {
        withContext(Dispatchers.IO) {
            try {
                val entity = dbRepository.getProductWithIdForUpdate(productId)
                entity?.let { productEntity ->
                    productEntity.variantInCart.remove(variant)
                    if (productEntity.variantInCart.isEmpty()) {
                        productEntity.inCart = false
                    }
                    dbRepository.upsertProduct(productEntity)
                }
            } catch (e: Exception) {
                e.message?.let {
                    fbRepository.logCrash(
                        "checkout: updating the product in cart in db",
                        it
                    )
                }
            }
        }
    }

    //extracting the sum of cart items price sent from shopping activity and cartitems observer
    suspend fun getCartPrice(cartItems: List<CartEntity>): MutableList<Float> {
        var price: Float = 0f
        var originalPrice: Float = 0f
        val priceList: MutableList<Float> = mutableListOf<Float>()
        gstAmount = 0f

        for (item in cartItems) {
            gstAmount += ((item.price * item.couponName.toInt() / 100) * item.quantity)
            gstAmount = Utils.roundPrice(gstAmount)
            price += item.price * item.quantity
            originalPrice += item.originalPrice * item.quantity
        }

        priceList.add(price)
        priceList.add(originalPrice)

        return priceList
//        return cartItems.indices
//            .asSequence()
//            .map { (cartItems[it].price * cartItems[it].quantity) }
//            .sum()
    }

    //extracting the sum of cart items price sent from shopping activity and cartitems observer
    suspend fun getCartOriginalPrice(cartItems: List<CartEntity>): Float = withContext(Dispatchers.Default) {
         var price: Float = 0f

        for (item in cartItems) {
            price += item.originalPrice * item.quantity
        }

        return@withContext price
//        return@withContext cartItems.indices
//            .asSequence()
//            .map { (cartItems[it].originalPrice * cartItems[it].quantity) }
//            .sum()
    }

    //extracting the sum of cart items quantity sent from shopping activity and cartitems observer
    fun getCartItemsQuantity(cartItems: List<CartEntity>): Int {
        var quantities = 0
        cartItems.forEach {
            quantities += it.quantity
        }
        return quantities
    }

    suspend fun clearCart(cartItems: List<CartEntity>): Boolean = withContext(Dispatchers.IO) {
        try {
            for (cartItem in cartItems) {
                val entity = dbRepository.getProductWithIdForUpdate(cartItem.productId)
                entity?.let { product ->
                    clearedProductIDs.add(product.id)
                    product.inCart = false
                    product.variantInCart.clear()
                    dbRepository.upsertProduct(product)
                }
            }
            cwmDish.clear()
            totalCartItems.clear()
            dbRepository.clearCart()
            withContext(Dispatchers.Main) {
                _uiEvent.value = UIEvent.Toast("Cart Items Emptied")
                _uiUpdate.value = UiUpdate.CartCleared(null)
            }
            true
        } catch (e: Exception) {
            _status.value = NetworkResult.Failed("orderPlaced", null)
            e.message?.let { fbRepository.logCrash("checkout: clearing cart from db", it) }
            false
        }
    }

    //Coupons
    fun verifyCoupon(couponCode: String, cartItems: List<CartEntity>) =
        viewModelScope.launch(Dispatchers.IO) {
            if (couponCode == "") {
                return@launch
            } else {
                val code: CouponEntity? = currentCoupon?.let {
                    it
                } ?: dbRepository.getCouponByCode(couponCode)
                code?.let { coupon ->
                    val cartPrice = getCartPrice(cartItems)
                    if (!coupon.categories.contains(Constants.ALL)) {
                        withContext(Dispatchers.Main) {
                            _uiEvent.value =
                                UIEvent.Toast("Coupon Applies only for few product categories")
                        }
                        return@launch
                    }
                    if (cartPrice[1] >= coupon.purchaseLimit) {
//                        if (couponPrice == null) {
//                            withContext(Dispatchers.Main) {
//
//                            }
//                        }
                        withContext(Dispatchers.Main) {
                            currentCoupon = coupon
                            couponPrice = couponDiscount(coupon, cartPrice[1].toDouble())
                            _uiUpdate.value = UiUpdate.CouponApplied(
                                    "Coupon Applied Successfully!"
                                )
                        }
//                    couponAppliedPrice = cartPrice - couponDiscount(coupon, cartPrice)
                    } else {
                        withContext(Dispatchers.Main) {
                            _uiEvent.value =
                                UIEvent.Toast("Coupon Applies only for Purchase more than Rs: ${coupon.purchaseLimit}")
                            _uiUpdate.value = UiUpdate.CouponApplied("")
                        }
                        return@launch
                    }
                } ?: withContext(Dispatchers.Main) {
                    _uiEvent.value = UIEvent.Toast("Coupon Code does not exist.")
                    _uiUpdate.value = UiUpdate.CouponApplied("")
                }
            }
        }

    fun couponDiscount(coupon: CouponEntity, cartPrice: Double): Double {
        var discountPrice: Double = when (coupon.type) {
            "percent" -> (cartPrice * coupon.amount / 100)
            "rupees" -> coupon.amount.toDouble()
            else -> 0.0
        }

        if (discountPrice > coupon.maxDiscount) {
            discountPrice = coupon.maxDiscount.toDouble()
        }

        return discountPrice
    }

    suspend fun getFreeDeliveryLimit(): Float {
        return  try {
         if (freeDeliveryLimit == null) {
            freeDeliveryLimit = fbRepository.getFreeDeliveryLimit() ?: 2000f
        }
        freeDeliveryLimit!!
        } catch (E: Exception) {
            2000f
        }
   }

    suspend fun getDeliveryCharge(): Float = withContext(Dispatchers.IO) {
        return@withContext try {
             userProfile?.let {
            dbRepository.getDeliveryCharge(it.address[0].LocationCode)?.let { pinCodes ->
                if (pinCodes.isEmpty()) {
//                        deliveryAvailability(null)
                    30f
                } else {
//                        deliveryAvailability(pinCodes[0])
                    pinCodes[0].deliveryCharge.toFloat()
                }
            } ?: let {
//                    deliveryAvailability(null)
                30f
            }
        } ?: let {
            tempAddress?.let {
                dbRepository.getDeliveryCharge(it.LocationCode)?.let { pinCodes ->
                    if (pinCodes.isEmpty()) {
                        //                        deliveryAvailability(null)
                        30f
                    } else {
                        //                        deliveryAvailability(pinCodes[0])
                        pinCodes[0].deliveryCharge.toFloat()
                    }
                } ?: let {
                    //                    deliveryAvailability(null)
                    30f
                }
            }
        } ?: 30f
        } catch (E: IOException) {
            30f
        }
    }

//    private suspend fun deliveryAvailability(pinCodesEntity: PinCodesEntity?) = withContext(Dispatchers.Main){
//        pinCodesEntity?.let {
//            if(!pinCodesEntity.deliveryAvailable) {
//                if (deliveryAvailable) {
//                    _deliveryNotAvailableDialog.value = System.currentTimeMillis()
//                }
//                deliveryAvailable = false
//            } else {
//                deliveryAvailable = true
//            }
//        } ?:let {
//            if (deliveryAvailable) {
//                _deliveryNotAvailableDialog.value = System.currentTimeMillis()
//            }
//            deliveryAvailable = false
//        }
//    }

//    fun placeOrder(order: Order) = viewModelScope.launch(Dispatchers.IO) {
//        if (order.paymentMethod == "Online") {
//            GlobalTransaction(
//                id = "",
//                userID = localProfile.id,
//                userName = localProfile.name,
//                userMobileNumber = localProfile.phNumber,
//                transactionID = order.transactionID,
//                transactionType = "Online Payment",
//                transactionAmount = order.price,
//                transactionDirection = PURCHASE,
//                timestamp = System.currentTimeMillis(),
//                transactionReason = "Product Purchase Online Transaction"
//            ).let {
//                fbRepository.createGlobalTransactionEntry(it)
//            }
//        }
//        _status.value = fbRepository.placeOrder(order)
//    }

//    fun limitedItemsUpdater(cartEntity: List<CartEntity>) = viewModelScope.launch {
//        try {
//            val limitedCartItems = mutableListOf<CartEntity>()
//            for (cartItem in cartEntity) {
//                withContext(Dispatchers.IO) {
//                    val entity = dbRepository.getProductWithIdForUpdate(cartItem.productId)
//                    entity?.let { product ->
//                        if (product.variants[cartItem.variantIndex].status == Constants.LIMITED) {
//                            limitedCartItems.add(cartItem)
//                        }
//                    }
//                }
//            }
//            _status.value = fbRepository.limitedItemsUpdater(limitedCartItems)
//        } catch (e: Exception) {
//            e.message?.let {
//                fbRepository.logCrash("checkout: getting Items from db for limited item validation",
//                    it
//                )
//            }
//            NetworkResult.Failed("ootItems", "Failed to validated Purchases. Please try again later")
//        }
//    }

fun proceedForWalletPayment(
    orderDetailsMap: HashMap<String, Any>
) {
    viewModelScope.launch {
        val cartItems = if (isCWMCart) {
            cwmDish
        } else {
            totalCartItems
        }
        val mrp = orderDetailsMap["mrp"].toString().toFloat()
        orderDetailsMap["orderID"] = generateOrderID()
        orderDetailsMap["referral"] = addReferralBonusStatus()
        wallet?.let {
            if (mrp > it.amount) {
                _uiEvent.value =
                    UIEvent.SnackBar(
                        "Insufficient Wallet Balance. Please choose any other payment method",
                        true
                    )
                return@launch
            } else {
                quickOrderUseCase
                    .initiateWalletTransaction(
                        orderDetailsMap,
                        mrp,
                        PURCHASE,
                        cart = cartItems as ArrayList<CartEntity>,
                        false
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
                                        "order" -> _uiUpdate.value =
                                            UiUpdate.PlacingOrder("Placing your Order...")
                                        "success" -> _uiUpdate.value =
                                            UiUpdate.OrderPlaced("Order Placed Successfully...!")
                                    }
                                }
                                is NetworkResult.Failed -> {
                                    when (result.message) {
                                        "wallet" -> _uiUpdate.value =
                                            UiUpdate.WalletTransactionFailed(
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

private fun addReferralBonusStatus(): String {
    return userProfile?.let {
        if (it.extras[0] == "yes") {
            it.referralId.toString()
        } else {
            ""
        }
    } ?: ""
}

fun placeCashOnDeliveryOrder(
    orderDetailsMap: HashMap<String, Any>
) {
    viewModelScope.launch {
        val cartItems = if (isCWMCart) {
            cwmDish
        } else {
            totalCartItems
        }
        val mrp = orderDetailsMap["mrp"].toString().toFloat()
        orderDetailsMap["orderID"] = generateOrderID()
        orderDetailsMap["referral"] = addReferralBonusStatus()
        quickOrderUseCase
            .placeCashOnDeliveryOrder(
                orderDetailsMap,
                cartItems as ArrayList<CartEntity>,
                mrp,
                false
            ).onEach { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        if (result.message == "placing") {
                            _uiUpdate.value = UiUpdate.PlacingOrder("Placing your Order...")
                        } else {
                            _uiUpdate.value = UiUpdate.OrderPlaced("Order Placed Successfully...!")
                        }
                    }
                    is NetworkResult.Failed -> {
                        _uiUpdate.value =
                            UiUpdate.OrderPlacementFailed("Server Error! failed to place order. Try again")
                    }
                    else -> Unit
                }
            }.launchIn(this)
    }
}

fun placeOrderWithOnlinePayment(orderDetailsMap: HashMap<String, Any>) = viewModelScope.launch {
    val cartItems = if (isCWMCart) {
        cwmDish
    } else {
        totalCartItems
    }
    val mrp = orderDetailsMap["mrp"].toString().toFloat()
    orderDetailsMap["orderID"] = generateOrderID()
    orderDetailsMap["referral"] = addReferralBonusStatus()
    quickOrderUseCase
        .placeOnlinePaymentOrder(
            orderDetailsMap,
            mrp,
            PURCHASE,
            "Product Purchase Online Transaction",
            cartItems as ArrayList<CartEntity>,
            false
        ).collect { result ->
            when (result) {
                is NetworkResult.Success -> {
                    when (result.message) {
                        "validation" -> _uiUpdate.value = UiUpdate.ValidatingPurchase("")
                        "placing" -> _uiUpdate.value = UiUpdate.PlacingOrder("Placing Order...")
                        "placed" -> _uiUpdate.value =
                            UiUpdate.OrderPlaced("Order Placed Successfully...!")
                    }
                }
                is NetworkResult.Failed -> {
                    _uiUpdate.value = UiUpdate.OrderPlacementFailed("")
                }
                else -> Unit
            }
        }
}

private fun generateOrderID(): String {
    return userProfile?.let {
        TimeUtil().getOrderIDFormat(it.phNumber.takeLast(4))
    } ?: TimeUtil().getOrderIDFormat("${TimeUtil().getMonthNumber()}${TimeUtil().getDateNumber(0L)}")
}

fun updateReferralStatus() = viewModelScope.launch(Dispatchers.IO) {
    userProfile?.let {
        if (it.referralId != "" && it.extras[0] == "yes") {
            it.extras[0] = "no"
            fbRepository.uploadProfile(it.toUserProfile())
        }
    }
}

fun getHowToVideo(where: String) = viewModelScope.launch {
    val url = fbRepository.getHowToVideo(where)
    _uiUpdate.value = UiUpdate.HowToVideo(url)
}

sealed class UiUpdate {
    //address
    data class PopulateAddressData(val addressList: MutableList<Address>) : UiUpdate()
    data class AddressUpdate(
        val message: String,
        val position: Int,
        val address: Address?,
        val isSuccess: Boolean
    ) : UiUpdate()

    object NoProfileFound : UiUpdate()
    data class PlaceOrderWithWhatsapp(val msg: String? = null): UiUpdate()

    //coupon
    data class CouponApplied(val message: String) : UiUpdate()

    //wallet
    data class WalletData(val wallet: Wallet) : UiUpdate()

    //cwm
    data class UpdateCartData(val message: String, val position: Int, val count: Int?) : UiUpdate()

    //cart
    data class PopulateCartData(val cartItems: List<CartEntity>?) : UiUpdate()
    data class CartCleared(val message: String?) : UiUpdate()

    //order
    data class ValidatingPurchase(val message: String) : UiUpdate()
    data class StartingTransaction(val message: String) : UiUpdate()
    data class PlacingOrder(val message: String) : UiUpdate()
    data class OrderPlaced(val message: String) : UiUpdate()
    data class WalletTransactionFailed(val message: String) : UiUpdate()
    data class OrderPlacementFailed(val message: String) : UiUpdate()

    //how to
    data class HowToVideo(val url: String) : UiUpdate()

    object Empty : UiUpdate()
}

}
