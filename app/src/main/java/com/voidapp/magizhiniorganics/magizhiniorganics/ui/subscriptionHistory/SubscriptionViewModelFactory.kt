package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel

class SubscriptionViewModelFactory(
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SubscriptionHistoryViewModel(fbRepository, dbRepository) as T
    }
}