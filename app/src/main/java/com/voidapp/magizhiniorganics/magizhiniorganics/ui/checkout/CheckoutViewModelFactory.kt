package com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.QuickOrderUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository

class CheckoutViewModelFactory(
    private val repository: DatabaseRepository,
    private val firestoreRepository: FirestoreRepository,
    private val quickOrderUseCase: QuickOrderUseCase
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CheckoutViewModel(repository, firestoreRepository, quickOrderUseCase) as T
    }
}