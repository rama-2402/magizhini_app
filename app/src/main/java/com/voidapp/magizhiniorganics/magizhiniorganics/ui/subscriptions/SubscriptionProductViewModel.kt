package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions

import androidx.lifecycle.ViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity

class SubscriptionProductViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {
    var mProductID = ""
}