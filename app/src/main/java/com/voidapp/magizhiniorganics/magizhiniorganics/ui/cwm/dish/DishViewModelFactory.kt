package com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository

class DishViewModelFactory(
    private val repository: DatabaseRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DishViewModel(repository, firestoreRepository) as T
    }
}