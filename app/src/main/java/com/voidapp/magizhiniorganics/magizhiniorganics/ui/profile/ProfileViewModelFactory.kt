package com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirebaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeViewModel

class ProfileViewModelFactory (
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return ProfileViewModel(dbRepository, fbRepository) as T
    }
}