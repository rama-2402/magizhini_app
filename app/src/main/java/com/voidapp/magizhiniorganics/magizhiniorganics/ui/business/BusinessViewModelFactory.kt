package com.voidapp.magizhiniorganics.magizhiniorganics.ui.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirebaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatViewModel

class BusinessViewModelFactory(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return BusinessViewModel(dbRepository, fbRepository) as T
    }
}