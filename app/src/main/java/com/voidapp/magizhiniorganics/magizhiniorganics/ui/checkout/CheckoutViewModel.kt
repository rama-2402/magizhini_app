package com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ListenableWorker
import com.google.firebase.firestore.ktx.firestoreSettings
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CouponEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL_PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PURCHASE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException

class CheckoutViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {

    var navigateToPage: String = ALL_PRODUCTS
    var userId: String = ""
    var itemsInCart: List<CartEntity> = listOf()
    var addressPosition = 0
    private var localProfile = UserProfileEntity()

    private var _coupons: MutableLiveData<List<CouponEntity>> = MutableLiveData()
    val coupons: LiveData<List<CouponEntity>> = _coupons
    private var _couponIndex: MutableLiveData<Int> = MutableLiveData()
    val couponIndex: LiveData<Int> = _couponIndex
    private var _profile: MutableLiveData<UserProfileEntity> = MutableLiveData()
    val profile: LiveData<UserProfileEntity> = _profile

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    fun setEmptyStatus() {
        _status.value = NetworkResult.Empty
    }

    fun getUserProfileData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val profile = dbRepository.getProfileData()!!
            withContext(Dispatchers.Main) {
                localProfile = profile
                _profile.value = profile
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("checkout: getting profile from db", it) }
        }
    }

    private suspend fun updateAddress(id: String, address: ArrayList<Address>, status: String) {
        if(status == "add") {
            fbRepository.addAddress(id, address[0])
        } else {
            fbRepository.updateAddress(id, address)
        }
    }

    fun addAddress(id: String, newAddress: Address) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val list = arrayListOf<Address>(newAddress)

            val localUpdate = async {
                localProfile.address.add(newAddress)
                dbRepository.upsertProfile(localProfile)
                withContext(Dispatchers.Main) {
                    _profile.value = localProfile
                }
            }

            val cloudUpdate = async { updateAddress(id, list,"add") }

            localUpdate.await()
            cloudUpdate.await()
            _status.value = NetworkResult.Success("toast", "Address added")
        } catch (e: IOException) {
            _status.value = NetworkResult.Success("toast", "Failed to add address. Try later")
            e.message?.let { fbRepository.logCrash("checkout: add address to profile from db", it) }
        }
    }

    fun updateAddress(address: Address) = viewModelScope.launch (Dispatchers.IO){
        try {
            val localUpdate = async {
                with(localProfile.address[addressPosition]) {
                    userId = address.userId
                    addressLineOne = address.addressLineOne
                    addressLineTwo = address.addressLineTwo
                    LocationCode = address.LocationCode
                    LocationCodePosition = address.LocationCodePosition
                }
                dbRepository.upsertProfile(localProfile)
                withContext(Dispatchers.Main) {
                    _profile.value = localProfile
                }
            }
            val cloudUpdate = async { updateAddress(localProfile.id, localProfile.address,"update") }

            localUpdate.await()
            cloudUpdate.await()
            _status.value = NetworkResult.Success("toast", "Address updated")
        } catch (e: IOException) {
            _status.value = NetworkResult.Success("toast", "Failed to update address. Try later")
            e.message?.let { fbRepository.logCrash("checkout: update address to profile from db", it) }
        }
    }

    fun deleteAddress(position: Int) = viewModelScope.launch (Dispatchers.IO){
        try {
            val localUpdate = async {
                localProfile.address.removeAt(position)
                dbRepository.upsertProfile(localProfile)
                withContext(Dispatchers.Main) {
                    _profile.value = localProfile
                }
            }
            val cloudUpdate = async { updateAddress(localProfile.id, localProfile.address,"update") }

            localUpdate.await()
            cloudUpdate.await()
            _status.value = NetworkResult.Success("toast", "Address Deleted")
        } catch (e: IOException) {
            _status.value = NetworkResult.Success("toast", "Failed to delete address. Try later")
            e.message?.let { fbRepository.logCrash("checkout: update address to profile from db", it) }
        }
    }

    suspend fun getDeliveryChargeForTheLocation(areacode: String) = dbRepository.getDeliveryCharge(areacode)

    //cart functions
    fun getAllCartItems() = dbRepository.getAllCartItems()

    fun deleteCartItem(id: Int, productId: String, variant: String) = viewModelScope.launch (Dispatchers.IO) {
        try {
            dbRepository.deleteCartItem(id)
            updatingTheCartInProduct(productId, variant)
            getAllCartItems()
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("checkout: deleting cart item in db", it) }
        }
    }

    fun updateCartItem(id: Int, updatedCount: Int) = viewModelScope.launch (Dispatchers.IO) {
        try {
            dbRepository.updateCartItem(id, updatedCount)
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("checkout: updating cart item in db", it) }
        }
    }

    private fun updatingTheCartInProduct(productId: String, variant: String) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val productEntity = dbRepository.getProductWithIdForUpdate(productId)
            productEntity.variantInCart.remove(variant)
            if (productEntity.variantInCart.isEmpty()) {
                productEntity.inCart = false
            }
            dbRepository.upsertProduct(productEntity)
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("checkout: updating the product in cart in db", it) }
        }
    }

    //extracting the sum of cart items price sent from shopping activity and cartitems observer
    fun getCartPrice(mCartItems: List<CartEntity>): Float {
        return mCartItems.indices
            .asSequence()
            .map { (mCartItems[it].price * mCartItems[it].quantity) }
            .sum()
    }

    //extracting the sum of cart items price sent from shopping activity and cartitems observer
    fun getCartOriginalPrice(mCartItems: List<CartEntity>): Float {
        return mCartItems.indices
            .asSequence()
            .map { (mCartItems[it].originalPrice * mCartItems[it].quantity) }
            .sum()
    }

    //extracting the sum of cart items quantity sent from shopping activity and cartitems observer
    fun getCartItemsQuantity(mCartItems: List<CartEntity>): Int {
       var quantities = 0
        mCartItems.forEach {
            quantities += it.quantity
        }
        return quantities
    }

    fun clearCart(cartItems: List<CartEntity>) = viewModelScope.launch (Dispatchers.IO) {
        try {
            for (cartItem in cartItems) {
                val product = dbRepository.getProductWithIdForUpdate(cartItem.productId)
                product.inCart = false
                product.variantInCart.clear()
                dbRepository.upsertProduct(product)
            }
            dbRepository.clearCart()
            _status.value = NetworkResult.Success("orderPlaced", null)
        } catch (e: Exception) {
            _status.value = NetworkResult.Failed("orderPlaced", null)
            e.message?.let { fbRepository.logCrash("checkout: clearing cart from db", it) }
        }
    }

    //Coupons
    fun getAllCoupons(status: String) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val coupons = dbRepository.getAllActiveCoupons(status)
            withContext(Dispatchers.Main) {
                _coupons.value = coupons
            }
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("checkout: getting all active coupons from db", it) }
            _status.value = NetworkResult.Failed("toast", "Failed to fetch coupon details")
        }
    }

    fun isCouponAvailable(mCoupons: List<CouponEntity>, couponCode: String): Boolean {
        var isAvailable: Boolean = false
        for (i in mCoupons.indices) {
            if (
                mCoupons[i].code == couponCode &&
                mCoupons[i].categories.contains(Constants.ALL)
            ) {
                isAvailable = true
                _couponIndex.value = i
                break
            }
        }
        return isAvailable
    }

    fun placeOrder(order: Order) = viewModelScope.launch(Dispatchers.IO) {
        if (order.paymentMethod == "Online") {
            GlobalTransaction(
                id = "",
                userID = localProfile.id,
                userName = localProfile.name,
                userMobileNumber = localProfile.phNumber,
                transactionID = order.transactionID,
                transactionType = "Online Payment",
                transactionAmount = order.price,
                transactionDirection = PURCHASE,
                timestamp = System.currentTimeMillis(),
                transactionReason = "Product Purchase Online Transaction"
            ).let {
                fbRepository.createGlobalTransactionEntry(it)
            }
        }
        _status.value = fbRepository.placeOrder(order)
    }

    fun limitedItemsUpdater(cartEntity: List<CartEntity>) = viewModelScope.launch {
        try {
            val limitedCartItems = mutableListOf<CartEntity>()
            for (cartItem in cartEntity) {
                withContext(Dispatchers.IO) {
                    val product = dbRepository.getProductWithIdForUpdate(cartItem.productId)
                    if (product.variants[cartItem.variantIndex].status == Constants.LIMITED) {
                        limitedCartItems.add(cartItem)
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

    fun getWallet(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _status.value = fbRepository.getWallet(id)
    }

    suspend fun validateItemAvailability(cartItems: List<CartEntity>) {
        _status.value = NetworkResult.Loading("Validating Purchase...", "limited")
        _status.value = fbRepository.validateItemAvailability(cartItems)
    }

    suspend fun makeTransactionFromWallet(amount: Float, id: String, orderID: String) {
        if (fbRepository.makeTransactionFromWallet(amount, id, "Remove")) {
            TransactionHistory (
                orderID,
                System.currentTimeMillis(),
                TimeUtil().getMonth(),
                TimeUtil().getYear().toLong(),
                amount,
                id,
                id,
                Constants.SUCCESS,
                Constants.PURCHASE,
                orderID
            ).let {
              _status.value = NetworkResult.Success("transaction", it)
            }
        } else {
            _status.value = NetworkResult.Failed("transaction", "Server Error. Failed to make transaction from Wallet. Try other payment method")
        }
    }

    fun updateTransaction(transaction: TransactionHistory) = viewModelScope.launch (Dispatchers.IO) {
        _status.value = fbRepository.updateTransaction(transaction)
    }

    suspend fun generateOrderID(): String = withContext(Dispatchers.IO) {
        return@withContext fbRepository.generateOrderID()
    }
}