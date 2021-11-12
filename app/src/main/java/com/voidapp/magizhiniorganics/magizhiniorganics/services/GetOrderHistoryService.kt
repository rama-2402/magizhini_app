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
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
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
    private val fireStore: FirestoreRepository by instance()
    private val repository: DatabaseRepository by instance()

    override suspend fun doWork(): Result {

        val filter = inputData.getString("filter")!!
        val userID = inputData.getString("id")!!

        try {
            withContext(Dispatchers.IO) {
                val getOrders = async { getOrders(userID, filter) }
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
//                val profile = fireStore.getProfile(userID)
                for (year in 2021..TimeUtil().getYear().toInt()) {
                    val docs = FirebaseFirestore.getInstance()
                        .collection("Subscription")
                        .document("Active")
                        .collection(year.toString())
                        .whereEqualTo("customerID", userID)
                        .get().await()

                    for (doc in docs.documents) {
                        val subscription = doc.toObject(Subscription::class.java)?.toSubscriptionEntity()
                        subscription?.let { repository.upsertSubscription(it) }
                    }
                }
//                for (month in profile.subscribedMonths) {
//                    val documents = path
//                        .collection(month)
//                        .whereEqualTo("customerID", profile.id)
//                        .get().await()
//                    for (doc in documents) {
//                        val sub = doc.toObject(Subscription::class.java).toSubscriptionEntity()
//                        repository.upsertSubscription(sub)
//                    }
//                }
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
//                val profile = fireStore.getProfile(userID)
                val docs = FirebaseFirestore.getInstance()
                    .collection("Subscription")
                    .document("Cancelled")
                    .collection("subs")
                    .whereEqualTo("customerID", userID)
                    .get().await()

                for (doc in docs.documents) {
                    val subscription = doc.toObject(Subscription::class.java)?.toSubscriptionEntity()
                    subscription?.let { repository.upsertSubscription(it) }
                }
//                for (month in profile.subscribedMonths) {
//                    val documents = path
//                        .collection(month)
//                        .whereEqualTo("customerID", profile.id)
//                        .get().await()
//                    for (doc in documents) {
//                        val sub = doc.toObject(Subscription::class.java).toSubscriptionEntity()
//                        repository.upsertSubscription(sub)
//                    }
//                }
            }
        } catch (e: Exception) {
            e.message?.let {
                logCrash(userID, "GetOrderService: getting all the cancelled user subscriptions",
                    it
                )
            }
        }
    }

    private suspend fun getOrders(userID: String, filter: String) {
        val dates = baseContext.resources.getStringArray(R.array.dates)
        try {
//            val path = FirebaseFirestore.getInstance()
//                .collection("orderHistory")
//                .document(filter)
//            withContext(Dispatchers.IO) {
//                for (i in dates) {
//                    val snapShot = path
//                        .collection(i)
//                        .whereEqualTo("customerId", userID)
//                        .get()
//                        .await()
//                    snapShot?.let { querySnapshot ->
//                        for (doc in querySnapshot) {
//                            val order = doc.toObject(Order::class.java).toOrderEntity()
//                            repository.upsertOrder(order)
//                        }
//                    }
//                }
//            }
            val path = FirebaseFirestore.getInstance().collection("orderHistory")
            val months = path.get().await()
            for (a in months.documents) {
                Log.e("TAG", "getOrders: ${a.id}", )
            }
//            for (month in months.documents) {
//                Log.e("TAG", "month: ${month}", )
                val docs = path.document("November2021")
                    .collection("Active")
                    .whereEqualTo("customerId", userID)
                    .get().await()
                for (doc in docs.documents) {
                    val order = doc.toObject(Order::class.java)?.toOrderEntity()
                    Log.e("TAG", "getOrders: $order", )
                    order?.let { repository.upsertOrder(it) }
                }
//            }
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
            location,
            message
        ).let {
            try {
                FirebaseFirestore.getInstance()
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