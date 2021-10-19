package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory

import android.provider.SyncStateContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aminography.primecalendar.PrimeCalendar
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Subscription
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                dbRepository.cancelSubscription(sub)
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
                    val cancelSub = async { dbRepository.upsertSubscription(sub) }
                    val removeActiveSubFromProfile = async { removeActiveSubFromProfile(sub) }

                    cancelSub.await()
                    removeActiveSubFromProfile.await()
                }
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private suspend fun removeActiveSubFromProfile(sub: SubscriptionEntity) = withContext(Dispatchers.IO) {
        val profile = dbRepository.getProfileData()!!
        profile.subscriptions.remove(sub.id)
        dbRepository.upsertProfile(profile)
    }
}