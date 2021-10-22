package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Subscription
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistory
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubscriptionProductViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {
    var mProductID = ""
    var mUpdatedProductReview = ProductEntity()

    private var _product: MutableLiveData<ProductEntity> = MutableLiveData()
    val product: LiveData<ProductEntity> = _product
    private var _profile: MutableLiveData<UserProfileEntity> = MutableLiveData()
    val profile: LiveData<UserProfileEntity> = _profile
    private var _wallet: MutableLiveData<Wallet> = MutableLiveData()
    val wallet: LiveData<Wallet> = _wallet
    private var _failed: MutableLiveData<String> = MutableLiveData()
    val failed: LiveData<String> = _failed
    private var _subStatus: MutableLiveData<String> = MutableLiveData()
    val subStatus: LiveData<String> = _subStatus
    private var _previewImage: MutableLiveData<String> = MutableLiveData()
    val reviewImage: LiveData<String> = _previewImage
    private var _serverError: MutableLiveData<Boolean> = MutableLiveData()
    val serverError: LiveData<Boolean> = _serverError
    private var _uploadingReviewStatus: MutableLiveData<Int> = MutableLiveData()
    val uploadingReviewStatus: LiveData<Int> = _uploadingReviewStatus

    fun previewImage(url: String) {
        _previewImage.value = url
    }

    fun getProductByID(id: String) = viewModelScope.launch(Dispatchers.IO) {
        val product = dbRepository.getProductWithIdForUpdate(id)
        withContext(Dispatchers.Main) {
            _product.value = product
        }
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
                mUpdatedProductReview = productEntity
                dbRepository.upsertProduct(productEntity)
                fbRepository.addReview(productEntity.id, review)
                return@withContext true
            } else {
                withContext(Dispatchers.Main) {
                    _uploadingReviewStatus.value = +1
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
                    mUpdatedProductReview = productEntity
                    dbRepository.upsertProduct(productEntity)
                    fbRepository.addReview(productEntity.id, review)
                    withContext(Dispatchers.Main) {
                        _uploadingReviewStatus.value = -5
                    }
                    return@withContext true
                }
            }
        }

    fun getProfileData() {
        viewModelScope.launch (Dispatchers.IO){
            val profileEntity = dbRepository.getProfileData()!!
            withContext(Dispatchers.Main) {
                _profile.value = profileEntity
            }
        }
    }

    fun getWallet(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val wallet = fbRepository.getWallet(id)
            withContext(Dispatchers.Main) {
                _wallet.value = wallet
            }
        }
    }

    fun generateSubscription(subscription: Subscription) = viewModelScope.launch(Dispatchers.IO) {
        fbRepository.generateSubscription(this@SubscriptionProductViewModel, subscription)
    }

    suspend fun subscriptionAdded(subscription: SubscriptionEntity) = withContext(Dispatchers.IO) {
        dbRepository.upsertSubscription(subscription)
        withContext(Dispatchers.Main) {
            _subStatus.value = "complete"
        }
    }

    fun subscriptionFailed(message: String) {
        _failed.value = message
    }


    suspend fun checkWalletForBalance(amount: Float, id: String): Boolean {
        try {
            val amountInWallet = fbRepository.getWalletAmount(id)
            return amountInWallet >= amount
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun makeTransactionFromWallet(amount: Float, id: String, orderID: String): String {
        if (fbRepository.makeTransactionFromWallet(amount, id, "Remove")) {
            val transaction = TransactionHistory (
                orderID,
                System.currentTimeMillis(),
                Time().getMonth(),
                Time().getYear().toLong(),
                amount,
                id,
                id,
                Constants.SUCCESS,
                Constants.SUBSCRIPTION,
                orderID
            )
            return fbRepository.updateTransaction(transaction)
        } else {
            return "failed"
        }
    }

    suspend fun generateSubscriptionID(id: String): String = withContext(Dispatchers.IO) {
        return@withContext fbRepository.generateSubscriptionID(id)
    }
}