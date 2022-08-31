package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ADMINID
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ORDER_ESTIMATE_PATH
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ORDER_HISTORY_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PENDING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.QUICK_ORDER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.QUICK_ORDER_PAGE
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
import java.io.File

class QuickOrderUseCase(
    private val fbRepository: FirestoreRepository
) {
    private val fireStore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val firebaseStorage by lazy {
        FirebaseStorage.getInstance().reference
    }

    private var quickOrder: QuickOrder? = null

    fun sendGetEstimateRequest(
        orderListUri: List<Uri>,
        textItemsList: List<QuickOrderTextItem>,
        audioUri: String?,
        detailsMap: HashMap<String, String>
    ): Flow<NetworkResult> =
        flow {
            emit(NetworkResult.Success("starting", null))

            val imageUrl = arrayListOf<String>()
            var audioUrl: String = ""
            try {
                delay(1000)
                when (detailsMap["quickOrderType"]) {
                    "image" -> {
                        for (i in orderListUri.indices) {
                            emit(NetworkResult.Success("uploading", "Uploading Page ${i + 1}..."))
                            val reference: StorageReference = firebaseStorage.child(
                                "${ORDER_ESTIMATE_PATH}${detailsMap["id"]}/Page${i + 1}.jpg"
                            )

                            val url = reference.putFile(orderListUri[i])
                                .await().task.snapshot.metadata!!.reference!!.downloadUrl.await()

                            imageUrl.add(url.toString())
                        }
                    }
                    "voice" -> {
                        audioUrl = audioUri?.let {
                            //upload the audio in storage and put the url
                            emit(NetworkResult.Success("uploading", "Uploading Audio..."))
                            val reference: StorageReference = firebaseStorage.child(
                                "${ORDER_ESTIMATE_PATH}${detailsMap["id"]}/quickOrder.m4a"
                            )

                            val uri: Uri = Uri.fromFile(File(it))

                            val url = reference.putFile(uri)
                                .await().task.snapshot.metadata!!.reference!!.downloadUrl.await()

                            url.toString()
                        } ?: ""
                    }
                }

                quickOrder = QuickOrder(
                    customerID = detailsMap["id"].toString(),
                    customerName = detailsMap["name"].toString(),
                    phoneNumber = detailsMap["phNumber"].toString(),
                    orderID = detailsMap["orderID"].toString(),
                    mailID = "",
                    timeStamp = System.currentTimeMillis(),
                    cart = arrayListOf(),
                    imageUrl = imageUrl,
                    textItemsList = textItemsList as ArrayList<QuickOrderTextItem>,
                    audioFileUrl = audioUrl,
                    orderType = detailsMap["quickOrderType"].toString(),
                    note = "",
                    orderPlaced = false
                )

                quickOrder?.let {
                    fireStore
                        .collection(QUICK_ORDER)
                        .document(it.customerID)
                        .set(it, SetOptions.merge())
                        .await()

                    PushNotificationUseCase(fbRepository).sendPushNotification(
                        ADMINID,
                        "New Quick Order Estimate Request",
                        "${it.customerName} has requested a new Estimate Request for his quick order. Please follow up with his request",
                        QUICK_ORDER_PAGE
                    )

                    PushNotificationUseCase(fbRepository).sendPushNotification(
                        it.customerID,
                        "Estimate Request Sent",
                        "Thank you for using Magizhini Quick Order Services. We have received your Order Estimate Request. We will get back to you as soon as possible with an Estimate Amount",
                        QUICK_ORDER_PAGE
                    )

                    emit(NetworkResult.Success("complete", imageUrl))
                }
            } catch (e: Exception) {
                emit(NetworkResult.Failed("complete", "${e.message}"))
            }
        }.flowOn(Dispatchers.IO)

    suspend fun checkForPreviousEstimate(userID: String): NetworkResult =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val quickOrderDoc = fireStore
                    .collection(QUICK_ORDER)
                    .document(userID)
                    .get().await()

                if (quickOrderDoc.exists()) {
                    quickOrderDoc.toObject(QuickOrder::class.java)?.let {
                        NetworkResult.Success("estimate", it)
                    } ?: NetworkResult.Success("estimate", null)
                } else {
                    NetworkResult.Success("estimate", null)
                }
            } catch (e: Exception) {
                NetworkResult.Failed("estimate", e.message.toString())
            }
        }

    suspend fun initiateWalletTransaction(
        orderDetailsMap: HashMap<String, Any>,
        amount: Float,
        purpose: String,
        cart: ArrayList<CartEntity>,
        isQuickOrder: Boolean
    ): Flow<NetworkResult> =
        flow<NetworkResult> {
            try {

                emit(NetworkResult.Success("transaction", "Making payment from wallet... "))

                if (
                    fbRepository.makeTransactionFromWallet(
                        amount,
                        orderDetailsMap["userID"].toString(),
                        "Remove"
                    )
                ) {
                    TransactionHistory(
                        id = orderDetailsMap["orderID"].toString(),
                        timestamp = System.currentTimeMillis(),
                        month = TimeUtil().getMonth(),
                        year = TimeUtil().getYear().toLong(),
                        amount = amount,
                        fromID = orderDetailsMap["userID"].toString(),
                        fromUPI = orderDetailsMap["userID"].toString(),
                        status = SUCCESS,
                        purpose = purpose,
                        transactionFor = orderDetailsMap["orderID"].toString(),
                    ).let {
                        val transactionID = makeTransactionEntry(it)
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
                                orderDetailsMap["userID"].toString(),
                                "New Payment from Magizhini Wallet",
                                "You have paid Rs: ${amount} for your recent Magizhini purchase with Order ID: ${orderDetailsMap["orderID"].toString()}",
                                WALLET_PAGE
                            )
                            delay(1000)
                            emit(NetworkResult.Success("order", "Placing order..."))
                            val transactionMap: HashMap<String, Any> = hashMapOf()
                            transactionMap["transactionID"] = transactionID
                            transactionMap["amount"] = amount
                            transactionMap["paymentMode"] = "Wallet"
                            transactionMap["paymentDone"] = true
                            if (placeOrder(transactionMap, orderDetailsMap, cart, isQuickOrder)) {
                                if (isQuickOrder) {
                                    updateOrderPlacedInQuickOrder(orderDetailsMap["userID"].toString())
                                }
                                delay(1000)
                                emit(
                                    NetworkResult.Success(
                                        "success",
                                        "Order Placed Successfully..."
                                    )
                                )
                            } else {
                                delay(1000)
                                emit(
                                    NetworkResult.Failed(
                                        "order",
                                        "Server Error! Failed to place order"
                                    )
                                )
                                return@flow
                            }
                        }
                    }
                } else {
                    delay(1000)
                    emit(NetworkResult.Failed("wallet", "Server Error! Failed to make transaction"))
                    return@flow
                }
            } catch (e: Exception) {
                delay(1000)
                emit(NetworkResult.Failed("wallet", e.message.toString()))
            }
        }.flowOn(Dispatchers.IO)

    private suspend fun makeTransactionEntry(transactionHistory: TransactionHistory): String {
        return try {
            when (val result =
                fbRepository.updateTransaction(transactionHistory, "Quick Order Purchase")) {
                is NetworkResult.Success -> result.data.toString()
                is NetworkResult.Failed -> "failed"
                else -> "failed"
            }
        } catch (e: Exception) {
            "failed"
        }
    }

    suspend fun placeOnlinePaymentOrder(
        orderDetailsMap: HashMap<String, Any>,
        amount: Float,
        purpose: String,
        reason: String,
        cart: ArrayList<CartEntity>,
        isQuickOrder: Boolean
    ): Flow<NetworkResult> =
        flow<NetworkResult> {
            try {
                emit(NetworkResult.Success("validation", null))
                GlobalTransaction(
                    id = "",
                    userID = orderDetailsMap["userID"].toString(),
                    userName = orderDetailsMap["name"].toString(),
                    userMobileNumber = orderDetailsMap["phoneNumber"].toString(),
                    transactionID = orderDetailsMap["transactionID"].toString(),
                    transactionType = "Online Payment",
                    transactionAmount = amount,
                    transactionDirection = purpose,
                    timestamp = System.currentTimeMillis(),
                    transactionReason = reason
                ).let {
                    if (fbRepository.createGlobalTransactionEntry(it)) {
                        emit(NetworkResult.Success("placing", null))

                        val transactionMap: HashMap<String, Any> = hashMapOf()
                        transactionMap["transactionID"] =
                            orderDetailsMap["transactionID"].toString()
                        transactionMap["paymentMode"] = "Online"
                        transactionMap["paymentDone"] = true
                        transactionMap["amount"] = amount

                        if (placeOrder(
                                transactionMap,
                                orderDetailsMap,
                                cart,
                                isQuickOrder
                            )
                        ) {
                            if (isQuickOrder) {
                                updateOrderPlacedInQuickOrder(orderDetailsMap["userID"].toString())
                            }
                            delay(1000)
                            emit(NetworkResult.Success("placed", null))
                        } else {
                            delay(1000)
                            emit(NetworkResult.Failed("placed", null))
                        }
                    } else {
                        delay(1000)
                        emit(NetworkResult.Failed("transaction", null))
                    }
                }
            } catch (e: Exception) {
                emit(NetworkResult.Failed("transaction", null))
            }
        }.flowOn(Dispatchers.IO)

    suspend fun placeCashOnDeliveryOrder(
        orderDetailsMap: HashMap<String, Any>,
        cart: ArrayList<CartEntity>,
        amount: Float,
        isQuickOrder: Boolean
    ): Flow<NetworkResult> = flow<NetworkResult> {

        delay(1000)
        emit(NetworkResult.Success("placing", null))

        val transactionMap: HashMap<String, Any> = hashMapOf()
        transactionMap["transactionID"] = "COD"
        transactionMap["paymentMode"] = "COD"
        transactionMap["paymentDone"] = false
        transactionMap["amount"] = amount

        if (
            placeOrder(
                transactionMap,
                orderDetailsMap,
                cart,
                isQuickOrder
            )
        ) {
            if (isQuickOrder) {
                updateOrderPlacedInQuickOrder(orderDetailsMap["userID"].toString())
            }
            delay(1000)
            emit(NetworkResult.Success("placed", null))
        } else {
            delay(1000)
            emit(NetworkResult.Failed("", null))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun placeOrder(
        transactionMap: HashMap<String, Any>,
        orderDetailsMap: HashMap<String, Any>,
        cart: ArrayList<CartEntity>,
        isQuickOrder: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Order(
                orderId = orderDetailsMap["orderID"].toString(),
                customerId = orderDetailsMap["userID"].toString(),
                transactionID = transactionMap["transactionID"].toString(),
                cart = cart,
                purchaseDate = TimeUtil().getCurrentDate(),
                paymentMethod = transactionMap["paymentMode"].toString(),
                isPaymentDone = transactionMap["paymentDone"].toString().toBoolean(),
                deliveryPreference = orderDetailsMap["deliveryPreference"].toString(),
                deliveryNote = orderDetailsMap["deliveryNote"].toString(),
                appliedCoupon = orderDetailsMap["appliedCoupon"].toString(),
                address = orderDetailsMap["address"] as Address,
                price = transactionMap["amount"].toString().toFloat(),
                orderStatus = PENDING,
                monthYear = "${TimeUtil().getMonth()}${TimeUtil().getYear()}",
                phoneNumber = orderDetailsMap["phoneNumber"].toString()
            ).let {
                when {
                    isQuickOrder -> it.extras.add(QUICK_ORDER)
                    orderDetailsMap["referral"].toString() != "" -> {
                        it.extras.add("")
                        it.extras.add(orderDetailsMap["referral"].toString())
                    }
                }
                when (fbRepository.placeOrder(it)) {
                    is NetworkResult.Success -> {
                        PushNotificationUseCase(fbRepository).sendPushNotification(
                            ADMINID,
                            "New order received",
                            "There is a new order received on Magizhini Store. Please follow up with new order",
                            ORDER_HISTORY_PAGE
                        )
                        PushNotificationUseCase(fbRepository).sendPushNotification(
                            it.customerId,
                            "Order Placed",
                            "Thanks for purchasing in Magizhini Organics. Your Order (ID: ${it.orderId}) is received. We will notify during every step till delivery. You can check you order progress in Order History page.",
                            ORDER_HISTORY_PAGE
                        )
                        if (!cart.isNullOrEmpty() && isQuickOrder) {
                            updateQuickOrderCart(cart, it.customerId)
                        } else {
                            true
                        }
                    }
                    is NetworkResult.Failed -> false
                    else -> false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun updateQuickOrderCart(
        cart: ArrayList<CartEntity>,
        customerID: String
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            fireStore
                .collection(QUICK_ORDER)
                .document(customerID)
                .update("cart", cart).await()
            true
        } catch (e: Exception) {
            fbRepository.logCrash("quickOrder: updating the cart", e.message.toString())
            false
        }
    }

    private suspend fun updateOrderPlacedInQuickOrder(userID: String) {
        try {
            fireStore
                .collection(QUICK_ORDER)
                .document(userID)
                .update("orderPlaced", true).await()
        } catch (e: Exception) {
            fbRepository.logCrash("quickOrder: updating order placed", e.message.toString())
        }
    }

    private suspend fun deleteQuickOrder(userID: String) = withContext(Dispatchers.IO) {
        try {
            val mFireStoreStorage = FirebaseStorage.getInstance().reference
            val quickOrderToDelete =
                fireStore.collection(QUICK_ORDER).document(userID).get().await()
                    .toObject(QuickOrder::class.java)

            quickOrderToDelete?.let {
                val storeUpdate = async {
                    fireStore
                        .collection(QUICK_ORDER)
                        .document(userID)
                        .delete()
                }
                val deleteQuickOrderImages = async {
                    val storage = FirebaseStorage.getInstance()
                    for (i in it.imageUrl.indices) {
                        val url = it.imageUrl[i]
                        val imageName = storage.getReferenceFromUrl(url).name
                        val sRef: StorageReference = mFireStoreStorage.child(
                            "$ORDER_ESTIMATE_PATH${it.customerID}/$imageName"
                        )
                        sRef.delete()
                    }
                }

                deleteQuickOrderImages.await()
                storeUpdate.await()
            }
        } catch (e: Exception) {
            fbRepository.logCrash("quickOrder", e.message.toString())
        }
    }

    suspend fun deleteQuickOrder(
        quickOrder: QuickOrder?,
        customerID: String? = null
    ): Flow<NetworkResult> = flow<NetworkResult> {

        emit(NetworkResult.Success("Deleting Your Order List Image...", "image"))

        val order = quickOrder ?: let {
            fireStore
                .collection(QUICK_ORDER)
                .document(customerID!!)
                .get().await().toObject(QuickOrder::class.java)
        }
        try {
            val storage = FirebaseStorage.getInstance()
            val storageReference = FirebaseStorage.getInstance().reference
            quickOrder?.let {
                when (it.orderType) {
                    "image" -> {
                        for (i in quickOrder.imageUrl.indices) {
                            val url = quickOrder.imageUrl[i]
                            val imageName = storage.getReferenceFromUrl(url).name
                            storageReference.child(
                                "$ORDER_ESTIMATE_PATH${quickOrder.customerID}/$imageName"
                            ).delete().await()
                        }
                        delay(1000)
                        emit(NetworkResult.Success("Removing your Quick Order Request...", "order"))
                    }
                    "voice" -> {
                        storageReference.child(
                            "$ORDER_ESTIMATE_PATH${quickOrder.customerID}/quickOrder.m4a"
                        ).delete().await()
                        delay(1000)
                        emit(NetworkResult.Success("Removing your Quick Order Request...", "order"))
                    }
                }
            }
            order?.let {
                when (it.orderType) {
                    "image" -> {
                        for (i in it.imageUrl.indices) {
                            val url = it.imageUrl[i]
                            val imageName = storage.getReferenceFromUrl(url).name
                            storageReference.child(
                                "$ORDER_ESTIMATE_PATH${it.customerID}/$imageName"
                            ).delete().await()
                        }
                        delay(1000)
                        emit(NetworkResult.Success("Removing your Quick Order Request...", "order"))
                    }
                    "voice" -> {
                        storageReference.child(
                            "$ORDER_ESTIMATE_PATH${it.customerID}/quickOrder.m4a"
                        ).delete().await()
                        delay(1000)
                        emit(NetworkResult.Success("Removing your Quick Order Request...", "order"))
                    }
                }
            }
        } catch (e: Exception) {
            fbRepository.logCrash("quickOrder", e.message.toString())
            delay(1000)
            emit(
                NetworkResult.Failed(
                    "Failed to delete Images. Please try again later",
                    "order"
                )
            )
        }

        try {
            quickOrder?.let {
                fireStore
                    .collection(QUICK_ORDER)
                    .document(it.customerID)
                    .delete().await()
            }
            order?.let {
                fireStore
                    .collection(QUICK_ORDER)
                    .document(it.customerID)
                    .delete().await()
            }

            delay(1000)
            emit(
                NetworkResult.Success(
                    "Quick Order Request is removed successfully...",
                    "success"
                )
            )
        } catch (e: Exception) {
            fbRepository.logCrash("quickOrder", e.message.toString())
            delay(1000)
            emit(
                NetworkResult.Failed(
                    "Failed to remove your order request. Please try again later",
                    "failed"
                )
            )
        }
    }.flowOn(Dispatchers.IO)
}