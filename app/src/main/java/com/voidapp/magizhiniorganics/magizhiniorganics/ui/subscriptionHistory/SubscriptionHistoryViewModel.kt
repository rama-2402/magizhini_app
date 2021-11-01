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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

class SubscriptionHistoryViewModel(
    val fbRepository: FirestoreRepository,
    val dbRepository: DatabaseRepository
): ViewModel() {

    private var _activeSubs: MutableLiveData<List<SubscriptionEntity>> = MutableLiveData()
    val activeSub: LiveData<List<SubscriptionEntity>> = _activeSubs
    private var _error: MutableLiveData<String> = MutableLiveData()
    val error: LiveData<String> = _error
    private var _subStatus: MutableLiveData<String> = MutableLiveData()
    val subStatus: LiveData<String> = _subStatus
    private var _wallet: MutableLiveData<Wallet> = MutableLiveData()
    val wallet: LiveData<Wallet> = _wallet
    private var _renewSub: MutableLiveData<SubscriptionEntity> = MutableLiveData()
    val renewSub: LiveData<SubscriptionEntity> = _renewSub
    private var _cancelSub: MutableLiveData<SubscriptionEntity> = MutableLiveData()
    val cancelSub: LiveData<SubscriptionEntity> = _cancelSub
    private var _profile: MutableLiveData<UserProfileEntity> = MutableLiveData()
    val profile: LiveData<UserProfileEntity> = _profile
    private var _walletTransactionStatus: MutableLiveData<Boolean> = MutableLiveData()
    val walletTransactionStatus: LiveData<Boolean> = _walletTransactionStatus

    fun getProfile() = viewModelScope.launch(Dispatchers.IO) {
        val profile = dbRepository.getProfileData()!!
        withContext(Dispatchers.Main) {
            _profile.value = profile
        }
    }

    fun getSubscriptions(status: String) = viewModelScope.launch(Dispatchers.IO) {
        val subs = dbRepository.getAllSubscriptionsHistory(status)
        withContext(Dispatchers.Main) {
            _activeSubs.value = subs
        }
    }

    fun cancelSubscription(sub: SubscriptionEntity) = viewModelScope.launch(Dispatchers.IO) {
        sub.status = Constants.CANCELLED
        if (fbRepository.cancelSubscription(sub)) {
            try {
                val cancelSub = async { dbRepository.cancelSubscription(sub) }
                val removeActiveSubFromProfile = async { removeActiveSubFromProfile(sub) }

                cancelSub.await()
                removeActiveSubFromProfile.await()

                withContext(Dispatchers.Main) {
                    _subStatus.value = "complete"
                }
            } catch (e: Exception) {}
        } else {
            _error.value = "Server Error! Failed to cancel subscription"
        }
    }

    fun cancelDate(sub: SubscriptionEntity ,cancelDate: Long): Boolean {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                if (fbRepository.addCancellationDates(sub, cancelDate)) {
                    sub.cancelledDates.add(cancelDate)
                    dbRepository.upsertSubscription(sub)
                }
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private suspend fun removeActiveSubFromProfile(sub: SubscriptionEntity) = withContext(Dispatchers.IO) {
        dbRepository.cancelActiveSubscription(sub.id)
    }

    fun getWallet(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val wallet = fbRepository.getWallet(id)
            withContext(Dispatchers.Main) {
                _wallet.value = wallet
            }
        }
    }

    suspend fun checkWalletForBalance(amount: Float, id: String): Boolean {
        return try {
            val amountInWallet = fbRepository.getWalletAmount(id)
            amountInWallet >= amount
        } catch (e: Exception) {
            false
        }
    }

    suspend fun calculateBalance(sub: SubscriptionEntity) = withContext(Dispatchers.Default) {
        val singleDayPrice = sub.estimateAmount/30f
        val totalRefundAmount = if (System.currentTimeMillis() > sub.startDate) {
            // we add plus one because the day will counted if exactly at 00:00. any timestamp past that point day will not be calculated. So to include that we add one to the total
            val remainingDays = (sub.endDate - System.currentTimeMillis()) / (1000*60*60*24)
            val cancelledDates = mutableListOf<Long>()
            for (date in sub.cancelledDates.indices) {
                if (sub.cancelledDates[date] < System.currentTimeMillis()) {
                    cancelledDates.add(sub.cancelledDates[date])
                }
            }
            val refundDays = cancelledDates.size + sub.notDeliveredDates.size + remainingDays + 1
            refundDays * singleDayPrice
        } else {
            30f * singleDayPrice
        }
        if (fbRepository.makeTransactionFromWallet(totalRefundAmount, sub.customerID, "Add")) {
            withContext(Dispatchers.Main) {
                _walletTransactionStatus.value = true
            }
        } else {
            withContext(Dispatchers.Main) {
                _walletTransactionStatus.value = false
            }
        }
    }

    suspend fun makeTransactionFromWallet(amount: Float, id: String, orderID: String): String {
        if (fbRepository.makeTransactionFromWallet(amount, id, "Remove")) {
            val transaction = TransactionHistory (
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
            )
            return fbRepository.updateTransaction(transaction)
        } else {
            return "failed"
        }
    }

    fun renewSelectedSubscription(sub: SubscriptionEntity, renewal: Boolean) {
        if (renewal) {
            _renewSub.value = sub
        } else {
            _cancelSub.value = sub
        }
    }

    suspend fun renewSubscription(
        id: String,
        monthYear: String,
        newDate: Long
    ): Boolean {
            return try {
                if (fbRepository.renewSubscription(id, monthYear, newDate)) {
                    dbRepository.updateSubscription(id, newDate)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
}