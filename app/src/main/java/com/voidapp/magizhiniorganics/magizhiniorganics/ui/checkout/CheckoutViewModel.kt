package com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ListenableWorker
import com.google.firebase.firestore.ktx.firestoreSettings
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.QuickOrderUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CouponEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder.QuickOrderViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL_PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PURCHASE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.toCartEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.io.IOException

class CheckoutViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository,
    private val quickOrderUseCase: QuickOrderUseCase
): ViewModel() {

    var userProfile: UserProfileEntity? = null
    var wallet: Wallet? = null
    var currentCoupon: CouponEntity? = null

    var couponAppliedPrice: Float? = null

    var deliveryCharge: Float = 0f
    var checkedAddressPosition: Int = 0

    var addressPosition: Int = 0
    val totalCartItems: MutableList<CartEntity> = mutableListOf()

    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate
    private val _uiEvent: MutableLiveData<UIEvent> = MutableLiveData()
    val uiEvent: LiveData<UIEvent> = _uiEvent

    var navigateToPage: String = ALL_PRODUCTS

    var cwmDish: MutableList<CartEntity> = mutableListOf()
    var isCWMCart: Boolean = false

    private var _coupons: MutableLiveData<List<CouponEntity>> = MutableLiveData()
    val coupons: LiveData<List<CouponEntity>> = _coupons
    private var _profile: MutableLiveData<UserProfileEntity> = MutableLiveData()
    val profile: LiveData<UserProfileEntity> = _profile

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(NetworkResult.Empty)
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
                    _uiEvent.value = UIEvent.ProgressBar(true)
                    _uiUpdate.value = UiUpdate.PopulateAddressData(profile.address.map { it.copy() } as MutableList<Address>)
                }
                getWallet(profile.id)
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("checkout: getting profile from db", it) }
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

    private suspend fun updateAddress(id: String, address: ArrayList<Address>, status: String) {
        if(status == "add") {
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
                _uiUpdate.value = UiUpdate.AddressUpdate("add", 0, newAddress, true)
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.AddressUpdate(e.message.toString(), 0, null, false)
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
                        _uiUpdate.value = UiUpdate.AddressUpdate("update", addressPosition, address, true)
                    }
                }
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.AddressUpdate(e.message.toString(), 0,null, false)
            }
            e.message?.let {
                fbRepository.logCrash(
                    "checkout: update address to profile from db",
                    it
                )
            }
        }
    }

    fun deleteAddress(position: Int) = viewModelScope.launch (Dispatchers.IO){
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
                _uiUpdate.value = UiUpdate.AddressUpdate("delete", position,userProfile!!.address[position], true)
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.AddressUpdate(e.message.toString(), 0, null, false)
            }
            e.message?.let { fbRepository.logCrash("checkout: update address to profile from db", it) }
        }
    }

    fun getAllCartItem(cartItems: MutableList<CartEntity>) = viewModelScope.launch (Dispatchers.IO) {
        /*
        * we are making two different copies where one copy is passed initially and hard copy is made which is later used for recycler view
        * */
        if (isCWMCart) {
            withContext(Dispatchers.Main) {
                cwmDish.addAll(cartItems.map { it.copy() }) //making a hard copy to recyclerview update recycler view using diffUtil
                _uiUpdate.value = UiUpdate.PopulateCartData(cartItems)
            }
            return@launch
        }
        dbRepository.getAllCartItem()?.let { cartItems ->
            withContext(Dispatchers.Main) {
                totalCartItems.addAll(cartItems.map { it.copy() })
                _uiUpdate.value = UiUpdate.PopulateCartData(cartItems)
            }
        } ?: withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.PopulateCartData(null)
        }
    }

    fun deleteCartItem(id: Int, productId: String, variant: String, position: Int = 0) = viewModelScope.launch (Dispatchers.IO) {
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
                _uiUpdate.value = UiUpdate.UpdateCartData("delete", position, null)
            }
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("checkout: deleting cart item in db", it) }
        }
    }

    fun updateCartItem(id: Int, updatedCount: Int, position: Int = 0) = viewModelScope.launch (Dispatchers.IO) {
        try {
            if (isCWMCart) {
                cwmDish[position].quantity = updatedCount
            } else {
                dbRepository.updateCartItem(id, updatedCount)
                totalCartItems[position].quantity = updatedCount
            }
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.UpdateCartData("update", position, updatedCount)
            }
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("checkout: updating cart item in db", it) }
        }
    }

    private suspend fun updatingTheCartInProduct(productId: String, variant: String) {
        withContext (Dispatchers.IO) {
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
                e.message?.let { fbRepository.logCrash("checkout: updating the product in cart in db", it) }
            }
        }
    }

    //extracting the sum of cart items price sent from shopping activity and cartitems observer
    fun getCartPrice(cartItems: List<CartEntity>): Float {
        return cartItems.indices
            .asSequence()
            .map { (cartItems[it].price * cartItems[it].quantity) }
            .sum()
    }

    //extracting the sum of cart items price sent from shopping activity and cartitems observer
    fun getCartOriginalPrice(cartItems: List<CartEntity>): Float {
        return cartItems.indices
            .asSequence()
            .map { (cartItems[it].originalPrice * cartItems[it].quantity) }
            .sum()
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
                    product.inCart = false
                    product.variantInCart.clear()
                    dbRepository.upsertProduct(product)
                }
            }
            dbRepository.clearCart()
            true
        } catch (e: Exception) {
            _status.value = NetworkResult.Failed("orderPlaced", null)
            e.message?.let { fbRepository.logCrash("checkout: clearing cart from db", it) }
            false
        }
    }

    //Coupons
