package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ListenableWorker
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.SubscriptionUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.business.BusinessViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SINGLE_DAY_LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SubscriptionProductViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository,
    private val subscriptionUseCase: SubscriptionUseCase
): ViewModel() {
    var navigateToPage: String = ""

    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate
    private val _uiEvent: MutableLiveData<UIEvent> = MutableLiveData()
    val uiEvent: LiveData<UIEvent> = _uiEvent

    var product: ProductEntity? = null
    var wallet: Wallet? = null
    var userProfile: UserProfileEntity? = null
    var address: Address? = null

    var subStartDate: Long = System.currentTimeMillis() + SINGLE_DAY_LONG
    val customSubDays: MutableList<String> = mutableListOf()
    val subCancelledDates: MutableList<Long> = mutableListOf()

    //live data for fragments
    private var _reviews: MutableLiveData<ArrayList<Review>?> = MutableLiveData()
    val reviews: LiveData<ArrayList<Review>?> = _reviews
    private var _description: MutableLiveData<String> = MutableLiveData()
    val description: LiveData<String> = _description
    private var _previewImage: MutableLiveData<String> = MutableLiveData()
    val previewImage: LiveData<String> = _previewImage

    fun setEmptyUiEvent() {
        _uiEvent.value = UIEvent.EmptyUIEvent
    }

    fun setEmptyStatus() {
        _uiUpdate.value = UiUpdate.Empty
    }

    fun checkStoragePermission() {
        _uiUpdate.value = UiUpdate.CheckStoragePermission(null)
    }

    fun getProductByID(id: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val prod = dbRepository.getProductWithIdForUpdate(id)
            prod ?: withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.PopulateProduct(null)
            }
            prod?.let {
                getProfileData()
                withContext(Dispatchers.Main) {
                    product = it
                    getProductReviews()
                    _uiUpdate.value = UiUpdate.PopulateProduct(it)
                    _description.value = it.description
                }
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("sub product: getting the sub product from DB", it) }
        }
    }

    fun getProfileData() = viewModelScope.launch (Dispatchers.IO){
        try {
            dbRepository.getProfileData()?.let {
                withContext(Dispatchers.Main) {
                    userProfile = it
                    getWallet(it.id)
                }
            }
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("sub product: getting the profile from DB", it) }
        }
    }

    fun getProductReviews() = viewModelScope.launch {
        product?.let {
            fbRepository.productReviewsListener(it.id, this@SubscriptionProductViewModel)
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
        _uiUpdate.value = UiUpdate.OpenPreviewImage(content, url, null)
    }

    fun upsertProductReview(
        review: Review,
        uri: Uri?,
        extension: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            _uiEvent.value = UIEvent.ProgressBar(true)
        }
        if (uri == null) {
            uploadReviewToFirebase(review)
        } else {
            val imageUrl = fbRepository.uploadImage(
                "${Constants.REVIEW_IMAGE_PATH}${product!!.id}/",
                uri,
                extension,
                "review"
            )

            if (imageUrl == "failed") {
                withContext(Dispatchers.Main) {
                    _uiEvent.value = UIEvent.ProgressBar(false)
                    _uiEvent.value = UIEvent.SnackBar("Server error! Failed to add Review Image", true)
                }
            } else {
                review.reviewImageUrl = imageUrl
                uploadReviewToFirebase(review)
            }
        }
    }

    private suspend fun uploadReviewToFirebase(review: Review) = withContext(Dispatchers.IO) {
        fbRepository.addReview(product!!.id, review)
        withContext(Dispatchers.Main) {
            _uiEvent.value = UIEvent.ProgressBar(false)
            _uiEvent.value = UIEvent.Toast("Thanks for the Review :)")
            previewImage("added")
        }
    }

    private fun getWallet(userID: String) = viewModelScope.launch {
        when(val result = fbRepository.getWallet(userID)) {
            is NetworkResult.Success -> {
                wallet = result.data as Wallet
            }
            is NetworkResult.Failed -> {
                _uiEvent.value = UIEvent.SnackBar(result.data.toString(), true)
            }
            else -> Unit
        }
    }

    fun generateSubscription(subscriptionMap: HashMap<String, Any>, transactionID: String?) = viewModelScope.launch {
        _uiUpdate.value = UiUpdate.CreateStatusDialog("Creating Subscription", "sub")
        Subscription(
            id = generateSubscriptionID(),
            cancelledDates = subCancelledDates as ArrayList<Long>,
            customDates = customSubDays as ArrayList<String>
        ).also { sub ->
            address?.let {
                sub.address = it
            }
            userProfile?.let {
                sub.customerID = it.id
                sub.phoneNumber = it.phNumber
            }
            product?.let {
                sub.productID = it.id
                sub.productName = it.name
                val variantName = "${it.variants[subscriptionMap["variantPosition"].toString().toInt()].variantName} ${it.variants[subscriptionMap["variantPosition"].toString().toInt()].variantType}"
                sub.variantName = variantName
            }
            sub.startDate = subStartDate
            sub.endDate = generateSubEndDate()
            sub.paymentMode = subscriptionMap["paymentMode"].toString()
            sub.subType = subscriptionMap["subType"].toString()
            sub.monthYear = subscriptionMap["monthYear"].toString()
            sub.status = subscriptionMap["status"].toString()
            val mrp = getEstimateAmount(subscriptionMap["subTypePosition"].toString().toInt(), subscriptionMap["variantPosition"].toString().toInt())
            sub.basePay = mrp.toFloat()
            sub.estimateAmount = mrp.toFloat()

            if (sub.paymentMode == "Online") {
                placeSubscriptionWithOnline(sub, transactionID!!)
            } else {
                placeSubscriptionWithWallet(sub, userProfile!!.name)
            }
        }
    }

    private suspend fun placeSubscriptionWithOnline(subscription: Subscription, transactionID: String) = withContext(Dispatchers.IO) {
        subscriptionUseCase
            .placeSubscriptionWithOnline(
                subscription,
                userProfile!!.name,
                transactionID
            ).onEach { result ->
                withContext(Dispatchers.Main) {
                    when(result) {
                        is NetworkResult.Success -> {
                            when(result.message) {
                                "placing" -> _uiUpdate.value = UiUpdate.PlacingSubscription("Validating Transaction...", "validating")
                                "placed" -> {
                                    _uiUpdate.value = UiUpdate.PlacedSubscription(
                                        "Subscription Placed Successfully!",
                                        "success"
                                    )
                                    delay(1800)
                                    _uiUpdate.value = UiUpdate.DismissStatusDialog(true)
                                }
                            }
                        }
                        is NetworkResult.Failed -> {
                            _uiUpdate.value = UiUpdate.UpdateStatusDialog(result.message, "fail")
                            delay(1800)
                            _uiUpdate.value = UiUpdate.DismissStatusDialog(false)
                        }
                        else -> Unit
                    }
                }
            }.launchIn(this)
    }

    private suspend fun placeSubscriptionWithWallet(subscription: Subscription, userName: String) = withContext(Dispatchers.IO) {
        subscriptionUseCase
            .placeSubscriptionWithWallet(
                subscription,
                userName
            ).onEach { result ->
                withContext(Dispatchers.Main) {
                    when(result) {
                        is NetworkResult.Success -> {
                            when(result.message) {
                                "transaction" -> _uiUpdate.value = UiUpdate.ValidatingTransaction("Making payment from wallet... ", "transaction")
                                "validating" -> _uiUpdate.value = UiUpdate.PlacingSubscription("Validating Transaction...", "validating")
                                "placed" -> {
                                    _uiUpdate.value = UiUpdate.PlacedSubscription(
                                        "Subscription Placed Successfully!",
                                        "success"
                                    )
                                    delay(1800)
                                    _uiUpdate.value = UiUpdate.DismissStatusDialog(true)
                                }
                            }
                        }
                        is NetworkResult.Failed -> {
                            _uiUpdate.value = UiUpdate.UpdateStatusDialog(result.message, "fail")
                            delay(1000)
                            _uiUpdate.value = UiUpdate.DismissStatusDialog(false)
                        }
                        else -> Unit
                    }
                }
            }.launchIn(this)
    }

    private suspend fun generateSubscriptionID(): String = withContext(Dispatchers.IO) {
        return@withContext fbRepository.generateSubscriptionID()
    }

    suspend fun getEstimateAmount(subTypePosition: Int, selectedVariantPosition: Int): Double = withContext(Dispatchers.Default) {
        product?.let {
            return@withContext when(subTypePosition) {
                0 -> it.variants[selectedVariantPosition].variantPrice * 30
                1 -> it.variants[selectedVariantPosition].variantPrice * 15
                else -> it.variants[selectedVariantPosition].variantPrice * (30 - subCancelledDates.size)
            }
        } ?: 0.0
    }

    suspend fun getCancelDates(date: Long , subTypePosition: Int) = withContext(Dispatchers.Default) {
        val singeDateDifference: Long = 86400000
        val cancelledDates = mutableListOf<Long>()
        var startDate = date - singeDateDifference
        subCancelledDates.clear()
        when (subTypePosition) {
            1 -> {
                for (i in 1..15) {
                    startDate += (2 * singeDateDifference)
                    cancelledDates.add(startDate)
                }
                subCancelledDates.addAll(cancelledDates)
            }
            2 -> {
                for (i in 1..30) {
                    startDate += singeDateDifference
                    val day = SimpleDateFormat("EEEE", Locale.ENGLISH).format(startDate)
                    if (!customSubDays.contains(day)) {
                        cancelledDates.add(startDate)
                    }
                }
                subCancelledDates.addAll(cancelledDates)
            }
            else -> subCancelledDates.clear()
        }
    }

    private fun generateSubEndDate(): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = subStartDate
        cal.add(Calendar.DATE, 29)  //since we add the start date as well, we add remaining 29 days to get a total of 30 days
        return cal.timeInMillis
    }

    sealed class UiUpdate {
        data class PopulateProduct(val product: ProductEntity?): UiUpdate()
        //review
        data class CheckStoragePermission(val message: String?): UiUpdate()
        //preview
        data class OpenPreviewImage(val message: String?, val imageUrl: String?, val imageUri: Uri?): UiUpdate()

        //status dialog
        data class CreateStatusDialog(val message: String?, val data: String?): UiUpdate()
        data class ValidatingTransaction(val message: String?, val data: String?): UiUpdate()
        data class PlacingSubscription(val message: String?, val data: String?): UiUpdate()
        data class PlacedSubscription(val message: String?, val data: String?): UiUpdate()
        data class UpdateStatusDialog(val message: String?, val data: String?): UiUpdate()
        data class DismissStatusDialog(val status: Boolean): UiUpdate()

        object Empty: UiUpdate()
    }
}

