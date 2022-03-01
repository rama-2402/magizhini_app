package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import android.util.Log
import androidx.lifecycle.*
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.BirthdayCard
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TestimonialsEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class HomeViewModel (
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository
): ViewModel() {

    private var _referralStatus: MutableLiveData<Boolean> = MutableLiveData()
    val referralStatus: LiveData<Boolean> = _referralStatus
    private var _allowReferral: MutableLiveData<String> = MutableLiveData()
    val allowReferral: LiveData<String> = _allowReferral
    private var _showBirthday: MutableLiveData<BirthdayCard?> = MutableLiveData()
    val showBirthday: LiveData<BirthdayCard?> = _showBirthday
    private var _specialsOne: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
    val specialsOne: LiveData<MutableList<ProductEntity>> = _specialsOne
    private var _specialsTwo: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
    val specialsTwo: LiveData<MutableList<ProductEntity>> = _specialsTwo
    private var _specialsThree: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
    val specialsThree: LiveData<MutableList<ProductEntity>> = _specialsThree
    private var _bestSellers: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
    val bestSellers: LiveData<MutableList<ProductEntity>> = _bestSellers
    private var _testimonials: MutableLiveData<MutableList<TestimonialsEntity>> = MutableLiveData()
    val testimonials: LiveData<MutableList<TestimonialsEntity>> = _testimonials
    private var _recyclerPosition: MutableLiveData<Int> = MutableLiveData()
    val recyclerPosition: LiveData<Int> = _recyclerPosition
    private var _specialBanners: MutableLiveData<List<SpecialBanners>> = MutableLiveData()
    val specialBanners: LiveData<List<SpecialBanners>> = _specialBanners
    private var _notifications: MutableLiveData<List<UserNotificationEntity>?> = MutableLiveData()
    val notifications: LiveData<List<UserNotificationEntity>?> = _notifications

    val bannersList: MutableList<SpecialBanners> = mutableListOf()

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
            val entity = dbRepository.getProductWithIdForUpdate(product.id)
            entity?.let { productEntity ->
                productEntity.variantInCart.add(variant)
                dbRepository.upsertProduct(productEntity)
                withContext(Dispatchers.Main) {
                    recyclerToRefresh = recycler
                    _recyclerPosition.value = position
                }
            }
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("Home: add item to cart", it) }
        }
    }

    fun deleteCartItemFromShoppingMain(productEntity: ProductEntity, variantName: String, position: Int, recycler: String) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val entity = dbRepository.getProductWithIdForUpdate(productEntity.id)
            entity?.let { product ->
                product.variantInCart.remove(variantName)
                dbRepository.upsertProduct(product)
                dbRepository.deleteProductFromCart(productEntity.id, variantName)
                updatingTheCartInProduct(productEntity.id , variantName, position, recycler)
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: delete item to cart", it) }
        }
    }

    private suspend fun updatingTheCartInProduct(productId: String, variant: String, position: Int, recycler: String) =
        withContext(Dispatchers.IO){
        try {
            val entity = dbRepository.getProductWithIdForUpdate(productId)
            entity?.let { productEntity ->
                productEntity.variantInCart.remove(variant)
                if (productEntity.variantInCart.isEmpty()) {
                    productEntity.inCart = false
                }
                dbRepository.upsertProduct(productEntity)
                withContext(Dispatchers.Main) {
                    recyclerToRefresh = recycler
                    _recyclerPosition.value = position
                }
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
        val getAllTestimonials = async { getAllTestimonials() }

        getBestSellers.await()
        getSpecialsOne.await()
        getSpecialsTwo.await()
        getSpecialsThree.await()
        getSpecialBanners.await()
        getAllTestimonials.await()
    }

    private suspend fun getSpecialBanners() {
        try {
            val banners = dbRepository.getSpecialBanners()
            bannersList.clear()
            withContext(Dispatchers.Main) {
                bannersList.addAll(banners)
                _specialBanners.value = banners
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
                dbRepository.getProductWithIdForUpdate(items[item])?.let { products.add(it) }
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
                dbRepository.getProductWithIdForUpdate(items[item])?.let{products.add(it)}
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
                dbRepository.getProductWithIdForUpdate(items[item])?.let{products.add(it)}
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
                dbRepository.getProductWithIdForUpdate(items[item])?.let{products.add(it)}
            }
            withContext(Dispatchers.Main) {
                _specialsThree.value = products
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating spl three from db", it) }
        }
    }

    private suspend fun getAllTestimonials() {
        try {
            val testimonials = dbRepository.getAllTestimonials()
            withContext(Dispatchers.Main) {
                _testimonials.value = testimonials as MutableList<TestimonialsEntity>
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating testimonials from db", it) }
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

    fun updateToken(token: String?)= viewModelScope.launch(Dispatchers.IO) {
        token?.let {
            val birthdayCard: BirthdayCard? = fbRepository.updateToken(it)
            withContext(Dispatchers.Main) {
                _showBirthday.value = birthdayCard
            }
        }
    }

    suspend fun getProductByID(id: String): ProductEntity? = withContext(Dispatchers.IO) {
        return@withContext dbRepository.getProductWithIdForUpdate(id)
    }

    suspend fun getCategoryByID(id: String): String = withContext(Dispatchers.IO) {
        dbRepository.getCategoryByID(id)?.let { return@withContext it } ?: "Dairy Products"
    }

    fun getAllNotifications() = viewModelScope.launch(Dispatchers.IO) {
        val notifications = dbRepository.getAllNotifications()
            withContext(Dispatchers.Main) {
                _notifications.value = notifications
            }
    }

    fun applyReferralNumber(currentUserID: String, code: String) = viewModelScope.launch {
        _referralStatus.value = fbRepository.applyReferralNumber(currentUserID, code, true)
    }

    fun checkForReferral() = viewModelScope.launch(Dispatchers.IO) {
        val profile = dbRepository.getProfileData()!!
        withContext(Dispatchers.Main) {
           if( profile.referralId.isNullOrEmpty() ) {
               _allowReferral.value = profile.id
           } else {
               _allowReferral.value = "no"
           }
        }
    }

    fun updateBirthdayCard(customerID: String) = viewModelScope.launch(Dispatchers.IO) {
        fbRepository.updateBirthdayCard(customerID)
    }
}