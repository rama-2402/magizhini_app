package com.voidapp.magizhiniorganics.magizhiniorganics.ui.walletimport androidx.lifecycle.ViewModelimport androidx.lifecycle.ViewModelProviderimport com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepositoryclass WalletViewModelFactory(    private val dbRepository: DatabaseRepository,    private val fbRepository: FirestoreRepository): ViewModelProvider.NewInstanceFactory() {    override fun <T : ViewModel> create(modelClass: Class<T>): T {        return WalletViewModel(dbRepository, fbRepository) as T    }}