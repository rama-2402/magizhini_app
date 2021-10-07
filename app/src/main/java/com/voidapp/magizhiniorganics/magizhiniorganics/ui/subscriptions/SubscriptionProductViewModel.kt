package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.WalletEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.bindings.WithContext

class SubscriptionProductViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {
    var mProductID = ""

    private var _product: MutableLiveData<ProductEntity> = MutableLiveData()
    val product: LiveData<ProductEntity> = _product
    private var _profile: MutableLiveData<UserProfileEntity> = MutableLiveData()
    val profile: LiveData<UserProfileEntity> = _profile
    private var _wallet: MutableLiveData<WalletEntity> = MutableLiveData()
    val wallet: LiveData<WalletEntity> = _wallet
    private var _failed: MutableLiveData<String> = MutableLiveData()
    val failed: LiveData<String> = _failed
    private var _subStatus: MutableLiveData<String> = MutableLiveData()
    val subStatus: LiveData<String> = _subStatus

    fun getProductByID(id: String) = viewModelScope.launch(Dispatchers.IO) {
        val product = dbRepository.getProductWithIdForUpdate(id)
        withContext(Dispatchers.Main) {
            _product.value = product
        }
    }

    fun upsertProductReview(id: String, review: Review, productEntity: ProductEntity) = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.upsertProduct(productEntity)
        fbRepository.addReview(id, review)
    }

    fun getProfileData() {
        viewModelScope.launch (Dispatchers.IO){
            val profileEntity = dbRepository.getProfileData()!!
            withContext(Dispatchers.Main) {
                _profile.value = profileEntity
            }
        }
    }

    fun getWallet() {
        viewModelScope.launch(Dispatchers.IO) {
            val walletEntity = dbRepository.getWallet()
            withContext(Dispatchers.Main) {
                _wallet.value = walletEntity
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
}