package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.SubscriptionUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository

class SubscriptionProductViewModelFactory(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository,
    private val subscriptionUseCase: SubscriptionUseCase
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SubscriptionProductViewModel(dbRepository, fbRepository, subscriptionUseCase) as T
    }
}