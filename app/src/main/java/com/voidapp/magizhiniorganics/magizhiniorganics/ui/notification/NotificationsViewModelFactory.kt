package com.voidapp.magizhiniorganics.magizhiniorganics.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel

class NotificationsViewModelFactory(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return NotificationsViewModel(dbRepository, fbRepository) as T
    }
}