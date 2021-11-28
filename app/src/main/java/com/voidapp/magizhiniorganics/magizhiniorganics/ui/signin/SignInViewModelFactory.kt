package com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirebaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository

class SignInViewModelFactory(
    private val fbRepository: FirestoreRepository,
    private val firebaseRepository: FirebaseRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SignInViewModel(fbRepository, firebaseRepository) as T
    }
}