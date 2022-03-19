package com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CWMFood
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DishViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {

    var dish: CWMFood? = null
    var userProfile: UserProfileEntity? = null
    val cartItems = mutableListOf<CartEntity>()
    var positionToUpdate = 0

    //live data for fragments
    private var _reviews: MutableLiveData<ArrayList<Review>?> = MutableLiveData()
    val reviews: LiveData<ArrayList<Review>?> = _reviews
    private var _description: MutableLiveData<String> = MutableLiveData()
    val description: LiveData<String> = _description
    private var _previewImage: MutableLiveData<String> = MutableLiveData()
    val previewImage: LiveData<String> = _previewImage

    private val _storagePermissionCheck: MutableLiveData<Boolean?> = MutableLiveData()
    val storagePermissionCheck: LiveData<Boolean?> = _storagePermissionCheck
    private val _openPreviewImage: MutableLiveData<String?> = MutableLiveData()
    val openPreviewImage: LiveData<String?> = _openPreviewImage

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(
        NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    fun setStoragePermission(check: Boolean?) {
        _storagePermissionCheck.value = check
    }

    fun setEmptyStatus() {
        _status.value = NetworkResult.Empty
    }

    fun updateCartItem(position: Int, count: Int) {
        positionToUpdate = position
        cartItems[position].quantity = count
        updateDishPrice(position, count)
        _status.value = NetworkResult.Success("update", cartItems)
    }

    private fun updateDishPrice(position: Int, count: Int) {
        dish?.let {
            it.totalPrice -= (it.ingredients[position].price * it.ingredients[position].quantity)
            it.ingredients[position].quantity = count
            it.totalPrice += (it.ingredients[position].price * it.ingredients[position].quantity)
        }
    }

    fun deleteItemFromCart(position: Int) {
        positionToUpdate = position
        cartItems.removeAt(position)
        dish?.let {
            it.totalPrice -= (it.ingredients[position].price * it.ingredients[position].quantity)
            it.ingredients.removeAt(position)
        }
        _status.value = NetworkResult.Success("update", cartItems)
    }

    fun getProductReviews() = viewModelScope.launch {
        dish?.let {
            fbRepository.productReviewsListener(it.id, this@DishViewModel)
        }
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

    fun previewImage(content: String) {
        _previewImage.value = content
    }

    fun openPreview(url: String, content: String) {
        _openPreviewImage.value = url
    }

    fun upsertProductReview(
        review: Review,
        uri: Uri?,
        extension: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            _status.value = NetworkResult.Loading("")
        }
        if (uri == null) {
            Log.e("qw", "upsertProductReview: no img", )
            uploadReviewToFirebase(review)
        } else {
            val imageUrl = fbRepository.uploadImage(
                "${Constants.REVIEW_IMAGE_PATH}${dish!!.id}/",
                uri,
                extension,
                "review"
            )

            if (imageUrl == "failed") {
                withContext(Dispatchers.Main) {
                    _status.value = NetworkResult.Failed( "image","server error! Failed to upload image")
                }
            } else {
                review.reviewImageUrl = imageUrl
                uploadReviewToFirebase(review)
            }
        }
    }

    private suspend fun uploadReviewToFirebase(review: Review) = withContext(Dispatchers.IO) {
        fbRepository.addReview(dish!!.id, review)
        withContext(Dispatchers.Main) {
            _status.value = NetworkResult.Success( "image", "Thanks for the Review :)")
            previewImage("added")
        }
    }

    fun getCartSize(ingredients: MutableList<CartEntity>): Int {
        var size: Int = 0
        ingredients.forEach {
            size += it.quantity
        }
        return size
    }

    fun getUserProfile() =viewModelScope.launch(Dispatchers.IO) {
        dbRepository.getProfileData()?.let {
            userProfile = it
        }
    }
}