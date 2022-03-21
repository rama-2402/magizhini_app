package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase

import android.provider.SyncStateContract
import android.util.Log
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.NotificationData
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.PushNotification
import com.voidapp.magizhiniorganics.magizhiniorganics.services.RetrofitInstance
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PushNotificationUseCase(
    val fbRepository: FirestoreRepository
) {

    suspend fun sendPushNotification(
        customerID: String,
        title: String,
        message: String,
        activity: String
    ) = withContext(Dispatchers.IO) {
        val token = getToken(customerID)
        if (token == "") {
            return@withContext
        }
        PushNotification(
            NotificationData(
                title,
                message,
                "",
                activity
            ),
            token
        ).also {
            sendNotification(it)
        }
    }

    private suspend fun sendNotification(notification: PushNotification): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                //something
                return@withContext true
            } else {
                Log.e("TAG", response.errorBody().toString())
                return@withContext false
            }
        } catch(e: Exception) {
            Log.e("TAG", e.toString())
            return@withContext false
        }
    }

    private suspend fun getToken(customerID: String): String {
        return fbRepository.getCustomerToken(customerID)
//        if (token == "") {
//            _status.value = NetworkCallbackResult.Failed("token", "Failed to connect to server. Messages will not be sent")
//        }
    }
}