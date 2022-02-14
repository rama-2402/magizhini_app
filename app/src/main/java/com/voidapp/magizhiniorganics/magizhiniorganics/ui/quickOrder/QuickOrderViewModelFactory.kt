package com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.QuickOrderUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryViewModel

class QuickOrderViewModelFactory(
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository,
    private val quickOrderUseCase: QuickOrderUseCase
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return QuickOrderViewModel(fbRepository, dbRepository, quickOrderUseCase) as T
    }
}