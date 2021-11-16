package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import androidx.lifecycle.*
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.Favorites
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class HomeViewModel (
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository
): ViewModel() {


    private var _specialsOne: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
    val specialsOne: LiveData<MutableList<ProductEntity>> = _specialsOne
    private var _specialsTwo: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
    val specialsTwo: LiveData<MutableList<ProductEntity>> = _specialsTwo
    private var _specialsThree: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
    val specialsThree: LiveData<MutableList<ProductEntity>> = _specialsThree
    private var _bestSellers: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
    val bestSellers: LiveData<MutableList<ProductEntity>> = _bestSellers
    private var _recyclerPosition: MutableLiveData<Int> = MutableLiveData()
    val recyclerPosition: LiveData<Int> = _recyclerPosition
    private var _specialBanners: MutableLiveData<List<String>> = MutableLiveData()
    val specialBanners: LiveData<List<String>> = _specialBanners

    var recyclerToRefresh = ""

    var bestSellerHeader = ""
    var specialsOneHeader = ""
    var specialsTwoHeader = ""
    var specialsThreeHeader = ""

    var homeListener: HomeListener? = null

    fun getAllBanners() = dbRepository.getAllBanners()

    fun getALlCategories() = dbRepository.getAllProductCategories()

    fun selectedCategory(category: String) {
        homeListener?.displaySelectedCategory(category)
    }

    fun signOut() {
        fbRepository.signOut()
    }

    fun upsertCartItem(product: ProductEntity, variant: String, count: Int, price:
    Float, originalPrice: Float, variantIndex: Int, position: Int, recycler: String) = viewModelScope.launch(
        Dispatchers.IO) {
        try {
            val orderQuantity = if (product.variants[0].inventory == 0) {
                10
            } else {
                product.variants[0].inventory
            }
            val cartEntity = CartEntity(
                productId = product.id,
                productName = product.name,
                thumbnailUrl = product.thumbnailUrl,
                variant = variant,
                quantity = count,
                maxOrderQuantity = orderQuantity,
                price = price,
                originalPrice = originalPrice,
                variantIndex = variantIndex
            )
            dbRepository.upsertCart(cartEntity)
            val productEntity = dbRepository.getProductWithIdForUpdate(product.id)
            productEntity.variantInCart.add(variant)
            dbRepository.upsertProduct(productEntity)
            withContext(Dispatchers.Main) {
                recyclerToRefresh = recycler
                _recyclerPosition.value = position
            }
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("Home: add item to cart", it) }
        }
    }

    fun deleteCartItemFromShoppingMain(productEntity: ProductEntity, variantName: String, position: Int, recycler: String) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val product = dbRepository.getProductWithIdForUpdate(productEntity.id)
            product.variantInCart.remove(variantName)
            dbRepository.upsertProduct(product)
            dbRepository.deleteProductFromCart(productEntity.id, variantName)
            updatingTheCartInProduct(productEntity.id , variantName, position, recycler)
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: delete item to cart", it) }
        }
    }

    private suspend fun updatingTheCartInProduct(productId: String, variant: String, position: Int, recycler: String) =
        withContext(Dispatchers.IO){
        try {
            val productEntity = dbRepository.getProductWithIdForUpdate(productId)
            productEntity.variantInCart.remove(variant)
            if (productEntity.variantInCart.isEmpty()) {
                productEntity.inCart = false
            }
            dbRepository.upsertProduct(productEntity)
            withContext(Dispatchers.Main) {
                recyclerToRefresh = recycler
                _recyclerPosition.value = position
            }
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("Home: updating product after delete from cart", it) }
        }
    }

    fun updateFavorites(id: String, product: ProductEntity) = viewModelScope.launch(Dispatchers.IO) {
        val localFavoritesUpdate = async { localFavoritesUpdate(product) }
        val storeFavoritesUpdate = async { storeFavoritesUpdate(id, product) }
        val updateProduct = async { updateProduct(product) }

        localFavoritesUpdate.await()
        storeFavoritesUpdate.await()
        updateProduct.await()
    }

    private suspend fun localFavoritesUpdate(product: ProductEntity) = withContext(Dispatchers.IO) {
        try {
            if (product.favorite) {
                Favorites(product.id).also {
                    dbRepository.upsertFavorite(it)
                }
            } else {
                dbRepository.deleteFavorite(product.id)
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: updating favorites to db", it) }
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
        try {
            dbRepository.updateProductFavoriteStatus(product.id, product.favorite)
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: update product fav boolean status", it) }
        }
    }

    fun moveToProductDetails(id: String, name: String) {
        homeListener?.moveToProductDetails(id, name)
    }

    fun getDataToPopulate() = viewModelScope.launch(Dispatchers.IO) {

        val getBestSellers = async { getBestSellers() }
        val getSpecialsOne = async { getSpecialsOne() }
        val getSpecialsTwo = async { getSpecialsTwo() }
        val getSpecialsThree = async { getSpecialsThree() }
        val getSpecialBanners = async { getSpecialBanners() }

        getBestSellers.await()
        getSpecialsOne.await()
        getSpecialsTwo.await()
        getSpecialsThree.await()
        getSpecialBanners.await()
    }

    private suspend fun getSpecialBanners() {
        try {
            val banners = dbRepository.getSpecialBanners()
            val bannerUrls = arrayListOf<String>()
            for (item in banners.indices) {
                bannerUrls.add(banners[item].url)
            }
            withContext(Dispatchers.Main) {
                _specialBanners.value = bannerUrls
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating spl banners from db", it) }
        }
    }

    private suspend fun getBestSellers() {
        try {
            val bestSeller = dbRepository.getBestSellers()
            bestSellerHeader = bestSeller.name
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(bestSeller.id)
            for (item in items.indices) {
                products.add(dbRepository.getProductWithIdForUpdate(items[item]))
            }
            withContext(Dispatchers.Main) {
                _bestSellers.value = products
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating best sellers from db", it) }
        }
    }
    private suspend fun getSpecialsOne() {
        try {
            val one = dbRepository.getSpecialsOne()
            specialsOneHeader = one.name
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(one.id)
            for (item in items.indices) {
                products.add(dbRepository.getProductWithIdForUpdate(items[item]))
            }
            withContext(Dispatchers.Main) {
                _specialsOne.value = products
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating spl one from db", it) }
        }
    }
    private suspend fun getSpecialsTwo() {
        try {
            val two = dbRepository.getSpecialsTwo()
            specialsTwoHeader = two.name
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(two.id)
            for (item in items.indices) {
                products.add(dbRepository.getProductWithIdForUpdate(items[item]))
            }
            withContext(Dispatchers.Main) {
                _specialsTwo.value = products
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating spl two from db", it) }
        }
    }
    private suspend fun getSpecialsThree() {
        try {
            val three = dbRepository.getSpecialsThree()
            specialsThreeHeader = three.name
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(three.id)
            for (item in items.indices) {
                products.add(dbRepository.getProductWithIdForUpdate(items[item]))
            }
            withContext(Dispatchers.Main) {
                _specialsThree.value = products
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating spl three from db", it) }
        }
    }

    fun getUpdatedBestSellers() = viewModelScope.launch(Dispatchers.IO) {
        getBestSellers()
    }
    fun getUpdatedSpecialsOne() = viewModelScope.launch(Dispatchers.IO) {
        getSpecialsOne()
    }
    fun getUpdatedSpecialsTwo() = viewModelScope.launch(Dispatchers.IO) {
        getSpecialsTwo()
    }
    fun getUpdatedSpecialsThree() = viewModelScope.launch(Dispatchers.IO) {
        getSpecialsThree()
    }
}