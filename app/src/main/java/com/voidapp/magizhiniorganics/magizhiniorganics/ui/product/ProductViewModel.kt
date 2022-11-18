package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ReviewAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LIMITED
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SHORT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ProductViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
) : ViewModel() {

    var tempFile: File? = null
    var reviewAdapter: ReviewAdapter? = null

    var userProfile: UserProfileEntity? = null

    var product: ProductEntity? = null
    var productID: String = ""
    var selectedVariantPosition: Int = 0
    var selectedVariantName: String = ""
    val similarProducts: MutableList<ProductEntity> = mutableListOf()

    val cartItems: MutableList<CartEntity> = mutableListOf()
    val clearedIDsFromCart: MutableList<String> = mutableListOf()
    var cartItemsCount: Int = 0

    var couponPrice: Float? = null
    var currentCoupon: CouponEntity? = null

    private var _reviews: MutableLiveData<ArrayList<Review>?> = MutableLiveData()
    val reviews: LiveData<ArrayList<Review>?> = _reviews
    private var _description: MutableLiveData<String> = MutableLiveData()
    val description: LiveData<String> = _description
    private var _previewImage: MutableLiveData<String> = MutableLiveData()
    val previewImage: LiveData<String> = _previewImage

    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate
    private val _uiEvent: MutableLiveData<UIEvent> = MutableLiveData()
    val uiEvent: LiveData<UIEvent> = _uiEvent

    fun setEmptyStatus() {
        _uiUpdate.value = UiUpdate.Empty
    }

    fun setEmptyUiEvent() {
        _uiEvent.value = UIEvent.EmptyUIEvent
    }

    fun getProfileData() = viewModelScope.launch(Dispatchers.IO) {
        try {
            userProfile = dbRepository.getProfileData()
        } catch (e: IOException) {
            e.message?.let {
                fbRepository.logCrash(
                    "sub product: getting the sub product from DB",
                    it
                )
            }
        }
    }

    fun getProductByID() = viewModelScope.launch(Dispatchers.IO) {
        dbRepository.getProductWithIdForUpdate(productID)?.let {
            it.variants.forEach { variant ->
                if (variant.status == LIMITED) {
                    setUpProductListener(it.id)
                    return@forEach
                }
            }
            withContext(Dispatchers.Main) {
                product = it
                _description.value = it.description
                _uiUpdate.value = UiUpdate.PopulateProductData(null, it)
            }
            if (similarProducts.isEmpty()) {
                dbRepository.getAllProductByCategoryStatic(it.category).let { products ->
                    similarProducts.clear()
                    similarProducts.addAll(products.shuffled())
                }
                withContext(Dispatchers.Main) {
                    _uiUpdate.value = UiUpdate.PopulateSimilarProducts
                }
            }
        } ?: withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.PopulateProductData("Product not available", null)
        }
    }

    //limited variant listener
    private fun setUpProductListener(id: String) = viewModelScope.launch {
        fbRepository.productListener(id, this@ProductViewModel)
    }

    fun updateLimitedVariant(variants: ArrayList<ProductVariant>) {
        product?.let {
            it.variants.clear()
            it.variants.addAll(variants)
        }
        _uiUpdate.value = UiUpdate.UpdateLimitedItemCount(null)
    }

    //review listener
    fun getProductReviews() = viewModelScope.launch {
        fbRepository.productReviewsListener(productID, this@ProductViewModel)
    }

    fun reviewListener(reviews: ArrayList<Review>) {
        if (reviews.isNullOrEmpty()) {
            _reviews.value = null
        } else {
            reviews.sortedByDescending {
                it.timeStamp
            }
            _reviews.value = reviews
        }
    }

    fun getDiscountPercent(price: Float, discountPrice: Float): Float =
        ((price - discountPrice) / price) * 100

    fun getVariantName(position: Int): String {
        val variant = product!!.variants[position]
        return "${variant.variantName} ${variant.variantType}"
    }

    fun updateVariantName(position: Int) {
        val variant = product!!.variants[position]
        selectedVariantName = "${variant.variantName} ${variant.variantType}"
    }

    private fun getVariantOriginalPrice(position: Int) =
        product!!.variants[position].variantPrice.toFloat()

    fun isProductAvailable(): Boolean {
        return product?.let {
            it.variants[selectedVariantPosition].status != Constants.OUT_OF_STOCK
        } ?: false
    }

    fun checkStoragePermission() {
        _uiUpdate.value = UiUpdate.CheckStoragePermission(null)
    }

    fun previewImage(content: String) {
        _previewImage.value = content
    }

