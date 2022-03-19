package com.voidapp.magizhiniorganics.magizhiniorganics.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository

class ProductViewModelFactory(
    private val repository: DatabaseRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProductViewModel(repository, firestoreRepository) as T
    }
}