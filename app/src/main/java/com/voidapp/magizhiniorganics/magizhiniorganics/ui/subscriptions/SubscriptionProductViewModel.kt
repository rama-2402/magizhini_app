package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions

import android.net.Uri
import android.util.Log
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
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class SubscriptionProductViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {
    var mProductID = ""
    private var subscriptionItem = ProductEntity()
    var userProfile = UserProfileEntity()
    var liveWallet = Wallet()

    private var _product: MutableLiveData<ProductEntity> = MutableLiveData()
    val product: LiveData<ProductEntity> = _product
    private var _failed: MutableLiveData<String> = MutableLiveData()
    val failed: LiveData<String> = _failed
    private var _reviews: MutableLiveData<ArrayList<Review>> = MutableLiveData()
    val reviews: LiveData<ArrayList<Review>> = _reviews

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(
        NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status
    private val _wallet: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(NetworkResult.Empty)
    val wallet: StateFlow<NetworkResult> = _wallet

    fun emptyResult() {
        _status.value = NetworkResult.Empty
    }

    fun getProductByID(id: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val product = dbRepository.getProductWithIdForUpdate(id)
            withContext(Dispatchers.Main) {
                subscriptionItem = product
                getProductReviews(product.id)
                _product.value = product
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("sub product: getting the sub product from DB", it) }
        }
    }

    fun getProfileData() = viewModelScope.launch (Dispatchers.IO){
        try {
            val profileEntity = dbRepository.getProfileData()!!
            withContext(Dispatchers.Main) {
                userProfile = profileEntity
                getWallet(profileEntity.id)
            }
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("sub product: getting the profile from DB", it) }
        }
    }

    private suspend fun getProductReviews(id: String) = fbRepository.productReviewsListener(id, this)

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

    private fun getWallet(id: String) = viewModelScope.launch(Dispatchers.IO) {
        _wallet.value = fbRepository.getWallet(id)
    }

    fun generateSubscription(subscription: Subscription) = viewModelScope.launch(Dispatchers.IO) {
        _status.value = fbRepository.generateSubscription(subscription)
    }

    fun checkWalletForBalance(amount: Float): Boolean {
        return liveWallet.amount >= amount
    }

    suspend fun makeTransactionFromWallet(amount: Float, id: String, orderID: String, transactionType: String) {
        if (fbRepository.makeTransactionFromWallet(amount, id, transactionType)) {
            TransactionHistory (
                orderID,
                System.currentTimeMillis(),
                TimeUtil().getMonth(),
                TimeUtil().getYear().toLong(),
                amount,
                id,
                id,
                Constants.SUCCESS,
                Constants.PURCHASE,
                orderID
            ).let {
                _status.value = NetworkResult.Success("transaction", it)
            }
        } else {
            _status.value = NetworkResult.Failed("transaction", "Server Error. Failed to make transaction from Wallet. Try other payment method")
        }
    }

    fun updateTransaction(transaction: TransactionHistory) = viewModelScope.launch (Dispatchers.IO) {
        _status.value = fbRepository.updateTransaction(transaction)
    }

    suspend fun generateSubscriptionID(id: String): String = withContext(Dispatchers.IO) {
        return@withContext fbRepository.generateSubscriptionID(id)
    }
}