//    fun openPreview(url: String, content: String, thumbnail: ShapeableImageView) {
//        _uiUpdate.value = UiUpdate.OpenPreviewImage(content, url, null, thumbnail)
//    }

    fun upsertProductReview(
        review: Review,
        uri: Uri?,
        extension: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            userProfile?:let {
                UIEvent.Toast("You must be signed in to add review", SHORT)
                return@withContext
            }
            _uiEvent.value = UIEvent.ProgressBar(true)
        }
        if (uri == null) {
            uploadReviewToFirebase(review)
        } else {
            val imageUrl = fbRepository.uploadImage(
                "${Constants.REVIEW_IMAGE_PATH}${productID}/",
                uri,
                extension,
                "review"
            )

            if (imageUrl == "failed") {
                withContext(Dispatchers.Main) {
                    _uiEvent.value = UIEvent.ProgressBar(false)
                    _uiEvent.value =
                        UIEvent.SnackBar("Server error! Failed to add Review Image", true)
                }
            } else {
                review.reviewImageUrl = imageUrl
                uploadReviewToFirebase(review)
            }
        }
    }

    private suspend fun uploadReviewToFirebase(review: Review) = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            userProfile?:let {
                UIEvent.Toast("You must be signed in to add review", SHORT)
                return@withContext
            }
        }
        fbRepository.addReview(productID, review)
        tempFile?.delete()
        tempFile = null
        withContext(Dispatchers.Main) {
            _uiEvent.value = UIEvent.ProgressBar(false)
            _uiEvent.value = UIEvent.Toast("Thanks for the Review :)")
            previewImage("added")
        }
    }

    fun updateFavorites() = viewModelScope.launch(Dispatchers.IO) {
        val id = userProfile?.id ?: ""

        product?.let {
            it.favorite = !it.favorite

            val localFavoritesUpdate = async { localFavoritesUpdate(it) }
            val storeFavoritesUpdate = async { storeFavoritesUpdate(id, it) }
            val updateProduct = async { updateProduct(it) }

            if (
                localFavoritesUpdate.await() &&
                storeFavoritesUpdate.await() &&
                updateProduct.await()
            ) {
                withContext(Dispatchers.Main) {
                    _uiUpdate.value = UiUpdate.UpdateFavorites(it.favorite)
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiEvent.value = UIEvent.Toast("Failed to set Favorites")
                }
            }
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
                fbRepository.logCrash(
                    "product activity: updating favorites to DB",
                    it
                )
            }
            false
        }
    }

    private suspend fun storeFavoritesUpdate(id: String, product: ProductEntity): Boolean =
        withContext(Dispatchers.IO) {
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
                fbRepository.logCrash(
                    "product activity: updating favorites to DB",
                    it
                )
            }
            false
        }
    }

    //live data of the cart items

    fun getAllCartItem() = viewModelScope.launch(Dispatchers.IO) {
        /*
        * we are making two different copies where one copy is passed initially and hard copy is made which is later used for recycler view
        * */
        dbRepository.getAllCartItem()?.let { cart ->
            cartItems.clear()
            cartItemsCount = 0
            cart.forEach {
                cartItemsCount += it.quantity
            }
            withContext(Dispatchers.Main) {
                cartItems.addAll(cart.map {
//                    cartItemsCount += it.quantity
                    it.copy()
                })
                _uiUpdate.value = UiUpdate.PopulateCartData(cart)
            }
        } ?: withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.PopulateCartData(null)
        }
    }

    fun upsertCartItem(productQuantity: Int) = viewModelScope.launch(Dispatchers.IO) {
        try {
            CartEntity(
                productId = product!!.id,
                productName = product!!.name,
                thumbnailUrl = product!!.thumbnailUrl,
                variant = selectedVariantName,
                quantity = productQuantity,
                maxOrderQuantity = getMaxOrderQuantity(),
                price = getSelectedItemPrice(),  //todo
                originalPrice = getVariantOriginalPrice(selectedVariantPosition),
                couponName = if (product!!.extras.isEmpty()) "0" else product!!.extras[0].toString(),
                variantIndex = selectedVariantPosition
            ).let { cartEntity ->
                val cartJob = async {
                    dbRepository.upsertCart(cartEntity)
                }
                val productJob = async {
                    product?.let {
                        it.variantInCart.add(selectedVariantName)
                        dbRepository.upsertProduct(it)
                    }
                }
                cartJob.await()
                productJob.await()
                val newItem = dbRepository.getCartItemByProduct(product!!.id, selectedVariantName)!!
                withContext(Dispatchers.Main) {
                    cartItemsCount += 1
                    cartItems.add(newItem)
                    _uiUpdate.value = UiUpdate.AddCartItem(newItem)
                }
            }
        } catch (e: IOException) {
            e.message?.let {
                fbRepository.logCrash(
                    "product activity: adding product to cart in DB",
                    it
                )
            }
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.AddCartItem(null)
            }
        }
    }

    fun getSelectedItemPrice(): Float {
        product?.let {
            val variantPrice = if (it.variants[selectedVariantPosition].discountPrice == 0.0) {
                it.variants[selectedVariantPosition].variantPrice
            } else {
                it.variants[selectedVariantPosition].discountPrice
            }
            val appliedCouponPrice = couponPrice?.let {
                couponDiscount(currentCoupon!!, variantPrice.toFloat())
            } ?: 0f
            return (variantPrice.toFloat() - appliedCouponPrice)
        } ?: return 0f
    }

    private fun getMaxOrderQuantity(): Int {
        return if (product!!.variants[selectedVariantPosition].inventory == 0) {
            10
        } else {
            product!!.variants[selectedVariantPosition].inventory
        }
    }

    //from cart adapter
    fun updateCartItem(id: Int, updatedCount: Int, position: Int, addOrRemove: String) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cartItemsCount = if (addOrRemove == "add") {
                    cartItemsCount + 1
                } else {
                    cartItemsCount - 1
                }
                dbRepository.updateCartItem(id, updatedCount)
                cartItems[position].quantity = updatedCount
                withContext(Dispatchers.Main) {
                    _uiUpdate.value = UiUpdate.UpdateCartData("update", position, updatedCount)
                }
            } catch (e: Exception) {
                e.message?.let { fbRepository.logCrash("checkout: updating cart item in db", it) }
            }
        }

    fun deleteCartItem(id: Int, productId: String, variant: String, position: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cartItemsCount -= cartItems[position].quantity
                cartItems.removeAt(position)
                val delete = async { dbRepository.deleteCartItem(id) }
                val updateProduct = async {
                    dbRepository.getProductWithIdForUpdate(productId)?.let {
                        updatingTheCartInProduct(it, variant)
                    }
                }
                delete.await()
                updateProduct.await()
                withContext(Dispatchers.Main) {
                    clearedIDsFromCart.add(productId)
                    _uiUpdate.value = UiUpdate.UpdateCartData("delete", position, null)
                }
            } catch (e: Exception) {
                e.message?.let { fbRepository.logCrash("checkout: deleting cart item in db", it) }
            }
        }

    //this is called to remove item from cart by clicking remove button from prod page
    fun deleteProductFromCart() = viewModelScope.launch(Dispatchers.IO) {
        try {
            var cartItemPosition: Int = 0
            product?.let {
                val delete = async {
                    cart@ for (i in cartItems.indices) {
                        if (
                            cartItems[i].productId == productID &&
                            cartItems[i].variant == selectedVariantName
                        ) {
                            cartItemPosition = i
                            break@cart
                        }
                    }
                    cartItemsCount -= cartItems[cartItemPosition].quantity
                    cartItems.removeAt(cartItemPosition)
                    dbRepository.deleteProductFromCart(product!!.id, selectedVariantName)
                }
                val updateProduct = async { updatingTheCartInProduct(it, selectedVariantName) }
                delete.await()
                updateProduct.await()
                withContext(Dispatchers.Main) {
                    _uiUpdate.value = UiUpdate.UpdateCartData("delete", cartItemPosition, null)
                }
            }
        } catch (e: IOException) {
            e.message?.let {
                fbRepository.logCrash(
                    "product activity:  to DB",
                    it
                )
            }
        }
    }

    private suspend fun updatingTheCartInProduct(product: ProductEntity, variant: String) =
        withContext(Dispatchers.IO) {
            try {
                product.variantInCart.remove(variant)
                if (product.variantInCart.isEmpty()) {
                    product.inCart = false
                }
                dbRepository.upsertProduct(product)
                true
            } catch (e: IOException) {
                e.message?.let {
                    fbRepository.logCrash(
                        "product activity: updating the product after remove from cart in db",
                        it
                    )
                }
                false
            }
        }

    fun getCartPrice(cartItems: List<CartEntity>): Float {
        return cartItems.indices
            .asSequence()
            .map { (cartItems[it].price * cartItems[it].quantity) }
            .sum()
    }

    fun verifyCoupon(couponCode: String) = viewModelScope.launch(Dispatchers.IO) {
        if (couponCode == "") {
            return@launch
        } else {
            val code: CouponEntity? = currentCoupon?.let {
                currentCoupon
            } ?: dbRepository.getCouponByCode(couponCode)
            code?.let { coupon ->
                if (!coupon.categories.contains(product!!.category)) {
                    withContext(Dispatchers.Main) {
                        currentCoupon = null
                        _uiEvent.value =
                            UIEvent.Toast("Coupon does not apply for products in this Category")
                    }
                    return@launch
                }
                if (couponPrice == null) {
                    currentCoupon = coupon
                    couponPrice = couponDiscount(coupon, getSelectedItemPrice())
                    withContext(Dispatchers.Main) {
                        _uiEvent.value =
                            UIEvent.Toast("Rs: $couponPrice Coupon Discount is Applied")
                        _uiUpdate.value = UiUpdate.CouponApplied(true)
                    }
                }
            } ?: withContext(Dispatchers.Main) {
                _uiEvent.value = UIEvent.Toast("Coupon Code does not exist.")
            }
        }
    }

    private fun couponDiscount(coupon: CouponEntity, cartPrice: Float): Float {
        var discountPrice = when (coupon.type) {
            "percent" -> (cartPrice * coupon.amount) / 100
            "rupees" -> coupon.amount
            else -> 0f
        }

        if (discountPrice > coupon.maxDiscount) {
            discountPrice = coupon.maxDiscount
        }

        return discountPrice
    }

    fun setNullCoupon() {
        _uiUpdate.value = UiUpdate.CouponApplied(null)
    }

    fun clearCart() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val clearInProduct = async {
                for (cartItem in cartItems) {
                    clearedIDsFromCart.add(cartItem.productId)
                    val entity = dbRepository.getProductWithIdForUpdate(cartItem.productId)
                    entity?.let { product ->
                        product.inCart = false
                        product.variantInCart.clear()
                        dbRepository.upsertProduct(product)
                    }
                }
                cartItems.clear()
            }
            val clearCart = async {
                product?.let {
                    it.inCart = false
                    it.variantInCart.clear()
                }
                cartItemsCount = 0
                dbRepository.clearCart()
            }
            clearInProduct.await()
            clearCart.await()
            withContext(Dispatchers.Main) {
                _uiEvent.value = UIEvent.Toast("Cart Items Emptied")
                _uiUpdate.value = UiUpdate.CartCleared(null)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _uiEvent.value = UIEvent.Toast("Failed to clear cart")
            }
            e.message?.let { fbRepository.logCrash("checkout: clearing cart from db", it) }
        }
    }

    fun getHowToVideo(where: String) = viewModelScope.launch {
        val url = fbRepository.getHowToVideo(where)
        _uiUpdate.value = UiUpdate.HowToVideo(url)
    }

    sealed class UiUpdate {
        data class PopulateProductData(val message: String?, val product: ProductEntity?) :
            UiUpdate()
        object PopulateSimilarProducts: UiUpdate()

        data class UpdateLimitedItemCount(val message: String?) : UiUpdate()
        data class UpdateFavorites(val isFavorite: Boolean) : UiUpdate()

        //coupon
        data class CouponApplied(val message: Boolean?) : UiUpdate()

        //cart
        data class PopulateCartData(val cartItems: List<CartEntity>?) : UiUpdate()
        data class AddCartItem(val cartEntity: CartEntity?) : UiUpdate()
        data class UpdateCartData(val message: String, val position: Int, val count: Int?) :
            UiUpdate()

        data class CartCleared(val ids: MutableList<String>?) : UiUpdate()
        data class ClearedProductIDs(val ids: MutableList<String>) : UiUpdate()

        //review
        data class CheckStoragePermission(val message: String?) : UiUpdate()
        //preview
//        data class OpenPreviewImage(val message: String?, val imageUrl: String?, val imageUri: Uri?, val thumbnail: ShapeableImageView): UiUpdate()

        //howto
        data class HowToVideo(val url: String): UiUpdate()

        object Empty : UiUpdate()
    }
}