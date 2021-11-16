package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ListenableWorker
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

class ProductViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {

    var productID = ""
    var product = ProductEntity()
    var userProfile: UserProfileEntity = UserProfileEntity()
    var coupons: List<CouponEntity> = listOf<CouponEntity>()

    private var _reviews: MutableLiveData<ArrayList<Review>> = MutableLiveData()
    val reviews: LiveData<ArrayList<Review>> = _reviews
    private var _itemCount: MutableLiveData<ArrayList<ProductVariant>> = MutableLiveData()
    val itemCount: LiveData<ArrayList<ProductVariant>> = _itemCount
    private var _couponIndex: MutableLiveData<Int> = MutableLiveData()
    val couponIndex: LiveData<Int> = _couponIndex
    private var _isCouponApplied: MutableLiveData<Boolean> = MutableLiveData()
    val isCouponApplied: LiveData<Boolean> = _isCouponApplied
    private var _previewImage: MutableLiveData<String> = MutableLiveData()
    val previewImage: LiveData<String> = _previewImage

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    fun getCartItemsPrice() = dbRepository.getCartPrice()

    fun previewImage(url: String) {
        _previewImage.value = url
    }

    fun getProfileData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            userProfile = dbRepository.getProfileData()!!
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("sub product: getting the sub product from DB", it) }
        }
    }

    fun getProductById(id: String) = dbRepository.getProductWithId(id)

    fun setEmptyResult() {
        _status.value = NetworkResult.Empty
    }
//
//    fun getDiscountedPrice(productEntity: ProductEntity, position: Int): String {
//        val variant = productEntity.variants[position]
//        val price = variant.variantPrice.toFloat()
//        val discountAmount = variant.discountPrice
//        //checking if the selected variant has any discount. If the variant has discount then
//        //it will take priority over the total product discount
//        when (discountAvailability(productEntity, position)) {
//            "variant" -> {
//                return if (variant.discountType == "Percentage") {
//                    "${(price - (price * discountAmount) / 100)}"
//                } else {
//                    "${price - discountAmount}"
//                }
//            }
//            "product" -> {
//                return if (productEntity.discountType == "Percentage") {
//                    "${(price - (price * productEntity.discountAmt) / 100)}"
//                } else {
//                    "${price - productEntity.discountAmt}"
//                }
//            }
//            else -> {
//                return "0"
//            }
//        }
//    }

