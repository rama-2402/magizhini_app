package com.voidapp.magizhiniorganics.magizhiniorganics.ui.walletimport androidx.lifecycle.LiveDataimport androidx.lifecycle.MutableLiveDataimport androidx.lifecycle.ViewModelimport androidx.lifecycle.viewModelScopeimport com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfileimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Walletimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constantsimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtilimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.toUserProfileimport kotlinx.coroutines.Dispatchersimport kotlinx.coroutines.launchimport kotlinx.coroutines.withContextclass WalletViewModel(    val dbRepository: DatabaseRepository,    val fbRepository: FirestoreRepository): ViewModel() {    private var _wallet: MutableLiveData<Wallet> = MutableLiveData()    val wallet: LiveData<Wallet> = _wallet    private var _transactions: MutableLiveData<List<TransactionHistory>> = MutableLiveData()    val transactions: LiveData<List<TransactionHistory>> = _transactions    private var _profile: MutableLiveData<UserProfile> = MutableLiveData()    val profile: LiveData<UserProfile> = _profile    fun getWallet(id: String) = viewModelScope.launch(Dispatchers.IO) {//        val wallet = fbRepository.getWallet(id)//        withContext(Dispatchers.Main) {//            _wallet.value = wallet//        }    }    fun getTransactions(id: String) = viewModelScope.launch (Dispatchers.IO) {        val transactions = fbRepository.getTransactions(id)        withContext(Dispatchers.Main) {            _transactions.value = transactions        }    }    fun getUserProfileData() = viewModelScope.launch(Dispatchers.IO) {        val profile = dbRepository.getProfileData()!!        withContext(Dispatchers.Main) {            _profile.value = profile.toUserProfile()        }    }    suspend fun createTransaction(amount: Float, id: String, orderID: String): String {        if (fbRepository.makeTransactionFromWallet(amount, id, "Add")) {            val transaction = TransactionHistory (                id,                System.currentTimeMillis(),                TimeUtil().getMonth(),                TimeUtil().getYear().toLong(),                amount,                id,                orderID,                Constants.SUCCESS,                Constants.ADD_MONEY,                orderID            )            return ""//            return fbRepository.updateTransaction(transaction)        } else {            return "failed"        }    }}