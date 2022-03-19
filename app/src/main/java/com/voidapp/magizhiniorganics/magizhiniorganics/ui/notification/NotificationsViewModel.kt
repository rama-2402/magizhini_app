package com.voidapp.magizhiniorganics.magizhiniorganics.ui.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserNotificationEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {

    var userId: String = ""
    var allNotifications: MutableList<UserNotificationEntity> = mutableListOf()
    var clickedNotificationPosition = 0

    private var _notifications: MutableLiveData<List<UserNotificationEntity>> = MutableLiveData()
    val notifications: LiveData<List<UserNotificationEntity>> = _notifications
    private var _couponIndex: MutableLiveData<Int> = MutableLiveData()
    val couponIndex: LiveData<Int> = _couponIndex
    private var _profile: MutableLiveData<UserProfileEntity> = MutableLiveData()
    val profile: LiveData<UserProfileEntity> = _profile

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(
        NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    fun setEmptyStatus() {
        _status.value = NetworkResult.Empty
    }

    fun getAllNotifications() = viewModelScope.launch(Dispatchers.IO) {
        _status.value = NetworkResult.Loading("")
        val notifications = dbRepository.getAllNotifications()
        withContext(Dispatchers.Main) {
            notifications?.let { _notifications.value = it } ?: listOf<UserNotificationEntity>()
        }
    }

    suspend fun getProductByID(id: String): ProductEntity? = withContext(Dispatchers.IO) {
        return@withContext dbRepository.getProductWithIdForUpdate(id)
    }

    suspend fun getCategoryName(id: String): String? = withContext(Dispatchers.IO) {
        return@withContext dbRepository.getCategoryByID(id)
    }

    fun deleteNotification() = viewModelScope.launch {
        _status.value = fbRepository.deleteNotification(allNotifications[clickedNotificationPosition])
    }

    fun clearAllNotifications() = viewModelScope.launch {
        _status.value = fbRepository.clearAllNotifications(allNotifications)
    }


}