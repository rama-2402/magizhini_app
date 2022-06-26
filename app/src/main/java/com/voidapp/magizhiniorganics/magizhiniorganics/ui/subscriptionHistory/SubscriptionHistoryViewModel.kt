package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.GlobalTransaction
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistory
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ACTIVE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ADD_MONEY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALTERNATE_DAYS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CANCELLED
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CUSTOM_DAYS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.MONTHLY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SINGLE_DAY_LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SubscriptionHistoryViewModel(
    val fbRepository: FirestoreRepository,
    val dbRepository: DatabaseRepository
): ViewModel() {

    var subscription: SubscriptionEntity? = null
    val subscriptionsList: MutableList<SubscriptionEntity> = mutableListOf()
    var subPosition: Int = 0

    var liveWallet: Wallet? = null
    var currentUserProfile: UserProfileEntity? = null

    private var updatedCustomDaysForSubRenewal: Int = 0
    private val updatedCancelledDaysForSubRenewal = arrayListOf<Long>()

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(
        NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate
    private val _uiEvent: MutableLiveData<UIEvent> = MutableLiveData()
    val uiEvent: LiveData<UIEvent> = _uiEvent

    fun setEmptyUiEvent() {
        _uiEvent.value = UIEvent.EmptyUIEvent
    }

    fun setEmptyStatus() {
        _uiUpdate.value = UiUpdate.Empty
    }

    fun getProfile() = viewModelScope.launch(Dispatchers.IO) {
        try {
            dbRepository.getProfileData()?.let { profile ->
                getWallet(profile.id)
                withContext(Dispatchers.Main) {
                    currentUserProfile = profile
                }
            }
        } catch (e: IOException) {
            fbRepository.logCrash("subHistoryVM: getting profile data", e.message.toString())
        }
    }

    private suspend fun getWallet(id: String) {
        when(val walletStatus = fbRepository.getWallet(id)) {
            is NetworkResult.Success -> liveWallet = walletStatus.data as Wallet
            is NetworkResult.Failed -> {
                withContext(Dispatchers.Main) {
                    _uiEvent.value =
                        UIEvent.SnackBar(walletStatus.data as String, true)
                }
            }
            else -> Unit
        }
    }

    fun getSubscriptions(status: String) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val subs = dbRepository.getAllSubscriptionsHistory(status)
            subscriptionsList.clear()
            subscriptionsList.addAll(subs)
            withContext(Dispatchers.Main) {
                _uiUpdate.value =
                    UiUpdate.PopulateSubscriptions(subs.map { it.copy() } as MutableList<SubscriptionEntity>, null)
            }
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("subHistoryVM: getting subscriptions", it) }
            withContext(Dispatchers.Main) {
                _uiUpdate.value =
                    UiUpdate.PopulateSubscriptions(null, "Failed to fetch subscriptions. Try again later")
            }
        }

    }

    fun cancelSubscription() = viewModelScope.launch {
        subscription?.let {
            _uiUpdate.value =
                UiUpdate.ShowLoadStatusDialog("Cancelling your Subscription...", "upload")
            it.status = CANCELLED
            val status = fbRepository.cancelSubscription(it)
            delay(1000)
            when(status) {
                is NetworkResult.Success -> _uiUpdate.value = UiUpdate.SubCancelStatus(true, null)
                is NetworkResult.Failed -> {
                    _uiUpdate.value = UiUpdate.SubCancelStatus(false, status.message)
                    delay(1000)
                    _uiUpdate.value = UiUpdate.DismissLoadStatusDialog
                }
                else -> Unit
            }
        }
    }

    suspend fun cancelDate(sub: SubscriptionEntity ,cancelDate: Long) =
        withContext(Dispatchers.IO) {
              try {
                if (fbRepository.addCancellationDates(sub, cancelDate)) {
                    sub.cancelledDates.add(cancelDate)
                    dbRepository.upsertSubscription(sub)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

    private suspend fun calculateBalance(sub: SubscriptionEntity): Float = withContext(Dispatchers.Default) {
        return@withContext when(sub.subType) {
            CUSTOM_DAYS -> {
                var totalSubDays = 0
                val dayFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
                for (dateLong in sub.startDate..sub.endDate step 86400000) {
                    if (sub.customDates.contains(dayFormat.format(dateLong))) {
                        totalSubDays += 1
                    }
                }
                val totalAmountPaid = totalSubDays * sub.basePay
                val amountForDelivery = sub.deliveredDates.size * sub.basePay
                totalAmountPaid - amountForDelivery
            }
            else -> {
                val totalSubPeriod = ((sub.endDate - sub.startDate) + 86400000) / 86400000
                val totalSubscriptionRenewals = totalSubPeriod / 30
                val amountForDelivery = sub.deliveredDates.size * sub.basePay
                (sub.estimateAmount * totalSubscriptionRenewals) - amountForDelivery
            }
        }
    }

    fun initiateWalletTransaction(amount: Float, id: String, orderID: String, transactionType: String) = viewModelScope.launch {
        _uiUpdate.value = UiUpdate.ShowLoadStatusDialog("Initiating Payment from Wallet...", "transaction")
        makeTransactionFromWallet(amount, id, orderID, transactionType)
    }

    fun refundSubBalance() = viewModelScope.launch {
        delay(1000)
        _uiUpdate.value = UiUpdate.UpdateLoadStatusDialog("Wallet Refund Initiated... please wait", "transaction")

        val refundAmountJob = async { calculateBalance(subscription!!) }
        val refundAmount = refundAmountJob.await()

        makeTransactionFromWallet(refundAmount, subscription!!.customerID, subscription!!.id, "Add")
    }

    private suspend fun makeTransactionFromWallet(amount: Float, id: String, orderID: String, transactionType: String) {
        if (fbRepository.makeTransactionFromWallet(amount, id, transactionType)) {
            TransactionHistory (
                orderID,
                System.currentTimeMillis(),
                TimeUtil().getMonth(),
                TimeUtil().getYear().toLong(),
                amount,
                id,
                id,
                Constants.SUCCESS,
                Constants.SUBSCRIPTION,
                orderID
            ).let {
                if (transactionType == "Add") {
                    it.purpose = ADD_MONEY
                }
                val transactionStatus = fbRepository.updateTransaction(
                    it,
                    if (transactionType == "Add") {
                        "Subscription Cancellation Refund"
                    } else {
                        "Subscription (ID: $orderID) Renewal "
                    }
                )
                when(transactionStatus) {
                    is NetworkResult.Success -> {
                        delay(1000)
                        _uiUpdate.value =
                            UiUpdate.TransactionStatus(true, null, "renew")
                        if (subscription!!.status == CANCELLED) {
                            delay(1800)
                            _uiUpdate.value = UiUpdate.DismissLoadStatusDialog
                        }
                    }
                    is NetworkResult.Failed -> {
                        _uiUpdate.value =
                            UiUpdate.TransactionStatus(false, transactionStatus.message, "fail")
                        delay(1000)
                        _uiUpdate.value = UiUpdate.DismissLoadStatusDialog

                    }
                    else -> Unit
                }
            }
        } else {
            _uiUpdate.value =
                UiUpdate.TransactionStatus(false, "Server Error. Failed to make transaction from Wallet. Try other payment method", "fail")
            delay(1000)
            _uiUpdate.value = UiUpdate.DismissLoadStatusDialog
        }
    }

    suspend fun renewSubscription(sub: SubscriptionEntity, transactionID: String) {
        sub.cancelledDates.addAll(updatedCancelledDaysForSubRenewal)
        if (sub.paymentMode == "Online") {
            currentUserProfile?.let { profile ->
                GlobalTransaction(
                    id = "",
                    userID = profile.id,
                    userName = profile.name,
                    userMobileNumber = profile.phNumber,
                    transactionID = transactionID,
                    transactionType = "Online Payment",
                    transactionAmount = sub.estimateAmount,
                    transactionDirection = SUBSCRIPTION,
                    timestamp = System.currentTimeMillis(),
                    transactionReason = "${sub.productName} ${sub.variantName} Subscription Renewal Transaction"
                ).let {
                    fbRepository.createGlobalTransactionEntry(it)
                }
            }
        }
        if (sub.subType == MONTHLY) {
            val status = fbRepository.renewSubscription(sub, arrayListOf())
            updateRenewalStatus(status)
        } else {
            val status = fbRepository.renewSubscription(sub, updatedCancelledDaysForSubRenewal)
            updateRenewalStatus(status)
        }
    }

    private fun updateRenewalStatus(status: NetworkResult) = viewModelScope.launch {
        when(status) {
            is NetworkResult.Success -> {
                delay(1000)
                _uiUpdate.value =
                    UiUpdate.SubRenewalStatus(true, null)
                delay(1800)
                _uiUpdate.value =
                    UiUpdate.DismissLoadStatusDialog
            }
            is NetworkResult.Failed -> {
                delay(1000)
                _uiUpdate.value =
                    UiUpdate.SubRenewalStatus(false, status.message)
                delay(1000)
                _uiUpdate.value =
                    UiUpdate.DismissLoadStatusDialog
            }
            else -> Unit
        }
    }

    suspend fun getCancellationDays(sub: SubscriptionEntity) : ArrayList<Long> = withContext (Dispatchers.Default) {
        val singeDateDifference: Long = 86400000
        val cancelledDates = arrayListOf<Long>()
        var startDate = sub.endDate + SINGLE_DAY_LONG
        updatedCancelledDaysForSubRenewal.clear()
        if (sub.subType == ALTERNATE_DAYS) {
            val endDateLimit = sub.endDate - (2 * SINGLE_DAY_LONG)
            sub.cancelledDates.forEach {
                if (it >= endDateLimit) {
                    if (TimeUtil().getCustomDate(dateLong = it) == TimeUtil().getCustomDate(dateLong = sub.endDate)) {
                        startDate = sub.endDate
                    }
                }
            }
            for (i in 1..15) {
                startDate += (2 * singeDateDifference)
                cancelledDates.add(startDate)
            }
            updatedCancelledDaysForSubRenewal.addAll(cancelledDates)
            return@withContext cancelledDates
        } else {
            for (i in 1..30) {
                startDate += singeDateDifference
                val day = SimpleDateFormat("EEEE", Locale.ENGLISH).format(startDate)
                if (!sub.customDates.contains(day)) {
                    cancelledDates.add(startDate)
                }
            }
            updatedCancelledDaysForSubRenewal.addAll(cancelledDates)
            updatedCustomDaysForSubRenewal = 30 - cancelledDates.size
            return@withContext cancelledDates
        }
    }

    suspend fun getUpdatedEstimateForNewSubJob(sub: SubscriptionEntity): Float = withContext(Dispatchers.Default) {
        dbRepository.getProductWithIdForUpdate(sub.productID)?.let {
            if (it.status != ACTIVE || it.productType != SUBSCRIPTION) {
                //THIS CODE CAN BE USED IF WE WANT THE RENEWED SUB TO HAVE NEW UPDATED PRICES
//            var variantPosition = 0
//            for (i in updatedProduct.variants.indices) {
//                val variantName = "${updatedProduct.variants[i].variantName} ${updatedProduct.variants[i].variantType}"
//                if (variantName == sub.variantName) {
//                    variantPosition = i
//                }
//            }
//            _status.value = NetworkResult.Success("basePay", updatedProduct.variants[variantPosition].variantPrice)

                var variantPosition = 1000
                for (position in it.variants.indices) {
                    if (sub.variantName == "${it.variants[position].variantName} ${it.variants[position].variantType}") {
                        variantPosition = position
                        break
                    }
                }

                if (variantPosition == 1000) {
                    return@withContext 0f
                }

                val estimateAmount = when(sub.subType) {
                    CUSTOM_DAYS -> (it.variants[variantPosition].variantPrice * updatedCustomDaysForSubRenewal).toFloat()
                    ALTERNATE_DAYS -> (it.variants[variantPosition].variantPrice * 15).toFloat()
                    else -> (it.variants[variantPosition].variantPrice * 30).toFloat()
                }
                return@withContext estimateAmount
            } else {
                return@withContext 0f
            }
        } ?: return@withContext 0f
    }

    fun getHowToVideo(where: String) = viewModelScope.launch {
        val url = fbRepository.getHowToVideo(where)
        _uiUpdate.value = UiUpdate.HowToVideo(url)
    }

    sealed class UiUpdate {
        //sub
        data class PopulateSubscriptions(val subscriptions: MutableList<SubscriptionEntity>?, val message: String?): UiUpdate()
        data class UpdateSubscription(val subscription: SubscriptionEntity, val position: Int): UiUpdate()
        //load status dialog
        data class ShowLoadStatusDialog(val message: String?, val data: String?): UiUpdate()
        data class UpdateLoadStatusDialog(val message: String?, val data: String?): UiUpdate()
        object DismissLoadStatusDialog: UiUpdate()
        //sub renewal
        data class SubRenewalStatus(val status: Boolean, val message: String?): UiUpdate()
        //sub cancel
        data class SubCancelStatus(val status: Boolean, val message: String?): UiUpdate()
        //wallet
        data class TransactionStatus(val status: Boolean, val message: String?, val data: String?): UiUpdate()
        //howto
        data class HowToVideo(val url: String): UiUpdate()

        object Empty : UiUpdate()
    }
}