package com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ShoppingMainAdapter.ShoppingMainAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.ItemsBottomSheet
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL_PRODUCTS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShoppingMainViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {

    var categoryFilter: String = ALL_PRODUCTS
    var selectedProductID: String = ""
    var selectedProductPosition: Int? = null
    var navigateToPage: String = ""
    var shoppingMainListener: ShoppingMainAdapter.ShoppingMainListener? = null

    var selectedChip: String = Constants.ALL
    var selectedCategory: String = ""
    var profile = UserProfileEntity()

    var categoriesAdapter: ItemsBottomSheet? = null

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
    private var _position: MutableLiveData<Int> = MutableLiveData()
    val position: LiveData<Int> = _position

    var productToRefresh = ProductEntity()
    private val currentProductsList: MutableList<ProductEntity> = mutableListOf()
    val removedProductIDsFromCart: MutableList<String> = mutableListOf()

    fun getProfile() {
        profile = dbRepository.getProfileData()!!
    }
    fun getAllCartItems() = dbRepository.getAllCartItems()
    fun getCartItemsPrice() = dbRepository.getCartPrice()
    fun getALlCategories() = dbRepository.getAllProductCategories()
    fun getAllProducts() = dbRepository.getAllProducts()
    fun getAllFavoriteProducts() = dbRepository.getAllFavorites()
    fun getAllDiscount() = dbRepository.getAllDiscounts()
    fun getAllProductsInCategory(category: String) = dbRepository.getAllProductsInCategory(category)

    val allProducts: MutableList<ProductEntity> = mutableListOf()
    val discountProducts: MutableList<ProductEntity> = mutableListOf()
    val favoriteProducts: MutableList<ProductEntity> = mutableListOf()
    val subscriptionProducts: MutableList<ProductEntity> = mutableListOf()
    val limitedProducts: MutableList<ProductEntity> = mutableListOf()

    fun getAllProductsStatic() = viewModelScope.launch(Dispatchers.Default) {
        val productsJob = async { dbRepository.getAllProductsStatic() }
        val products = productsJob.await() as MutableList<ProductEntity>
        val dummyFilter = mutableListOf<ProductEntity>()
        dummyFilter.addAll(products)
        dummyFilter.forEach { product ->
            val dummyVariants = arrayListOf<ProductVariant>()
            dummyVariants.addAll(product.variants)
            product.variants.clear()
            dummyVariants.forEach { variant ->
                if (variant.status != Constants.LIMITED) {
                    product.variants.add(variant)
                }
            }
            if (product.variants.isEmpty()) {
                products.remove(product)
            }
        }
        withContext(Dispatchers.Main) {
            currentProductsList.clear()
            currentProductsList.addAll(products)
            _allProducts.value = products
        }
    }

    fun getAllSubscriptions() = viewModelScope.launch(Dispatchers.IO) {
        val products = dbRepository.getAllSubscriptions(Constants.SUBSCRIPTION)
        withContext(Dispatchers.Main) {
            currentProductsList.clear()
            currentProductsList.addAll(products)
            _subscriptions.value = products
        }
    }

    fun getAllProductByCategoryStatic(categoryFilter: String) = viewModelScope.launch(Dispatchers.IO) {
        val products = dbRepository.getAllProductByCategoryStatic(categoryFilter)
        withContext(Dispatchers.Main) {
            currentProductsList.clear()
            currentProductsList.addAll(products)
            _allProductsInCategory.value = products
        }
    }
    fun getAllFavoritesStatic() = viewModelScope.launch(Dispatchers.IO) {
        val products = dbRepository.getAllFavoritesStatic()
        withContext(Dispatchers.Main) {
            currentProductsList.clear()
            currentProductsList.addAll(products)
            _allFavorites.value = products
        }
    }
    fun getAllDiscountProducts() = viewModelScope.launch (Dispatchers.IO) {
        val products = dbRepository.getAllDiscountProducts()
        withContext(Dispatchers.Main) {
            currentProductsList.clear()
            currentProductsList.addAll(products)
            _discountAvailableProducts.value = products
        }
    }
//    fun getAllCategoryNames() = viewModelScope.launch (Dispatchers.IO) {
//        val names = dbRepository.getAllCategoryNames()
//        withContext(Dispatchers.Main) {
//            _availableCategoryNames.value = names
//        }
//    }

    fun deleteCartItemFromShoppingMain(product: ProductEntity, variantName: String, position: Int) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.deleteProductFromCart(product.id, variantName)
        updatingTheCartInProduct(product,"", variantName, position)
    }

    fun deleteCartItem(id: Int, productId: String, variant: String) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.deleteCartItem(id)
        updatingTheCartInProduct(null, productId, variant)
    }

    private suspend fun updatingTheCartInProduct(product: ProductEntity?, productId: String = "", variant: String, position: Int = -1)  {
        val productEntity = if (product == null) {
            val entity = dbRepository.getProductWithIdForUpdate(productId)
            entity?.let { it ->
                it.variantInCart.remove(variant)
                it
            }
        } else {
            product.variantInCart.remove(variant)
            product
        }

        if (productEntity?.variantInCart!!.isEmpty()) {
            productEntity.inCart = false
        }
        dbRepository.upsertProduct(productEntity)

        if (position < 0) {
            removedProductIDsFromCart.add(productId)
//            withContext(Dispatchers.Main) {
//                productToRefresh = productEntity
//                updateShoppingMainPage()
//            }
        } else {
            withContext(Dispatchers.Main) {
                productToRefresh = productEntity
                _position.value = position
            }
        }
    }

    private suspend fun updateShoppingMainPage() {
        when(selectedChip) {
            Constants.ALL -> getAllProductsStatic()
            Constants.CATEGORY -> getAllProductByCategoryStatic(selectedCategory)
            Constants.FAVORITES -> getAllFavoritesStatic()
            Constants.DISCOUNT -> getAllDiscountProducts()
            Constants.SUBSCRIPTION -> getAllSubscriptions()
        }
    }

    fun upsertCartItem(product: ProductEntity, position: Int , variant: String, count: Int, price:
    Float, originalPrice: Float, variantIndex: Int, maxOrderQuantity: Int) = viewModelScope.launch(Dispatchers.IO) {

        val orderQuantity = if (maxOrderQuantity == 0) {
                10
            } else {
                maxOrderQuantity
            }

        CartEntity(
            productId = product.id,
            productName = product.name,
            thumbnailUrl = product.thumbnailUrl,
            variant = variant,
            quantity = count,
            maxOrderQuantity = orderQuantity,
            price = price,
            originalPrice = originalPrice,
            variantIndex = variantIndex
        ).also { cartEntity ->
            dbRepository.upsertCart(cartEntity)
            product.variantInCart.add(variant)
            dbRepository.upsertProduct(product)
            withContext(Dispatchers.Main) {
                productToRefresh = product
                _position.value = position
            }
        }
    }

    fun updateCartItem(id: Int, updatedCount: Int) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.updateCartItem(id, updatedCount)
    }

    fun updateFavorites(id: String, product: ProductEntity, position: Int) = viewModelScope.launch(Dispatchers.IO) {
        val localFavoritesUpdate = async { localFavoritesUpdate(product) }
        val storeFavoritesUpdate = async { storeFavoritesUpdate(id, product) }
        val updateProduct = async { updateProduct(product) }

        localFavoritesUpdate.await()
        storeFavoritesUpdate.await()
        updateProduct.await()

        withContext(Dispatchers.Main) {
            productToRefresh = product
            _position.value = position
        }
    }

    private suspend fun localFavoritesUpdate(product: ProductEntity) = withContext(Dispatchers.IO) {
        if (product.favorite) {
            Favorites(product.id).also {
                dbRepository.upsertFavorite(it)
            }
        } else {
            dbRepository.deleteFavorite(product.id)
        }
    }

    private suspend fun storeFavoritesUpdate(id: String, product: ProductEntity) = withContext(Dispatchers.IO) {
        if (product.favorite) {
            fbRepository.addFavorites(id, product.id)
        } else {
            fbRepository.removeFavorites(id, product.id)
        }
    }

    private suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        dbRepository.updateProductFavoriteStatus(product.id, product.favorite)
    }

    fun limitedItemsFilter() = viewModelScope.launch {
        fbRepository.getLimitedItems(this@ShoppingMainViewModel)
    }

    fun limitedProducts(mutableLimitedItems: MutableList<ProductEntity>) = viewModelScope.launch(Dispatchers.Default){
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
        withContext(Dispatchers.Main) {
            currentProductsList.clear()
            currentProductsList.addAll(products)
            shoppingMainListener?.limitedItemList(products)
        }
    }

    fun refreshProduct() = viewModelScope.launch(Dispatchers.IO) {
        productToRefresh?.let {
            dbRepository.getProductWithIdForUpdate(selectedProductID)?.let {
                withContext(Dispatchers.Main) {
                    productToRefresh = it
                    _position.value = selectedProductPosition
                }
            }
        }
    }

    val changedProductsPositions = mutableListOf<Int>()
    val changedProducts = mutableListOf<ProductEntity>()
    private val _changedPositions: MutableLiveData<Int> = MutableLiveData()
    val changedPositions: LiveData<Int> = _changedPositions


    fun updateProducts(productIDs: List<String>) = viewModelScope.launch(Dispatchers.IO) {
        changedProducts.clear()
        changedProductsPositions.clear()
        for (id in productIDs.distinct()){
            prod@
            for (i in currentProductsList.indices) {
                if (id == currentProductsList[i].id) {
                    dbRepository.getProductWithIdForUpdate(currentProductsList[i].id)?.let {
                        changedProducts.add(it)
                        changedProductsPositions.add(i)
                    }
                    break@prod
                }
            }
        }
        withContext(Dispatchers.Main) {
            removedProductIDsFromCart.clear()
            _changedPositions.value = System.currentTimeMillis().toInt()
        }
    }

    fun clearCart(cartItems: MutableList<CartEntity>) = viewModelScope.launch(Dispatchers.IO) {
        try {
            for (cartItem in cartItems) {
                val entity = dbRepository.getProductWithIdForUpdate(cartItem.productId)
                entity?.let { product ->
                    removedProductIDsFromCart.add(product.id)
                    product.inCart = false
                    product.variantInCart.clear()
                    dbRepository.upsertProduct(product)
                }
            }
            dbRepository.clearCart()
            updateProducts(removedProductIDsFromCart)
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("checkout: clearing cart from db", it) }
        }
    }
}


