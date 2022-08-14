package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL_DISHES
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.AMMASPECIAL
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BANNER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUCCESS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat

class FoodSubscriptionUseCase(
    private val fbRepository: FirestoreRepository
) {
    private val fireStore by lazy {
        FirebaseFirestore.getInstance()
    }

    suspend fun getAllBanners(): List<Banner>? = withContext(Dispatchers.IO) {
        val banners = mutableListOf<Banner>()
        return@withContext try {
            fireStore.collection(AMMASPECIAL).document(AMMASPECIAL).collection(BANNER).get().await()
                .let { snap ->
                    snap.documents.forEach {
                        banners.add(it.toObject(Banner::class.java)!!)
                    }
                }
            banners
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllAmmaSpecials(): List<AmmaSpecial>? = withContext(Dispatchers.IO) {
        val specials = mutableListOf<AmmaSpecial>()
        return@withContext try {
            fireStore.collection(AMMASPECIAL).document(AMMASPECIAL).collection(ALL_DISHES).get()
                .await().let { snap ->
                    snap.documents.forEach {
                        specials.add(it.toObject(AmmaSpecial::class.java)!!)
                    }
                }
            specials
        } catch (e: Exception) {
            null
        }
    }

    suspend fun placeFoodSubscriptionOnlinePayment(
        ammaSpecialsOrder: AmmaSpecialOrder,
        transactionID: String
    ): Flow<NetworkResult> = flow<NetworkResult> {
        try {
            emit(NetworkResult.Success("validating", "Placing Order for the selected days..."))
            if (placeOrder(ammaSpecialsOrder, transactionID, "Online")) {
                delay(1000)
                emit(NetworkResult.Success("placed", null))
            } else {
                delay(1000)
                emit(NetworkResult.Success("Server Error! Failed to create Subscription", null))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Failed(e.message.toString(), null))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun placeFoodSubscriptionWithWallet(
        ammaSpecialOrder: AmmaSpecialOrder
    ): Flow<NetworkResult> = flow<NetworkResult> {
        try {
            delay(1000)
            emit(NetworkResult.Success("transaction", "Making payment from wallet... "))

            if (
                fbRepository.makeTransactionFromWallet(
                    ammaSpecialOrder.price.toFloat(),
                    ammaSpecialOrder.customerID,
                    "Remove"
                )
            ) {
                TransactionHistory(
                    id = ammaSpecialOrder.id,
                    timestamp = System.currentTimeMillis(),
                    month = TimeUtil().getMonth(),
                    year = TimeUtil().getYear().toLong(),
                    amount = ammaSpecialOrder.price.toFloat(),
                    fromID = ammaSpecialOrder.customerID,
                    fromUPI = ammaSpecialOrder.customerID,
                    status = SUCCESS,
                    purpose = SUBSCRIPTION,
                    transactionFor = ammaSpecialOrder.customerID,
                ).let {
                    val transactionID =
                        makeTransactionEntry(it, ammaSpecialOrder.orderType)
                    if (transactionID == "failed") {
                        emit(
                            NetworkResult.Failed(
                                "wallet",
                                "Server Error! Failed to make transaction"
                            )
                        )
                        return@flow
                    } else {
                        PushNotificationUseCase(fbRepository).sendPushNotification(
                            ammaSpecialOrder.customerID,
                            "New Payment from Magizhini Wallet",
                            "You have paid Rs: ${ammaSpecialOrder.price} for a New ${ammaSpecialOrder.orderType} Subscription of Amma's Special Food Delivery Plan starting from ${ammaSpecialOrder.orderDate}",
                            WALLET_PAGE
                        )
                        delay(1000)
                        emit(NetworkResult.Success("validating", "Creating Subscription..."))
                        if (placeOrder(ammaSpecialOrder, transactionID, "Wallet")) {
                            delay(1000)
                            emit(
                                NetworkResult.Success(
                                    "placed",
                                    "Subscription Created Successfully..."
                                )
                            )
                        } else {
                            delay(1000)
                            emit(
                                NetworkResult.Failed(
                                    "placed",
                                    "Server Error! Failed to place order"
                                )
                            )
                            return@flow
                        }
                    }
                }
            }
        } catch (e: Exception) {
            fbRepository.logCrash(
                "ammaspecialOrder: creating a wallet transaction",
                e.message.toString()
            )
            emit(NetworkResult.Failed(e.message.toString(), null))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun placeOrder(
        ammaSpecialsOrder: AmmaSpecialOrder,
        transactionID: String,
        paymentMode: String
    ): Boolean = withContext(Dispatchers.IO) {
        ammaSpecialsOrder.paymentMode = paymentMode
        return@withContext try {
            val updateStore = async {
                val doc = fireStore
                    .collection(AMMASPECIAL)
                    .document("Order")
                    .collection("Order")
                    .document()
                ammaSpecialsOrder.id = doc.id
                doc.set(ammaSpecialsOrder, SetOptions.merge()).await()
                true
            }
            val createGlobalTransactionEntry = async {
                GlobalTransaction(
                    id = "",
                    userID = ammaSpecialsOrder.customerID,
                    userName = ammaSpecialsOrder.userName,
                    userMobileNumber = ammaSpecialsOrder.phoneNumber,
                    transactionID = transactionID,
                    transactionType = ammaSpecialsOrder.paymentMode,
                    transactionAmount = ammaSpecialsOrder.price.toFloat(),
                    transactionDirection = "AMMA'S SPECIAL FOOD ORDER",
                    timestamp = System.currentTimeMillis(),
                    transactionReason = "AMMA'S SPECIAL ${ammaSpecialsOrder.orderType} FOOD Subscription"
                ).let { return@async fbRepository.createGlobalTransactionEntry(it) }
            }
            updateStore.await() &&
                    createGlobalTransactionEntry.await()
        } catch (e: Exception) {
            fbRepository.logCrash("ammaspecialOrder: Placing a new order", e.message.toString())
            false
        }
    }

    private suspend fun makeTransactionEntry(
        transactionHistory: TransactionHistory,
        subType: String
    ): String {
        return try {
            when (val result = fbRepository.updateTransaction(
                transactionHistory,
                "New $subType Amma's Special Subscription"
            )) {
                is NetworkResult.Success -> result.data.toString()
                is NetworkResult.Failed -> "failed"
                else -> "failed"
            }
        } catch (e: Exception) {
            "failed"
        }
    }

    suspend fun getFoodStatus(
        date: Long,
        ammaSpecialsOrder: List<AmmaSpecialOrder>
    ): HashMap<String, String>? = withContext(Dispatchers.IO) {
        return@withContext try {
            var statusMap: HashMap<String, String> = hashMapOf()
            val doc = fireStore
                .collection(AMMASPECIAL)
                .document("Status")
                .collection(SimpleDateFormat("dd-MM-yyyy").format(date))
            if (ammaSpecialsOrder.isNotEmpty()) {
                ammaSpecialsOrder.forEach { order ->
                    doc.document(order.id).get().await()
                        .toObject(AmmaSpecialDeliveryStatus::class.java)?.let {
                            statusMap[order.id] = it.status
                        } ?: let {
                        statusMap[order.id] = "na"
                    }
                }
            }
            Log.e("qwq", "getFoodStatus: $statusMap", )
            statusMap
        } catch (e: Exception) {
            fbRepository.logCrash(
                "ammaspecialDeliverystatus: Checking the delivery status",
                e.message.toString()
            )
            null
        }
    }

    suspend fun getAmmaSpecialOrders(userID: String): MutableList<AmmaSpecialOrder>? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val orders = mutableListOf<AmmaSpecialOrder>()
                val docs = fireStore
                    .collection(AMMASPECIAL)
                    .document("Order")
                    .collection("Order")
                    .whereEqualTo("customerID", userID).get().await()
                docs.documents.forEach { doc ->
                    doc.toObject(AmmaSpecialOrder::class.java)?.let { it -> orders.add(it) }
                }
                if (orders.isNullOrEmpty()) {
                    null
                } else {
                    orders
                }
            } catch (e: Exception) {
                fbRepository.logCrash(
                    "ammaspecialDeliverystatus: Checking the delivery status",
                    e.message.toString()
                )
                null
            }
        }

    suspend fun cancelDeliveryOn(
        date: Long,
        selectedOrder: AmmaSpecialOrder,
        refund: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = fireStore
                .collection(AMMASPECIAL)
                .document("Status")
                .collection(SimpleDateFormat("dd-MM-yyyy").format(date))

            if (doc.document(selectedOrder.id).get().await().exists()) {
                 doc.document(selectedOrder.id)
                .update("status", "cancel", "refund", refund).await()
            } else {
                AmmaSpecialDeliveryStatus(
                    selectedOrder.id,
                    "cancel",
                    refund
                ).let {
                    doc.document(it.id).set(it, SetOptions.merge())
                }
            }

            true
        } catch (e: Exception) {
            fbRepository.logCrash(
                "ammaspecialOrder: cancelling delivery on a particular day",
                e.message.toString()
            )
            false
        }
    }

    suspend fun cancelSubscription(selectedOrder: AmmaSpecialOrder): Boolean {
        return try {
            selectedOrder.status = "cancel"

            fireStore
                .collection(AMMASPECIAL)
                .document("Status")
                .collection(SimpleDateFormat("dd-MM-yyyy").format(System.currentTimeMillis()))
                .document(selectedOrder.id)
                .update("status", "cancel", "refund", "no").await()

            fireStore
                .collection(AMMASPECIAL)
                .document("Order")
                .collection("Order")
                .document(selectedOrder.id).delete().await()

            fireStore
                .collection(AMMASPECIAL)
                .document("Order")
                .collection("Unsub")
                .document(selectedOrder.id).set(selectedOrder, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            fbRepository.logCrash(
                "ammaspecialOrder: cancelling food sub",
                e.message.toString()
            )
            false
        }
    }

    suspend fun renewSubWithWallet(
        ammaSpecialOrder: AmmaSpecialOrder
    ): Flow<NetworkResult> = flow<NetworkResult> {
        try {
            delay(1000)
            emit(NetworkResult.Success("transaction", "Making payment from wallet... "))

            if (
                fbRepository.makeTransactionFromWallet(
                    ammaSpecialOrder.price.toFloat(),
                    ammaSpecialOrder.customerID,
                    "Remove"
                )
            ) {
                TransactionHistory(
                    id = ammaSpecialOrder.id,
                    timestamp = System.currentTimeMillis(),
                    month = TimeUtil().getMonth(),
                    year = TimeUtil().getYear().toLong(),
                    amount = ammaSpecialOrder.price.toFloat(),
                    fromID = ammaSpecialOrder.customerID,
                    fromUPI = ammaSpecialOrder.customerID,
                    status = SUCCESS,
                    purpose = SUBSCRIPTION,
                    transactionFor = ammaSpecialOrder.customerID,
                ).let {
                    val transactionID =
                        makeTransactionEntry(it, ammaSpecialOrder.orderType)
                    if (transactionID == "failed") {
                        emit(
                            NetworkResult.Failed(
                                "wallet",
                                "Server Error! Failed to make transaction"
                            )
                        )
                        return@flow
                    } else {
                        PushNotificationUseCase(fbRepository).sendPushNotification(
                            ammaSpecialOrder.customerID,
                            "New Payment from Magizhini Wallet",
                            "You have paid Rs: ${ammaSpecialOrder.price} for a New ${ammaSpecialOrder.orderType} Subscription of Amma's Special Food Delivery Plan starting from ${ammaSpecialOrder.orderDate}",
                            WALLET_PAGE
                        )
                        delay(1000)
                        emit(NetworkResult.Success("validating", "Creating Subscription..."))
                        if (renewOrder(ammaSpecialOrder, transactionID, "Wallet")) {
                            delay(1000)
                            emit(
                                NetworkResult.Success(
                                    "placed",
                                    "Subscription Created Successfully..."
                                )
                            )
                        } else {
                            delay(1000)
                            emit(
                                NetworkResult.Failed(
                                    "placed",
                                    "Server Error! Failed to place order"
                                )
                            )
                            return@flow
                        }
                    }
                }
            }
        } catch (e: Exception) {
            fbRepository.logCrash(
                "ammaspecialOrder: creating a wallet transaction",
                e.message.toString()
            )
            emit(NetworkResult.Failed(e.message.toString(), null))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun renewSubscriptionWithOnline(
        ammaSpecialsOrder: AmmaSpecialOrder,
        transactionID: String
    ): Flow<NetworkResult> = flow<NetworkResult> {
        try {
            emit(NetworkResult.Success("validating", "Renewing your subscription..."))
            if (renewOrder(ammaSpecialsOrder, transactionID, "Online")) {
                delay(1000)
                emit(NetworkResult.Success("placed", null))
            } else {
                delay(1000)
                emit(NetworkResult.Success("Server Error! Failed to renew Subscription", null))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Failed(e.message.toString(), null))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun renewOrder(
        ammaSpecialsOrder: AmmaSpecialOrder,
        transactionID: String,
        paymentMode: String
    ): Boolean = withContext(Dispatchers.IO) {
        ammaSpecialsOrder.paymentMode = paymentMode
        return@withContext try {
            val updateStore = async {
                fireStore
                    .collection(AMMASPECIAL)
                    .document("Order")
                    .collection("Order")
                    .document(ammaSpecialsOrder.id)
                    .set(ammaSpecialsOrder, SetOptions.merge()).await()
                true
            }
            val createGlobalTransactionEntry = async {
                GlobalTransaction(
                    id = "",
                    userID = ammaSpecialsOrder.customerID,
                    userName = ammaSpecialsOrder.userName,
                    userMobileNumber = ammaSpecialsOrder.phoneNumber,
                    transactionID = transactionID,
                    transactionType = ammaSpecialsOrder.paymentMode,
                    transactionAmount = ammaSpecialsOrder.price.toFloat(),
                    transactionDirection = "AMMA'S SPECIAL FOOD RENEWAL",
                    timestamp = System.currentTimeMillis(),
                    transactionReason = "AMMA'S SPECIAL ${ammaSpecialsOrder.orderType} FOOD Subscription Renewal"
                ).let { return@async fbRepository.createGlobalTransactionEntry(it) }
            }
            updateStore.await() &&
                    createGlobalTransactionEntry.await()
        } catch (e: Exception) {
            fbRepository.logCrash("ammaspecialOrder: Placing a new order", e.message.toString())
            false
        }
    }

    suspend fun getNonDeliveryDays(): MutableList<Long>? = withContext(Dispatchers.IO) {
        try {
            val dates = mutableListOf<Long>()
            fireStore
                .collection(AMMASPECIAL)
                .document("Leaves")
                .get().await().toObject(NonDeliveryDates::class.java)?.let {
                    dates.addAll(it.dates)
                }
            dates
        } catch (e: Exception) {
            fbRepository.logCrash("ammaspecialOrder: getting non delivery dates", e.message.toString())
            null
        }
    }

}