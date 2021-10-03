package com.voidapp.magizhiniorganics.magizhiniorganics.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.toOrderEntity
import kotlinx.coroutines.Dispatchers
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

    override suspend fun doWork(): Result {
        //TODO GET THE MONTHYEAR FROM THE FILTER AND FETCH THE REQUIRED MONTH'S DATA FROM STORE
        //OUTPUT ALL THE LIST OF DATA FROM THE STORE

        val filter = inputData.getString("filter")
        val dates = baseContext.resources.getStringArray(R.array.dates)
        val userID = SharedPref(baseContext).getData(Constants.USER_ID, Constants.STRING, "")
        try {
            val path = FirebaseFirestore.getInstance()
                .collection("orderHistory")
                .document(filter!!)
            for (i in dates) {
                val snapShot = path
                    .collection(i)
                    .whereEqualTo("customerId", userID)
                    .get()
                    .await()
                snapShot?.let { querySnapshot ->
                    withContext(Dispatchers.Default) {
                        for (doc in querySnapshot) {
                            val order = doc.toObject(Order::class.java).toOrderEntity()
                            withContext(Dispatchers.IO) {
                                repository.upsertOrder(order)
                            }
                        }
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}