package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory

import android.icu.util.TimeUnit
import android.provider.SyncStateContract
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aminography.primecalendar.PrimeCalendar
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Subscription
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistory
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

class SubscriptionHistoryViewModel(
    val fbRepository: FirestoreRepository,
    val dbRepository: DatabaseRepository
): ViewModel() {

    var liveWallet: Wallet = Wallet()
    var currentUserProfile: UserProfileEntity = UserProfileEntity()

    private var _activeSubs: MutableLiveData<MutableList<SubscriptionEntity>> = MutableLiveData()
    val activeSub: LiveData<MutableList<SubscriptionEntity>> = _activeSubs

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(
        NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    fun getProfile() = viewModelScope.launch(Dispatchers.IO) {
        val profile = dbRepository.getProfileData()!!
        getWallet(profile.id)
        withContext(Dispatchers.Main) {
            currentUserProfile = profile
        }
    }

    private suspend fun getWallet(id: String) {
        val suspendProfile = dbRepository.getProfileData()!!
        Log.e("TAG", "getWallet: $suspendProfile", )
        _status.value = fbRepository.getWallet(id)
    }

    fun getSubscriptions(status: String) = viewModelScope.launch(Dispatchers.IO) {
        val subs = dbRepository.getAllSubscriptionsHistory(status)
        withContext(Dispatchers.Main) {
            _activeSubs.value = subs as MutableList<SubscriptionEntity>
        }
    }

    fun cancelSubscription(sub: SubscriptionEntity) = viewModelScope.launch(Dispatchers.IO) {
        sub.status = Constants.CANCELLED
        _status.value = fbRepository.cancelSubscription(sub)
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


    fun checkWalletForBalance(amount: Float): Boolean {
        return liveWallet.amount >= amount
    }

    suspend fun calculateBalance(sub: SubscriptionEntity): Float = withContext(Dispatchers.Default) {
        val singleDayPrice = sub.estimateAmount/30f
        if (System.currentTimeMillis() > sub.startDate) {
            // we add plus one because the day will counted if exactly at 00:00. any timestamp past that point day will not be calculated. So to include that we add one to the total
            val remainingDays = (sub.endDate - System.currentTimeMillis()) / (1000*60*60*24)
            val cancelledDates = mutableListOf<Long>()
            for (date in sub.cancelledDates.indices) {
                if (sub.cancelledDates[date] < System.currentTimeMillis()) {
                    cancelledDates.add(sub.cancelledDates[date])
                }
            }
            val refundDays = cancelledDates.size + sub.notDeliveredDates.size + remainingDays + 1
            return@withContext (refundDays * singleDayPrice)
        } else {
            return@withContext (30f * singleDayPrice)
        }
    }

    suspend fun makeTransactionFromWallet(amount: Float, id: String, orderID: String, transactionType: String) {
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
                Constants.PURCHASE,
                orderID
            ).let {
                _status.value = NetworkResult.Success("transaction", it)
            }
        } else {
            _status.value = NetworkResult.Failed("transaction", "Server Error. Failed to make transaction from Wallet. Try other payment method")
        }
    }

    fun updateTransaction(transaction: TransactionHistory) = viewModelScope.launch (Dispatchers.IO) {
        _status.value = fbRepository.updateTransaction(transaction)
    }

    suspend fun renewSubscription(
        id: String,
        monthYear: String,
        newDate: Long
    ) {
        _status.value = fbRepository.renewSubscription(id, monthYear, newDate)
    }
}