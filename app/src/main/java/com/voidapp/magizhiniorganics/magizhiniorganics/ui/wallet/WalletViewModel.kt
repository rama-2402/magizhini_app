package com.voidapp.magizhiniorganics.magizhiniorganics.ui.walletimport androidx.lifecycle.LiveDataimport androidx.lifecycle.MutableLiveDataimport androidx.lifecycle.ViewModelimport androidx.lifecycle.viewModelScopeimport com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfileimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Walletimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constantsimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelperimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtilimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResultimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.toUserProfileimport kotlinx.coroutines.Dispatchersimport kotlinx.coroutines.flow.MutableStateFlowimport kotlinx.coroutines.flow.StateFlowimport kotlinx.coroutines.launchimport kotlinx.coroutines.withContextclass WalletViewModel(    val dbRepository: DatabaseRepository,    val fbRepository: FirestoreRepository): ViewModel() {    var navigateToPage: String = ""    private var _wallet: MutableLiveData<Wallet> = MutableLiveData()    val wallet: LiveData<Wallet> = _wallet    private var _toast: MutableLiveData<String> = MutableLiveData()    val toast: LiveData<String> = _toast    private var _transactions: MutableLiveData<List<TransactionHistory>> = MutableLiveData()    val transactions: LiveData<List<TransactionHistory>> = _transactions    private var _profile: MutableLiveData<UserProfile> = MutableLiveData()    val profile: LiveData<UserProfile> = _profile    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(        NetworkResult.Empty)    val status: StateFlow<NetworkResult> = _status    fun emptyResult() {        _status.value = NetworkResult.Empty    }    fun getWallet(id: String) = viewModelScope.launch(Dispatchers.IO) {        _status.value = fbRepository.getWallet(id)    }    fun getTransactions(id: String) = viewModelScope.launch (Dispatchers.IO) {        val transactions = fbRepository.getTransactions(id)        withContext(Dispatchers.Main) {            _transactions.value = transactions        }    }    fun getUserProfileData() = viewModelScope.launch(Dispatchers.IO) {        val profile = dbRepository.getProfileData()!!        withContext(Dispatchers.Main) {            _profile.value = profile.toUserProfile()        }    }    fun makeTransactionFromWallet(amount: Float, id: String, orderID: String, transactionType: String) = viewModelScope.launch {        if (fbRepository.makeTransactionFromWallet(amount, id, transactionType)) {            TransactionHistory (                id,                System.currentTimeMillis(),                TimeUtil().getMonth(),                TimeUtil().getYear().toLong(),                amount,                id,                orderID,                Constants.SUCCESS,                Constants.ADD_MONEY,                orderID            ).let {                _status.value = NetworkResult.Success("transaction", it)            }        } else {            _status.value = NetworkResult.Failed("transaction", "Server Error. Failed to make transaction from Wallet. Try other payment method")        }    }    fun updateTransaction(transaction: TransactionHistory) = viewModelScope.launch (Dispatchers.IO) {        _status.value = fbRepository.updateTransaction(transaction, "Wallet recharge")    }}