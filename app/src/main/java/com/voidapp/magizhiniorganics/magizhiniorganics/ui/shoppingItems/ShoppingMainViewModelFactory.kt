package com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository

class ShoppingMainViewModelFactory(
    private val repository: DatabaseRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ShoppingMainViewModel(repository, firestoreRepository) as T
    }
}