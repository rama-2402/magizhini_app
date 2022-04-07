package com.voidapp.magizhiniorganics.magizhiniorganics.ui.walletimport androidx.lifecycle.LiveDataimport androidx.lifecycle.MutableLiveDataimport androidx.lifecycle.ViewModelimport androidx.lifecycle.viewModelScopeimport com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.PushNotificationUseCaseimport com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntityimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Messagesimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfileimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Walletimport com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryViewModelimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constantsimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET_PAGEimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtilimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResultimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEventimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.toUserProfileimport kotlinx.coroutines.Dispatchersimport kotlinx.coroutines.delayimport kotlinx.coroutines.flow.MutableStateFlowimport kotlinx.coroutines.flow.StateFlowimport kotlinx.coroutines.launchimport kotlinx.coroutines.withContextimport net.yslibrary.android.keyboardvisibilityevent.util.UIUtilimport java.time.Monthclass WalletViewModel(    val dbRepository: DatabaseRepository,    val fbRepository: FirestoreRepository): ViewModel() {    val transactions: MutableList<TransactionHistory> = mutableListOf()    var userID: String = ""    var moneyToAddInWallet: Float? = null    var filteredMonth: String = TimeUtil().getMonth(dateLong = System.currentTimeMillis())    var filteredYear: Long = TimeUtil().getYear(dateLong = System.currentTimeMillis()).toLong()    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()    val uiUpdate: LiveData<UiUpdate> = _uiUpdate    fun setEmptyStatus() {        _uiUpdate.value = UiUpdate.EmptyUI    }    fun getWallet() = viewModelScope.launch {        when(val result = fbRepository.getWallet(userID)) {            is NetworkResult.Success -> {                _uiUpdate.value = UiUpdate.PopulateWalletData(result.data as Wallet, null)            }            is NetworkResult.Failed -> {                _uiUpdate.value = UiUpdate.PopulateWalletData(null, result.data.toString())            }            else -> Unit        }    }    fun getTransactions() = viewModelScope.launch {        if (transactions.isNullOrEmpty()) {            fbRepository.getTransactions(userID).let { it ->                transactions.addAll(it)                filterTransactions()            }        } else {            filterTransactions()        }    }    fun filterTransactions() = viewModelScope.launch(Dispatchers.Default) {        val filteredTransactions = mutableListOf<TransactionHistory>()        for (transaction in transactions) {            if (transaction.month == filteredMonth && transaction.year == filteredYear) {                filteredTransactions.add(transaction)            }        }        filteredTransactions.sortBy {            it.timestamp        }        withContext(Dispatchers.Main) {            _uiUpdate.value = UiUpdate.PopulateTransactions(filteredTransactions.map { it.copy() }, null)        }    }    fun getUserProfileData() = viewModelScope.launch(Dispatchers.IO) {         try {             dbRepository.getProfileData()?.let { profile ->                 withContext(Dispatchers.Main) {                     _uiUpdate.value = UiUpdate.UpdateUserProfileData(profile)                 }             } ?: withContext(Dispatchers.Main) {                 _uiUpdate.value = UiUpdate.UpdateUserProfileData(null)             }         } catch (e: Exception) {             e.message?.let { fbRepository.logCrash("wallet: fetching user profile data", it) }             _uiUpdate.value = UiUpdate.UpdateUserProfileData(null)         }    }    fun makeTransactionFromWallet(        amount: Float,        id: String,        orderID: String,        transactionType: String    ) = viewModelScope.launch {        _uiUpdate.value = UiUpdate.ShowLoadStatusDialog("Adding Money to the Wallet...",  "transaction")        if (fbRepository.makeTransactionFromWallet(amount, id, transactionType)) {            delay(1000)            _uiUpdate.value = UiUpdate.UpdateLoadStatusDialog("Validating Transaction...", "validating")            TransactionHistory (                id,                System.currentTimeMillis(),                TimeUtil().getMonth(),                TimeUtil().getYear().toLong(),                amount,                id,                orderID,                Constants.SUCCESS,                Constants.ADD_MONEY,                orderID            ).let {                if (transactionType == "Add") {                    it.purpose = Constants.ADD_MONEY                }                val transactionStatus = fbRepository.updateTransaction(                    it,                    "Added Money to Wallet"                )                when(transactionStatus) {                    is NetworkResult.Success -> {                        transactions.add(it)                        delay(1000)                        _uiUpdate.value =                            UiUpdate.TransactionStatus(true, null, "success")                        delay(1000)                        PushNotificationUseCase(fbRepository).sendPushNotification(                            userID,                            "Wallet Recharge Success",                        "Your Wallet has been recharged successfully. Shop Now to get more exciting offers!",                            WALLET_PAGE                        )                        _uiUpdate.value =                            UiUpdate.DismissDialog(true, null)                    }                    is NetworkResult.Failed -> {                        _uiUpdate.value =                            UiUpdate.TransactionStatus(false, transactionStatus.message, "fail")                        delay(1000)                        _uiUpdate.value =                            UiUpdate.DismissDialog(false, transactionStatus.message)                    }                    else -> Unit                }            }        } else {            _uiUpdate.value =                UiUpdate.TransactionStatus(false, "Server Error. Failed to make transaction from Wallet. Try other payment method", "fail")        }    }    sealed class UiUpdate {        data class PopulateTransactions(val transactions: List<TransactionHistory>, val messages: String?): UiUpdate()        data class PopulateWalletData(val wallet: Wallet?, val message: String?): UiUpdate()        data class UpdateUserProfileData(val profile: UserProfileEntity?): UiUpdate()        //wallet transaction status        data class TransactionStatus(val status: Boolean, val message: String?, val data: String?): UiUpdate()        //updating the status dialog        data class ShowLoadStatusDialog(val message: String?, val data: String?): UiUpdate()        data class UpdateLoadStatusDialog(val message: String?, val data: String?): UiUpdate()        data class DismissDialog(val isSuccessful: Boolean, val message: String?): UiUpdate()        object EmptyUI: UiUpdate()    }}