package com.voidapp.magizhiniorganics.magizhiniorganics.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUB
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUB_ACTIVE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.UNSUB
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USERS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class PortPhoneAuthToGmailAuth(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), KodeinAware {

    override val kodein: Kodein by kodein(context)

    private val fbRepository: FirestoreRepository by instance()

    private val googleAuthID: String = inputData.getString("userID")!!
    private val phoneAuthID: String = inputData.getString("phoneID")!!

    private val firestore by lazy {
        FirebaseFirestore.getInstance()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {

            val portProfile = async { portProfile() }
            val portOrders = async { portOrders() }
            val portSubscriptions = async { portSubscriptions() }
            val portWallet = async { portWallet() }
            val portTransactions = async { portTransactions() }

           if (
                portProfile.await() &&
                portOrders.await() &&
                portSubscriptions.await() &&
                portWallet.await() &&
                portTransactions.await()
           ) {
               Result.success()
           } else {
                Result.retry()
           }
        } catch (e: Exception) {
            fbRepository.logCrash("authPortWorker", e.message.toString())
            Result.retry()
        }
    }

    private suspend fun portTransactions() : Boolean {
        return try {
            val transactions = mutableListOf<TransactionHistory>()
            val docs = firestore.collection(WALLET)
                .document("Transaction")
                .collection(phoneAuthID)
                .get().await()
            for (doc in docs.documents) {
                doc.toObject(TransactionHistory::class.java)?.let { transactionHistory ->
                    transactions.add(transactionHistory)
                    transactionHistory.fromID = googleAuthID
                    firestore.collection(WALLET)
                        .document("Transaction")
                        .collection(googleAuthID)
                        .document(transactionHistory.id)
                        .set(transactionHistory, SetOptions.merge()).await()
                }
            }
            for (transaction in transactions) {
                firestore.collection(WALLET)
                    .document("Transaction")
                    .collection(phoneAuthID)
                    .document(transaction.id).delete()
            }
            transactions.clear()
            true
        } catch (e: Exception) {
            fbRepository.logCrash("authWorker: transaction ", e.message.toString())
            false
        }
    }

    private suspend fun portWallet() : Boolean {
        return try {
            firestore.collection(WALLET)
                .document(WALLET)
                .collection("Users")
                .document(phoneAuthID).get().await().toObject(Wallet::class.java)?.let { wallet ->
                    wallet.id = googleAuthID
                    firestore.collection(WALLET)
                        .document(WALLET)
                        .collection("Users")
                        .document(wallet.id).set(wallet, SetOptions.merge()).await()
                }
            firestore.collection(WALLET)
                .document(WALLET)
                .collection("Users")
                .document(phoneAuthID).delete()
            true
        } catch (e: Exception) {
            fbRepository.logCrash("authWorker: wallet", e.message.toString())
            false
        }
    }

    private suspend fun portSubscriptions() : Boolean {
        return try {
            val activeSubDoc = FirebaseFirestore.getInstance()
                .collection(SUBSCRIPTION).document(SUB_ACTIVE)
                .collection(SUB)
                .whereEqualTo("customerID", phoneAuthID)
                .get().await()
            for (doc in activeSubDoc.documents) {
                doc.toObject(Subscription::class.java)?.let { doc ->
                    firestore.collection(SUBSCRIPTION)
                        .document(SUB_ACTIVE)
                        .collection(SUB)
                        .document(doc.id)
                        .update("customerID", googleAuthID)
                }
            }

            val cancelledSubDoc = FirebaseFirestore.getInstance()
                .collection(SUBSCRIPTION).document(SUB_ACTIVE)
                .collection(UNSUB)
                .whereEqualTo("customerID", phoneAuthID)
                .get().await()
            for (doc in cancelledSubDoc.documents) {
                doc.toObject(Subscription::class.java)?.let { sub ->
                    firestore.collection(SUBSCRIPTION)
                        .document(SUB_ACTIVE)
                        .collection(UNSUB)
                        .document(sub.id)
                        .update("customerID", googleAuthID)
                }
            }
            true
        } catch (e: Exception) {
            fbRepository.logCrash("authWorker: subscription", e.message.toString())
            false
        }
    }

    private suspend fun portOrders() : Boolean {
        return try {
            val path = firestore.collection("orderHistory")
            val months = path.get().await()
            for (month in months.documents) {
                val docs = path.document(month.id)
                    .collection("Active")
                    .whereEqualTo("customerId", phoneAuthID)
                    .get().await()
                for (doc in docs.documents) {
                    doc.toObject(Order::class.java)?.let { order ->
                        path.document(month.id)
                            .collection("Active")
                            .document(order.orderId)
                            .update("customerId", googleAuthID)
                    }
                }
            }
            true
        } catch (e: Exception) {
            fbRepository.logCrash("authWorker: orders", e.message.toString())
            false
        }
    }

    private suspend fun portProfile():Boolean {
        return try {
            firestore
                .collection(USERS)
                .document(phoneAuthID)
                .get().await().toObject(UserProfile::class.java)?.let { profile ->
                    profile.id = googleAuthID
                    firestore
                        .collection(USERS)
                        .document(profile.id)
                        .set(profile, SetOptions.merge()).await()
                }

            firestore.collection(USERS).document(phoneAuthID).delete()
            true
        } catch (e: Exception) {
            fbRepository.logCrash("authWorker profile", e.message.toString())
            false
        }
    }
}