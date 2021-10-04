package com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.coroutines.*

class ShoppingMainViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {

    var shoppingMainListener: ShoppingMainListener? = null

    var selectedChip: String = Constants.ALL
    var selectedCategory: String = ""

    private var _allProducts: MutableLiveData<List<ProductEntity>> = MutableLiveData()
    val allProduct: LiveData<List<ProductEntity>> = _allProducts
    private var _allProductsInCategory: MutableLiveData<List<ProductEntity>> = MutableLiveData()
    val allProductsInCategory: LiveData<List<ProductEntity>> = _allProductsInCategory
    private var _allFavorites: MutableLiveData<List<ProductEntity>> = MutableLiveData()
    val allFavorites: LiveData<List<ProductEntity>> = _allFavorites
    private var _availableCategoryNames: MutableLiveData<List<String>> = MutableLiveData()
    val availableCategoryNames: LiveData<List<String>> = _availableCategoryNames
    private var _discountAvailableProducts: MutableLiveData<List<ProductEntity>> = MutableLiveData()
    val discountAvailableProducts: LiveData<List<ProductEntity>> = _discountAvailableProducts
    private var _subscriptions: MutableLiveData<List<ProductEntity>> = MutableLiveData()
    val subscriptions: LiveData<List<ProductEntity>> = _subscriptions
    private var _subscriptionProduct: MutableLiveData<ProductEntity> = MutableLiveData()
    val subscriptionProduct: LiveData<ProductEntity> = _subscriptionProduct

    fun getAllCartItems() = dbRepository.getAllCartItems()
    fun getCartItemsPrice() = dbRepository.getCartPrice()

    fun getAllProductsStatic() = viewModelScope.launch(Dispatchers.IO) {
        val products = dbRepository.getAllProductsStatic()
        withContext(Dispatchers.Main) {
            _allProducts.value = products
        }
    }

    fun getAllSubscriptions() = viewModelScope.launch(Dispatchers.IO) {
        val products = dbRepository.getAllSubscriptions(Constants.SUBSCRIPTION)
        withContext(Dispatchers.Main) {
            _subscriptions.value = products
        }
    }

    fun getAllProductByCategoryStatic(categoryFilter: String) = viewModelScope.launch(Dispatchers.IO) {
        val products = dbRepository.getAllProductByCategoryStatic(categoryFilter)
        withContext(Dispatchers.Main) {
            _allProductsInCategory.value = products
        }
    }
    fun getAllFavoritesStatic() = viewModelScope.launch(Dispatchers.IO) {
        val products = dbRepository.getAllFavoritesStatic()
        withContext(Dispatchers.Main) {
            _allFavorites.value = products
        }
    }
    fun getAllDiscountProducts() = viewModelScope.launch (Dispatchers.IO) {
        val products = dbRepository.getAllDiscountProducts()
        withContext(Dispatchers.Main) {
            _discountAvailableProducts.value = products
        }
    }
    fun getAllCategoryNames() = viewModelScope.launch (Dispatchers.IO) {
        val names = dbRepository.getAllCategoryNames()
        withContext(Dispatchers.Main) {
            _availableCategoryNames.value = names
        }
    }

    fun deleteCartItemFromShoppingMain(productId: String, variantName: String) = viewModelScope.launch (Dispatchers.IO) {
        val product = dbRepository.getProductWithIdForUpdate(productId)
        product.variantInCart.remove(variantName)
        dbRepository.upsertProduct(product)
        dbRepository.deleteCartItemFromShoppingMain(productId, variantName)
        updatingTheCartInProduct(productId, variantName)
    }

    fun deleteCartItem(id: Int, productId: String, variant: String) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.deleteCartItem(id)
        updatingTheCartInProduct(productId, variant)
        getAllCartItems()
    }

    private fun updatingTheCartInProduct(productId: String, variant: String) = viewModelScope.launch (Dispatchers.IO) {
        val productEntity = dbRepository.getProductWithIdForUpdate(productId)
        productEntity.variantInCart.remove(variant)
        if (productEntity.variantInCart.isEmpty()) {
            productEntity.inCart = false
        }
        dbRepository.upsertProduct(productEntity)
        updateShoppingMainPage()
    }

    private fun updateShoppingMainPage() {
        when(selectedChip) {
            Constants.ALL -> getAllProductsStatic()
            Constants.CATEGORY -> getAllProductByCategoryStatic(selectedCategory)
            Constants.FAVORITES -> getAllFavoritesStatic()
            Constants.DISCOUNT -> getAllDiscountProducts()
            Constants.SUBSCRIPTION -> getAllSubscriptions()
        }
    }

    fun subscriptionItemToView(product: ProductEntity) {
        _subscriptionProduct.value = product
    }

    fun upsertCartItem(id: String, productName: String, thumbnailUrl: String,  variant: String, count: Int, price:
    Float, originalPrice: Float, maximumOrderQuantity: Int, variantIndex: Int) = viewModelScope.launch(Dispatchers.IO) {
        val orderQuantity = if (maximumOrderQuantity == 0) {
            10
        } else {
            maximumOrderQuantity
        }
        val cartEntity = CartEntity(
            productId = id,
            productName = productName,
            thumbnailUrl = thumbnailUrl,
            variant = variant,
            quantity = count,
            maxOrderQuantity = orderQuantity,
            price = price,
            originalPrice = originalPrice,
            variantIndex = variantIndex
        )
        dbRepository.upsertCart(cartEntity)
        val product = dbRepository.getProductWithIdForUpdate(id)
        product.variantInCart.add(variant)
        dbRepository.upsertProduct(product)
        getAllCartItems()
    }

    fun updateCartItem(id: Int, updatedCount: Int) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.updateCartItem(id, updatedCount)
        getAllCartItems()
    }

    fun upsertProduct(product: ProductEntity) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.upsertProduct(product)
    }

    fun updateFavorites(id: String, position: Int, addedItem: String, removedItem:String) = viewModelScope.launch(Dispatchers.IO) {
       //if if the passed on variable is empty then update will not take place indicating that it is not added or deleted
        if (addedItem !== "") {
            //we get the profile data and add/remove the item from the arraylist and then we update the profile data back
           val profile = dbRepository.getProfileData()!!
           profile.favorites.add(addedItem)
           dbRepository.upsertProfile(profile)
            //after the profile update in the database the update will be called for firestore data
           fbRepository.addFavorties(id, addedItem)
       }
        if (removedItem !== "") {
            val profile = dbRepository.getProfileData()!!
            profile.favorites.remove(removedItem)
            dbRepository.upsertProfile(profile)
            fbRepository.removeFavorites(id, removedItem)
            updateShoppingMainPage()
        }
    }

    fun limitedItemsFilter() {
        fbRepository.getLimitedItems(this)
    }

    fun limitedProducts(mutableLimitedItems: MutableList<ProductEntity>) {
        val products: List<ProductEntity> = mutableLimitedItems
        products.forEach { product ->
            val allVariants = mutableListOf<ProductVariant>()
            allVariants.addAll(product.variants)
            product.variants.clear()
            for (i in allVariants.indices) {
                val variant = allVariants[i]
                if (variant.status == Constants.LIMITED) {
                    product.defaultVariant = i
                    product.variants.add(variant)
                }
            }
        }
        Log.e("qqqq", "limitedProducts: $products", )
        shoppingMainListener?.limitedItemList(products)
    }

    fun moveToProductDetails(id: String, name: String) {
        shoppingMainListener?.moveToProductDetails(id, name)
    }


}