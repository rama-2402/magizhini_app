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
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.GlobalTransaction
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Subscription
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistory
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ACTIVE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ADD_MONEY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALTERNATE_DAYS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CUSTOM_DAYS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.MONTHLY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList

class SubscriptionHistoryViewModel(
    val fbRepository: FirestoreRepository,
    val dbRepository: DatabaseRepository
): ViewModel() {

    var liveWallet: Wallet = Wallet()
    var currentUserProfile: UserProfileEntity = UserProfileEntity()
    private var updatedCustomDaysForSubRenewal: Int = 0
    private val updatedCancelledDaysForSubRenewal = arrayListOf<Long>()

    private var _activeSubs: MutableLiveData<MutableList<SubscriptionEntity>> = MutableLiveData()
    val activeSub: LiveData<MutableList<SubscriptionEntity>> = _activeSubs

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(
        NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    fun setEmptyStatus() {
        _status.value = NetworkResult.Empty
    }

    fun getProfile() = viewModelScope.launch(Dispatchers.IO) {
        val profile = dbRepository.getProfileData()!!
        getWallet(profile.id)
        withContext(Dispatchers.Main) {
            currentUserProfile = profile
        }
    }

    private suspend fun getWallet(id: String) {
        val suspendProfile = dbRepository.getProfileData()!!
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
        return@withContext if(sub.subType == CUSTOM_DAYS) {
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
        } else {
            val totalSubPeriod = (sub.endDate - sub.startDate + 86400000)/86400000
            val totalSubscriptionRenewals = totalSubPeriod / 30
            val amountForDelivery = sub.deliveredDates.size * sub.basePay
            (sub.estimateAmount * totalSubscriptionRenewals) - amountForDelivery
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
                Constants.SUBSCRIPTION,
                orderID
            ).let {
                if (transactionType == "Add") {
                    it.purpose = ADD_MONEY
                }
                _status.value = NetworkResult.Success("transaction", it)
            }
        } else {
            _status.value = NetworkResult.Failed("transaction", "Server Error. Failed to make transaction from Wallet. Try other payment method")
        }
    }

    fun updateTransaction(transaction: TransactionHistory) = viewModelScope.launch (Dispatchers.IO) {
        _status.value = fbRepository.updateTransaction(transaction, "Subscription Renewal")
    }

    suspend fun renewSubscription(sub: SubscriptionEntity, transactionID: String) {
        sub.cancelledDates.addAll(updatedCancelledDaysForSubRenewal)
        if (sub.paymentMode == "Online") {
            GlobalTransaction(
                id = "",
                userID = currentUserProfile.id,
                userName = currentUserProfile.name,
                userMobileNumber = currentUserProfile.phNumber,
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
        if (sub.subType == MONTHLY) {
            _status.value = fbRepository.renewSubscription(sub, arrayListOf())
        } else {
            _status.value = fbRepository.renewSubscription(sub, updatedCancelledDaysForSubRenewal)
        }
    }

    suspend fun getCancellationDays(sub: SubscriptionEntity) : ArrayList<Long> = withContext (Dispatchers.Default) {
        val singeDateDifference = 86400000
        val cancelledDates = arrayListOf<Long>()
        var startDate = sub.endDate
        updatedCancelledDaysForSubRenewal.clear()
        if (sub.subType == ALTERNATE_DAYS) {
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
        val updatedProduct = dbRepository.getProductWithIdForUpdate(sub.productID)
        updatedProduct?.let {
            if (updatedProduct.status != ACTIVE) {
                //THIS CODE CAN BE USED IF WE WANT THE RENEWED SUB TO HAVE NEW UPDATED PRICES
//            var variantPosition = 0
//            for (i in updatedProduct.variants.indices) {
//                val variantName = "${updatedProduct.variants[i].variantName} ${updatedProduct.variants[i].variantType}"
//                if (variantName == sub.variantName) {
//                    variantPosition = i
//                }
//            }
//            _status.value = NetworkResult.Success("basePay", updatedProduct.variants[variantPosition].variantPrice)

                val estimateAmount = when(sub.subType) {
//                CUSTOM_DAYS -> (updatedProduct.variants[variantPosition].variantPrice * updatedCustomDaysForSubRenewal).toFloat()
//                ALTERNATE_DAYS -> (updatedProduct.variants[variantPosition].variantPrice * 15).toFloat()
//                else -> (updatedProduct.variants[variantPosition].variantPrice * 30).toFloat()
                    CUSTOM_DAYS -> (sub.basePay * updatedCustomDaysForSubRenewal)
                    ALTERNATE_DAYS -> (sub.basePay * 15)
                    else -> (sub.basePay * 30)
                }
//            _status.value = NetworkResult.Success("basePay", estimateAmount)
                return@withContext estimateAmount
            } else {
                return@withContext 0f
            }
        } ?: return@withContext 0f

    }
}