//    fun getAllCoupons(status: String) = viewModelScope.launch (Dispatchers.IO) {
//        try {
//            val coupons = dbRepository.getAllActiveCoupons(status)
//            withContext(Dispatchers.Main) {
//                _coupons.value = coupons
//            }
//        } catch (e: Exception) {
//            e.message?.let { fbRepository.logCrash("checkout: getting all active coupons from db", it) }
//            _status.value = NetworkResult.Failed("toast", "Failed to fetch coupon details")
//        }
//    }

    fun verifyCoupon(couponCode: String, cartItems: List<CartEntity>) = viewModelScope.launch(Dispatchers.IO) {
        if (couponCode == "") {
            return@launch
        } else {
            val code: CouponEntity? = currentCoupon?.let{
                currentCoupon
            } ?: dbRepository.getCouponByCode(couponCode)
            code?.let { coupon ->
                val cartPrice = getCartPrice(cartItems)
                if (!coupon.categories.contains(Constants.ALL)) {
                    withContext(Dispatchers.Main) {
                        _uiEvent.value = UIEvent.Toast("Coupon Applies only for few product categories")
                    }
                    return@launch
                }
                if (cartPrice > coupon.purchaseLimit) {
                    if (couponAppliedPrice == null) {
                        withContext(Dispatchers.Main) {
                            _uiUpdate.value = UiUpdate.CouponApplied(
                                "Coupon Applied Successfully!"
                            )
                        }
                    }
                    currentCoupon = coupon
                    couponAppliedPrice = couponDiscount(coupon, cartPrice)
//                    couponAppliedPrice = cartPrice - couponDiscount(coupon, cartPrice)
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

    suspend fun getDeliveryCharge(): Float = withContext(Dispatchers.IO){
        return@withContext userProfile?.let {
            dbRepository.getDeliveryCharge(it.address[checkedAddressPosition].LocationCode).deliveryCharge.toFloat()
        } ?: 30f
    }

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

    fun limitedItemsUpdater(cartEntity: List<CartEntity>) = viewModelScope.launch {
        try {
            val limitedCartItems = mutableListOf<CartEntity>()
            for (cartItem in cartEntity) {
                withContext(Dispatchers.IO) {
                    val entity = dbRepository.getProductWithIdForUpdate(cartItem.productId)
                    entity?.let { product ->
                        if (product.variants[cartItem.variantIndex].status == Constants.LIMITED) {
                            limitedCartItems.add(cartItem)
                        }
                    }
                }
            }
            _status.value = fbRepository.limitedItemsUpdater(limitedCartItems)
        } catch (e: Exception) {
            e.message?.let {
                fbRepository.logCrash("checkout: getting Items from db for limited item validation",
                    it
                )
            }
            NetworkResult.Failed("ootItems", "Failed to validated Purchases. Please try again later")
        }
    }


    fun proceedForWalletPayment(
        orderDetailsMap: HashMap<String, Any>
    ) {
        viewModelScope.launch {
            val cartItems = if (isCWMCart) {
                cwmDish
            } else {
                totalCartItems
            }
            val mrp = getCartPrice(cartItems) + getDeliveryCharge() - (couponAppliedPrice ?: 0f)
            orderDetailsMap["orderID"] = generateOrderID()
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
                            cart = cartItems as ArrayList<CartEntity>
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
            val cartItems = if (isCWMCart) {
                cwmDish
            } else {
                totalCartItems
            }
            val mrp = getCartPrice(cartItems) + getDeliveryCharge() - (couponAppliedPrice ?: 0f)
            orderDetailsMap["orderID"] = generateOrderID()
            quickOrderUseCase
                .placeCashOnDeliveryOrder(
                    orderDetailsMap,
                    cartItems as ArrayList<CartEntity>,
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
        val cartItems = if (isCWMCart) {
            cwmDish
        } else {
            totalCartItems
        }
        val mrp = getCartPrice(cartItems) + getDeliveryCharge() - (couponAppliedPrice ?: 0f)
        quickOrderUseCase
            .placeOnlinePaymentOrder(
                orderDetailsMap,
                mrp,
                PURCHASE,
                "Product Purchase Online Transaction",
                cartItems as ArrayList<CartEntity>
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


    fun updateTransaction(transaction: TransactionHistory) = viewModelScope.launch (Dispatchers.IO) {
        _status.value = fbRepository.updateTransaction(transaction)
    }

    suspend fun generateOrderID(): String = withContext(Dispatchers.IO) {
        return@withContext fbRepository.generateOrderID()
    }

    sealed class UiUpdate {
        //address
        data class PopulateAddressData(val addressList: MutableList<Address>): UiUpdate()
        data class AddressUpdate(val message: String, val position: Int, val address: Address?, val isSuccess: Boolean): UiUpdate()
        //coupon
        data class CouponApplied(val message: String): UiUpdate()
        //wallet
        data class WalletData(val wallet: Wallet): UiUpdate()
        //cwm
        data class UpdateCartData(val message: String, val position: Int, val count: Int?): UiUpdate()
        //cart
        data class PopulateCartData(val cartItems: List<CartEntity>?): UiUpdate()
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