package com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CouponEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.coroutines.*

class CheckoutViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {

    var userId: String = ""
    var itemsInCart: List<CartEntity> = listOf()

    private var _addressList: MutableLiveData<ArrayList<Address>> = MutableLiveData()
    val addressList: LiveData<ArrayList<Address>> = _addressList
    private var _address: MutableLiveData<Address> = MutableLiveData()
    val address: LiveData<Address> = _address
    private var _coupons: MutableLiveData<List<CouponEntity>> = MutableLiveData()
    val coupons: LiveData<List<CouponEntity>> = _coupons
    private var _couponIndex: MutableLiveData<Int> = MutableLiveData()
    val couponIndex: LiveData<Int> = _couponIndex
    private var _selectedAddress: MutableLiveData<Address> = MutableLiveData()
    val selectedAddress: LiveData<Address> = _selectedAddress
    private var _orderPlacementFailed: MutableLiveData<String> = MutableLiveData()
    val orderPlacementFailed: LiveData<String> = _orderPlacementFailed
    private var _selectedAddressPosition: MutableLiveData<Int> = MutableLiveData()
    var selectedAddressPosition: LiveData<Int> = _selectedAddressPosition
    private var addressPosition = 0
    private var _limitedItemUpdateStatus: MutableLiveData<String> = MutableLiveData()
    var limitedItemUpdateStatus: LiveData<String> = _limitedItemUpdateStatus
    private var _orderCompleted: MutableLiveData<Boolean> = MutableLiveData()
    var orderCompleted: LiveData<Boolean> = _orderCompleted

    fun getAddress() = viewModelScope.launch (Dispatchers.IO) {
        val address = dbRepository.getProfileData()!!.address
        withContext(Dispatchers.Main) {
            _addressList.value = address
        }
    }

    fun addAddress(id: String, newAddress: Address) = viewModelScope.launch (Dispatchers.IO) {
        val profile = dbRepository.getProfileData()
        profile!!.address.add(newAddress)
        dbRepository.upsertProfile(profile)
        getAddress()
        fbRepository.addAddress(id, newAddress)
    }

    fun editAddress(address: Address, position: Int) {
        addressPosition = position
        _address.value = address
    }

    fun updateAddress(address: Address) = viewModelScope.launch (Dispatchers.IO){
        val profile = dbRepository.getProfileData()!!
        with(profile.address[addressPosition]) {
            userId = address.userId
            addressLineOne = address.addressLineOne
            addressLineTwo = address.addressLineTwo
            LocationCode = address.LocationCode
            LocationCodePosition = address.LocationCodePosition
        }
        dbRepository.upsertProfile(profile)
        getAddress()
        fbRepository.updateAddress(profile.id, profile.address)
    }

    fun deleteAddress(id: String, position: Int) = viewModelScope.launch (Dispatchers.IO){
        val profile = dbRepository.getProfileData()!!
        profile.address.removeAt(position)
        dbRepository.upsertProfile(profile)
        getAddress()
        fbRepository.updateAddress(id, profile.address)
    }

    fun selectedAddress(address: Address, position: Int) {
        _selectedAddress.value = address
        _selectedAddressPosition.value = position
    }

    suspend fun getDeliveryChargeForTheLocation(areacode: String) = dbRepository.getDeliveryCharge(areacode)

    //cart functions
    fun getAllCartItems() = dbRepository.getAllCartItems()

    fun deleteCartItem(id: Int, productId: String, variant: String) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.deleteCartItem(id)
        getAllCartItems()
        updatingTheCartInProduct(productId, variant)
    }

    fun updateCartItem(id: Int, updatedCount: Int) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.updateCartItem(id, updatedCount)
    }

    private fun updatingTheCartInProduct(productId: String, variant: String) = viewModelScope.launch (Dispatchers.IO) {
        val productEntity = dbRepository.getProductWithIdForUpdate(productId)
        productEntity.variantInCart.remove(variant)
        if (productEntity.variantInCart.isEmpty()) {
            productEntity.inCart = false
        }
        dbRepository.upsertProduct(productEntity)
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

    fun clearCart(cartItems: List<CartEntity>) = viewModelScope.launch (Dispatchers.Default) {
        for (cartItem in cartItems) {
            withContext(Dispatchers.IO) {
                val product = dbRepository.getProductWithIdForUpdate(cartItem.productId)
                product.inCart = false
                product.variantInCart.clear()
                dbRepository.upsertProduct(product)
            }
        }
        withContext(Dispatchers.IO) {
            dbRepository.clearCart()
        }
    }

    //Coupons
    fun getAllCoupons(status: String) = viewModelScope.launch (Dispatchers.IO) {
        val coupons = dbRepository.getAllActiveCoupons(status)
        withContext(Dispatchers.Main) {
            _coupons.value = coupons
        }
    }

    fun isCouponAvailable(mCoupons: List<CouponEntity>, couponCode: String): Boolean {
        var isAvailable: Boolean = false
        for (i in mCoupons.indices) {
            if ( mCoupons[i].code == couponCode &&
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
        fbRepository.placeOrder(order, this@CheckoutViewModel)
    }

    fun orderPlacementFailed(message: String) {
        _orderPlacementFailed.value = message
    }

    fun limitedItemsUpdater(cartEntity: List<CartEntity>) = viewModelScope.launch {
        withContext(Dispatchers.Default) {
            val limitedCartItems = mutableListOf<CartEntity>()
            for (cartItem in cartEntity) {
                withContext(Dispatchers.IO) {
                    Log.e("qqqq", cartItem.toString())
                    val product = dbRepository.getProductWithIdForUpdate(cartItem.productId)
                    Log.e("qqqq", "limitedItemsUpdater: $product", )
                    if (product.variants[cartItem.variantIndex].status == Constants.LIMITED) {
                        limitedCartItems.add(cartItem)
                    }
                }
            }
            withContext(Dispatchers.IO) {
                fbRepository.limitedItemsUpdater(limitedCartItems, this@CheckoutViewModel)
            }
        }
    }

    fun limitedItemsUpdated() {
        _limitedItemUpdateStatus.value = "complete"
    }

    fun orderPlaced() = viewModelScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            _orderCompleted.value = true
        }
    }

    fun getWallet() = dbRepository.getWallet()

    suspend fun validateItemAvailability(cartItems: List<CartEntity>) : List<CartEntity> = withContext(Dispatchers.IO) {
        fbRepository.validateItemAvailability(cartItems)
    }
}