//    fun discountAvailability(productEntity: ProductEntity, position: Int): String {
//        return if (productEntity.variants[position].variantDiscount) {
//            "variant"
//        } else
//            "product"
//    }

    fun checkStoragePermission() {
        _status.value = NetworkResult.Success("permission", null)
    }

    fun openPreview(url: String, content: String) {
        _status.value = NetworkResult.Success(content, url)
    }

    fun getProductReviews(id: String) = viewModelScope.launch {
        fbRepository.productReviewsListener(id, this@ProductViewModel)
    }

    fun reviewListener(reviews: ArrayList<Review>) {
        reviews.sortedByDescending {
            it.timeStamp
        }
        _reviews.value = reviews
    }

    fun upsertProductReview(
        review: Review,
        id: String,
        uri: Uri?,
        extension: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        _status.value = NetworkResult.Loading("")
        if (uri == null) {
            _status.value = fbRepository.addReview(id, review)
        } else {
            val imageUrl = fbRepository.uploadImage(
                "${Constants.REVIEW_IMAGE_PATH}${id}/",
                uri,
                extension,
                "review"
            )

            if (imageUrl == "failed") {
                _status.value = NetworkResult.Failed("review", "Server error! Review image could not be added")
            } else {
                review.reviewImageUrl = imageUrl
                _status.value = fbRepository.addReview(id, review)
            }
        }
    }

    fun upsertProduct(productEntity: ProductEntity) = viewModelScope.launch (Dispatchers.IO) {
        try {
            dbRepository.upsertProduct(productEntity)
        } catch (e: IOException) {
            e.message?.let {
                fbRepository.logCrash("product activity: updating product to DB",
                    it
                )
            }
        }
    }

    fun updateFavorites(id: String, product: ProductEntity) = viewModelScope.launch(Dispatchers.IO) {
        val localFavoritesUpdate = async { localFavoritesUpdate(product) }
        val storeFavoritesUpdate = async { storeFavoritesUpdate(id, product) }
        val updateProduct = async { updateProduct(product) }

        if (
            localFavoritesUpdate.await() &&
            storeFavoritesUpdate.await() &&
            updateProduct.await()
        ) {
           _status.value = NetworkResult.Success("toast", "Added to Favorties")
        } else {
            _status.value = NetworkResult.Failed("toast", "Added to Favorties")
        }
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
            true
        } catch (e: IOException) {
            e.message?.let {
                fbRepository.logCrash("product activity: updating favorites to DB",
                    it
                )
            }
            false
        }
    }

    private suspend fun storeFavoritesUpdate(id: String, product: ProductEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext if (product.favorite) {
                fbRepository.addFavorites(id, product.id)
            } else {
                fbRepository.removeFavorites(id, product.id)
            }
        } catch (e: IOException) {
            e.message?.let {
                fbRepository.logCrash(
                    "product activity: updating favorites to store",
                    it
                )
            }
            false
        }
    }

    private suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        try {
            dbRepository.updateProductFavoriteStatus(product.id, product.favorite)
            true
        } catch (e: IOException) {
            e.message?.let {
                fbRepository.logCrash("product activity: updating favorites to DB",
                    it
                )
            }
            false
        }
    }

    //live data of the cart items
    fun getAllCartItems() = dbRepository.getAllCartItems()

    fun upsertCartItem(id: String, productName: String, thumbnailUrl:String, variant: String, count: Int, price: Float, originalPrice: Float, couponCode: String, maximumOrderCount: Int,
    variantIndex: Int) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val orderQuantity = if (maximumOrderCount == 0) {
                10
            } else {
                maximumOrderCount
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
                couponName = couponCode,
                variantIndex = variantIndex
            )
            dbRepository.upsertCart(cartEntity)
            NetworkResult.Success("toast", "Added to Cart")
        } catch (e: IOException) {
            e.message?.let {
                fbRepository.logCrash("product activity: adding product to cart in DB",
                    it
                )
            }
            NetworkResult.Success("toast", "Failed to add in Cart")
        }
    }

    //from cart adapter
    fun updateCartItem(id: Int, updatedCount: Int) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.updateCartItem(id, updatedCount)
    }

    fun deleteProductFromCart(product: ProductEntity, variantName: String) = viewModelScope.launch (Dispatchers.IO) {
        try {
            dbRepository.deleteProductFromCart(product.id, variantName)
            updatingTheCartInProduct(product, variantName)
        } catch (e: IOException) {
            e.message?.let {
                fbRepository.logCrash("product activity:  to DB",
                    it
                )
            }
        }
    }

    fun deleteCartItem(id: Int, productId: String, variant: String) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.deleteCartItem(id)
//        getAllCartItems()
        updatingTheCartInProduct(product, variant)
        //calling this to reflect the product variant is remove from cart change the add/remove button accordingly
//        getProductById(productId)
    }

    private suspend fun updatingTheCartInProduct(product: ProductEntity, variant: String) = withContext(Dispatchers.IO) {
        try {
            product.variantInCart.remove(variant)
            if (product.variantInCart.isEmpty()) {
                product.inCart = false
            }
            dbRepository.upsertProduct(product)
            true
        } catch (e: IOException) {
            e.message?.let {
                fbRepository.logCrash("product activity: updating the product after remove from cart in db",
                    it
                )
            }
            false
        }
    }

    //limited variant listener
    fun setUpProductListener(id: String) = viewModelScope.launch {
        fbRepository.productListener(id, this@ProductViewModel)
    }

    fun updateLimitedVariant(variants: ArrayList<ProductVariant>) {
        _itemCount.value = variants
    }

    //Coupons
    fun getAllCoupons(status: String) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val couponsInDB = dbRepository.getAllActiveCoupons(status)
            withContext(Dispatchers.Main) {
                coupons = couponsInDB
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("product activity: getting all the coupons from db", it) }
        }
    }

    fun isCouponAvailable(couponCode: String, categoryName: String): Boolean {
        var isAvailable: Boolean = false
        _isCouponApplied.value = false
        for (i in coupons.indices) {
            if ( coupons[i].code == couponCode && coupons[i].categories.contains(categoryName)) {
                isAvailable = true
                _couponIndex.value = i
                _isCouponApplied.value = true
                break
            }
        }
        return isAvailable
    }

    fun removeCouponCode() {
        _isCouponApplied.value = false
    }


}