package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository

class HomeViewModelFactory(
    private val repository: DatabaseRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return HomeViewModel(repository, firestoreRepository) as T
    }
}