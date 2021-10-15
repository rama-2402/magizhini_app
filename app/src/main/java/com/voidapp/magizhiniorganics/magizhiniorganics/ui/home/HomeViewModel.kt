package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import android.util.Log
import androidx.lifecycle.*
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.WalletEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel (
    private val repository: DatabaseRepository,
    private val firestoreRepository: FirestoreRepository
): ViewModel() {

    var homeListener: HomeListener? = null

    fun getAllBanners() = repository.getAllBanners()

    fun getALlCategories() = repository.getAllProductCategories()

    fun selectedCategory(category: String) {
        homeListener?.displaySelectedCategory(category)
    }

    fun signOut() {
        firestoreRepository.signOut()
    }

    fun onDataTransactionFailure(message: String){
        homeListener?.onDataTransactionFailure(message)
    }

    fun getWallet() = viewModelScope.launch (Dispatchers.IO) {
        val wallet = repository.getWallet()
        if (wallet == null) {
            val walletEntity = WalletEntity(
                id = firestoreRepository.uid(),
                amount = 0f,
                reminder = true,
                nextRecharge = 0,
                listOf()
            )
            repository.upsertWallet(walletEntity)
        }
    }

    fun updateRecentPurchases() = viewModelScope.launch(Dispatchers.IO) {
        val recentPurchaseIDs = repository.getProfileData()!!.purchaseHistory
        val subscriptionIDs = repository.getProfileData()!!.subscriptions
        firestoreRepository.updateRecentPurchases(recentPurchaseIDs, subscriptionIDs)
    }
}