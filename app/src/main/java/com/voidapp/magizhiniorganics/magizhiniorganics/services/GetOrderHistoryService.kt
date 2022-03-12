package com.voidapp.magizhiniorganics.magizhiniorganics.services

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CrashLog
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Subscription
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUB
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUB_ACTIVE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.UNSUB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class GetOrderHistoryService(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), KodeinAware {
    val baseContext = context
    override val kodein: Kodein by kodein(baseContext)
    private val repository: DatabaseRepository by instance()

    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    override suspend fun doWork(): Result {

        val userID = inputData.getString("id")!!

        try {
            withContext(Dispatchers.IO) {
                val getOrders = async { getOrders(userID) }
                val getSubscriptions = async { getSubscriptions(userID) }
                val getCancelledSubscriptions = async { getCancelledSubscriptions(userID) }

                getOrders.await()
                getSubscriptions.await()
                getCancelledSubscriptions.await()
            }
        } catch (e: Exception) {
            return Result.retry()
        }
        return Result.success()
    }


    private suspend fun getSubscriptions(userID: String) {
        try {
            withContext(Dispatchers.IO) {
                val documents = FirebaseFirestore.getInstance()
                    .collection(SUBSCRIPTION).document(SUB_ACTIVE)
                    .collection(SUB)
                    .whereEqualTo("customerID", userID)
                    .get().await()
                for (doc in documents.documents) {
                    val sub = doc.toObject(Subscription::class.java)?.toSubscriptionEntity()
                    sub?.let { repository.upsertSubscription(sub) }
                }
            }
        } catch (e: Exception) {
            e.message?.let {
                logCrash(userID, "GetOrderService: getting all the user subscriptions",
                    it
                )
            }
        }
    }

    private suspend fun getCancelledSubscriptions (userID: String) {
        try {
            withContext(Dispatchers.IO) {
                val documents = FirebaseFirestore.getInstance()
                    .collection(SUBSCRIPTION).document(SUB_ACTIVE)
                    .collection(UNSUB)
                    .whereEqualTo("customerID", userID)
                    .get().await()
                for (doc in documents.documents) {
                    val sub = doc.toObject(Subscription::class.java)?.toSubscriptionEntity()
                    sub?.let { repository.upsertSubscription(sub) }
                }
            }
        } catch (e: Exception) {
            e.message?.let {
                logCrash(userID, "GetOrderService: getting all the user cancelled subscriptions",
                    it
                )
            }
        }
    }

    private suspend fun getOrders(userID: String) {
        try {
            val path = firestore.collection("orderHistory")
            val months = path.get().await()
            for (month in months.documents) {
                val docs = path.document(month.id)
                    .collection("Active")
                    .whereEqualTo("customerId", userID)
                    .get().await()
                for (doc in docs.documents) {
                    val order = doc.toObject(Order::class.java)?.toOrderEntity()
                    order?.let { repository.upsertOrder(it) }
                }
            }
        } catch (e: Exception) {
            e.message?.let {
                logCrash(userID, "GetOrderService: getting all the active orders",
                    it
                )
            }
        }
    }

    private suspend fun logCrash(userID: String, location: String, message: String) {
        CrashLog(
            userID,
            "${ Build.MANUFACTURER } ${ Build.MODEL } ${Build.VERSION.RELEASE} ${ Build.VERSION_CODES::class.java.fields[Build.VERSION.SDK_INT].name }",
            TimeUtil().getCustomDate("",System.currentTimeMillis()),
            TimeUtil().getTimeInHMS(dateLong = System.currentTimeMillis()),
            location,
            message
        ).let {
            try {
                    firestore
                    .collection("crashLog")
                    .document(userID)
                    .collection("MagizhiniApp")
                    .document()
                    .set(it, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.e("Magizhini", "logCrash: $it ", )
            }
        }
    }
}