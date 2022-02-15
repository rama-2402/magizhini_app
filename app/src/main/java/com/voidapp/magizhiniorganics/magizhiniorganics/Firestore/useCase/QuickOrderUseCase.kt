package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.QuickOrder
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistory
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ORDER_ESTIMATE_PATH
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PENDING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.QUICK_ORDER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUCCESS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.Exception

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

    fun sendGetEstimateRequest (
        orderListUri: List<Uri>,
        orderListExtension: List<String>,
        detailsMap: HashMap<String, String>
        ): Flow<NetworkResult> =
        flow {
            emit(NetworkResult.Success("starting", null))

            val imageUrl = arrayListOf<String>()

            try {
                delay(1000)
                for (i in orderListExtension.indices) {
                    emit(NetworkResult.Success("uploading", i + 1))
                    val reference: StorageReference = firebaseStorage.child(
                        "${ORDER_ESTIMATE_PATH}${detailsMap["id"]}/Page${i + 1}.${orderListExtension[i]}"
                    )

                    val url = reference.putFile(orderListUri[i])
                        .await().task.snapshot.metadata!!.reference!!.downloadUrl.await()

                    imageUrl.add(url.toString())
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
                    note = ""
                )

                quickOrder?.let {
                    fireStore
                        .collection(QUICK_ORDER)
                        .document(it.customerID)
                        .set(it, SetOptions.merge())
                        .await()

                    emit(NetworkResult.Success("complete", imageUrl))
                }
            } catch (e: Exception) {
                emit(NetworkResult.Failed("complete", "${ e.message.toString()}"))
            }
        }.flowOn(Dispatchers.IO)

    suspend fun checkForPreviousEstimate(userID: String): NetworkResult = withContext(Dispatchers.IO) {
        return@withContext try {
                val quickOrderDoc = fireStore
                    .collection(QUICK_ORDER)
                    .document(userID)
                    .get().await()

                if (quickOrderDoc.exists()) {
                    quickOrder = quickOrderDoc.toObject(QuickOrder::class.java)
                    NetworkResult.Success("estimate", quickOrder)
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
        cart: ArrayList<CartEntity>
        ): Flow<NetworkResult> =
            flow<NetworkResult> {
                try {

                    emit(NetworkResult.Success("transaction", "Making payment from wallet... "))

                    if (
                        fbRepository.makeTransactionFromWallet(amount, orderDetailsMap["userID"].toString(), "Remove")
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
                                emit(NetworkResult.Failed("wallet", "Server Error! Failed to make transaction"))
                                return@flow
                            } else {
                                delay(1000)
                                emit(NetworkResult.Success("order", "Placing order..."))
                                val transactionMap: HashMap<String, Any> = hashMapOf()
                                transactionMap["transactionID"] = transactionID
                                transactionMap["amount"] = amount
                                transactionMap["paymentMode"] = "Wallet"
                                transactionMap["paymentDone"] = true
                                if (placeOrder(transactionMap, orderDetailsMap, cart)) {
                                    delay(1000)
                                    emit(NetworkResult.Success("success", "Order Placed Successfully..."))
                                } else {
                                    delay(1000)
                                    emit(NetworkResult.Failed("order", "Server Error! Failed to place order"))
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
            when(val result = fbRepository.updateTransaction(transactionHistory)) {
                is NetworkResult.Success -> result.data.toString()
                is NetworkResult.Failed -> "failed"
                else -> "failed"
            }
        } catch (e: Exception) {
            "failed"
        }
    }

    suspend fun placeCashOnDeliveryOrder(
        orderDetailsMap: HashMap<String, Any>,
        cart: ArrayList<CartEntity>,
        amount: Float
        ): Flow<NetworkResult> = flow<NetworkResult> {

    }.flowOn(Dispatchers.IO)

    private suspend fun placeOrder(
        transactionMap: HashMap<String, Any>,
        orderDetailsMap: HashMap<String, Any>,
        cart: ArrayList<CartEntity>
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
                when(fbRepository.placeOrder(it)) {
                    is NetworkResult.Success -> true
                    is NetworkResult.Failed -> false
                    else -> false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}