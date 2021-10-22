package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product

import android.accessibilityservice.GestureDescription
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce

class ProductViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {

    var mProducts = ""

    private var _name: MutableLiveData<String> = MutableLiveData()
    val name: LiveData<String> = _name
    private var _profilePicUrl: MutableLiveData<String> = MutableLiveData()
    val profilePicUrl: LiveData<String> = _profilePicUrl
    private var _reviews: MutableLiveData<ArrayList<Review>> = MutableLiveData()
    val reviews: LiveData<ArrayList<Review>> = _reviews
    private var _itemCount: MutableLiveData<ArrayList<ProductVariant>> = MutableLiveData()
    val itemCount: LiveData<ArrayList<ProductVariant>> = _itemCount
    private var _coupons: MutableLiveData<List<CouponEntity>> = MutableLiveData()
    val coupons: LiveData<List<CouponEntity>> = _coupons
    private var _couponIndex: MutableLiveData<Int> = MutableLiveData()
    val couponIndex: LiveData<Int> = _couponIndex
    private var _isCouponApplied: MutableLiveData<Boolean> = MutableLiveData()
    val isCouponApplied: LiveData<Boolean> = _isCouponApplied
    private var _serverError: MutableLiveData<Boolean> = MutableLiveData()
    val serverError: LiveData<Boolean> = _serverError
    private var _uplodaingReviewStatus: MutableLiveData<Int> = MutableLiveData()
    val uplodaingReviewStatus: LiveData<Int> = _uplodaingReviewStatus
    private var _previewImage: MutableLiveData<String> = MutableLiveData()
    val reviewImage: LiveData<String> = _previewImage

    fun getCartItemsPrice() = dbRepository.getCartPrice()

    fun previewImage(url: String) {
        _previewImage.value = url
    }

    fun getProfileData() {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = dbRepository.getProfileData()!!
            val name = profile.name
            val url = profile.profilePicUrl
            withContext(Dispatchers.Main) {
                _name.value = name
                _profilePicUrl.value = url
            }
        }
    }

    fun getProductById(id: String) = dbRepository.getProductWithId(id)

    fun getDiscountedPrice(productEntity: ProductEntity, position: Int): String {
        val variant = productEntity.variants[position]
        val price = variant.variantPrice.toFloat()
        val discountAmount = variant.discountPercent
        //checking if the selected variant has any discount. If the variant has discount then
        //it will take priority over the total product discount
        when (discountAvailability(productEntity, position)) {
            "variant" -> {
                return if (variant.discountType == "Percentage") {
                    "${(price - (price * discountAmount) / 100)}"
                } else {
                    "${price - discountAmount}"
                }
            }
            "product" -> {
                return if (productEntity.discountType == "Percentage") {
                    "${(price - (price * productEntity.discountAmt) / 100)}"
                } else {
                    "${price - productEntity.discountAmt}"
                }
            }
            else -> {
                return "0"
            }
        }
    }

    fun discountAvailability(productEntity: ProductEntity, position: Int): String {
        return if (productEntity.variants[position].variantDiscount) {
            "variant"
        } else
            "product"
    }

    suspend fun upsertProductReview(
        review: Review,
        productEntity: ProductEntity,
        uri: Uri?,
        extension: String
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (uri == null) {
                productEntity.reviews.add(review)
                dbRepository.upsertProduct(productEntity)
                fbRepository.addReview(productEntity.id, review)
                return@withContext true
            } else {
                withContext(Dispatchers.Main) {
                    _uplodaingReviewStatus.value = +1
                }
                val imageUrl = fbRepository.uploadImage(
                    "${Constants.REVIEW_IMAGE_PATH}${productEntity.id}/",
                    uri,
                    extension,
                    "review"
                )

                if (imageUrl == "failed") {
                    if (_serverError.value == true) {
                        _serverError.value = false
                    } else {
                        _serverError.value = true
                    }
                    return@withContext false
                } else {
                    review.reviewImageUrl = imageUrl
                    productEntity.reviews.add(review)
                    dbRepository.upsertProduct(productEntity)
                    fbRepository.addReview(productEntity.id, review)
                    withContext(Dispatchers.Main) {
                        _uplodaingReviewStatus.value = -5
                    }
                    return@withContext true
                }
            }
        }

    fun upsertProduct(productEntity: ProductEntity) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.upsertProduct(productEntity)
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
            fbRepository.addFavorties(id, product.id)
        } else {
            fbRepository.removeFavorites(id, product.id)
        }
    }

    private suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        dbRepository.updateProductFavoriteStatus(product.id, product.favorite)
    }

    //live data of the cart items
    fun getAllCartItems() = dbRepository.getAllCartItems()

    fun upsertCartItem(id: String, productName: String, thumbnailUrl:String, variant: String, count: Int, price: Float, originalPrice: Float, couponCode: String, maximumOrderCount: Int,
    variantIndex: Int) = viewModelScope.launch(Dispatchers.IO) {
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
        Log.e("qqqq", "upsertCartItem: $cartEntity", )
        dbRepository.upsertCart(cartEntity)
    }

    //from cart adapter
    fun updateCartItem(id: Int, updatedCount: Int) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.updateCartItem(id, updatedCount)
    }

    fun deleteCartItemFromShoppingMain(productId: String, variantName: String) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.deleteCartItemFromShoppingMain(productId, variantName)
        updatingTheCartInProduct(productId, variantName)
    }

    fun deleteCartItem(id: Int, productId: String, variant: String) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.deleteCartItem(id)
        getAllCartItems()
        updatingTheCartInProduct(productId, variant)
        //calling this to reflect the product variant is remove from cart change the add/remove button accordingly
        getProductById(productId)
    }

    private fun updatingTheCartInProduct(productId: String, variant: String) = viewModelScope.launch (Dispatchers.IO) {
        val productEntity = dbRepository.getProductWithIdForUpdate(productId)
        productEntity.variantInCart.remove(variant)
        if (productEntity.variantInCart.isEmpty()) {
            productEntity.inCart = false
        }
        dbRepository.upsertProduct(productEntity)
    }

    fun getLimitedItems() = fbRepository.getLimitedItems(this)

    fun limitedProducts(limitedItems: MutableList<ProductEntity>) {
        limitedItems.forEach { product ->
            if (mProducts == product.id) {
                _itemCount.value = product.variants
            }
        }
    }

    //Coupons
    fun getAllCoupons(status: String) = viewModelScope.launch (Dispatchers.IO) {
        val coupons = dbRepository.getAllActiveCoupons(status)
        withContext(Dispatchers.Main) {
            _coupons.value = coupons
        }
    }

    fun isCouponAvailable(mCoupons: List<CouponEntity>, couponCode: String, categoryName: String): Boolean {
        var isAvailable: Boolean = false
        _isCouponApplied.value = false
        for (i in mCoupons.indices) {
            if ( mCoupons[i].code == couponCode && mCoupons[i].categories.contains(categoryName)) {
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