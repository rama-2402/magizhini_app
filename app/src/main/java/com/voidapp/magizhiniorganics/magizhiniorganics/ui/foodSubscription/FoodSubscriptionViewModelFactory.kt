package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.FoodSubscriptionUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository

class FoodSubscriptionViewModelFactory(
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository,
    private val foodSubscriptionUseCase: FoodSubscriptionUseCase
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FoodSubscriptionViewModel(fbRepository, dbRepository, foodSubscriptionUseCase) as T
    }
}