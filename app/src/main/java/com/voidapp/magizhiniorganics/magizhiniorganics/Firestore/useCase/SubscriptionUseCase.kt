package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase

import com.google.firebase.firestore.FirebaseFirestore
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Subscription
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistory
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class SubscriptionUseCase(
    private val fbRepository: FirestoreRepository
) {

    private val fireStore by lazy {
        FirebaseFirestore.getInstance()
    }

    suspend fun placeSubscriptionWithOnline(
        subscription: Subscription,
        userName: String,
        transactionID: String
    ): Flow<NetworkResult> =
        flow<NetworkResult> {
            try {
                emit(NetworkResult.Success("validating", "Creating Subscription..."))
                if (placeSubscription(subscription, userName, transactionID)) {
                    delay(1000)
                    emit(NetworkResult.Success("placed", null))
                } else {
                    delay(1000)
                    emit(NetworkResult.Success("Server Error! Failed to create Subscription", null))
                }
            } catch (e:Exception) {
                emit(NetworkResult.Failed(e.message.toString(), null))
            }
        }.flowOn(Dispatchers.IO)

    suspend fun placeSubscriptionWithWallet(
        subscription: Subscription,
        userName: String
    ): Flow<NetworkResult>
    = flow<NetworkResult> {
        try {
            delay(1000)
            emit(NetworkResult.Success("transaction", "Making payment from wallet... "))

            if (
                fbRepository.makeTransactionFromWallet(subscription.estimateAmount, subscription.customerID, "Remove")
            ) {
                TransactionHistory(
                    id = subscription.id,
                    timestamp = System.currentTimeMillis(),
                    month = TimeUtil().getMonth(),
                    year = TimeUtil().getYear().toLong(),
                    amount = subscription.estimateAmount,
                    fromID = subscription.customerID,
                    fromUPI = subscription.customerID,
                    status = Constants.SUCCESS,
                    purpose = SUBSCRIPTION,
                    transactionFor = subscription.id,
                ).let {
                    val transactionID = makeTransactionEntry(it, subscription.productName, subscription.variantName)
                    if (transactionID == "failed") {
                        emit(NetworkResult.Failed("wallet", "Server Error! Failed to make transaction"))
                        return@flow
                    } else {
                        PushNotificationUseCase(fbRepository).sendPushNotification(
                            subscription.customerID,
                            "New Payment from Magizhini Wallet",
                            "You have paid Rs: ${subscription.estimateAmount} for a New Subscription of ${subscription.productName} - ${subscription.variantName} starting from ${TimeUtil().getCustomDate(dateLong = subscription.startDate)}",
                            WALLET_PAGE
                        )
                        delay(1000)
                        emit(NetworkResult.Success("validating", "Creating Subscription..."))
                        if (placeSubscription(subscription, userName, transactionID)) {
                            delay(1000)
                            emit(NetworkResult.Success("placed", "Subscription Created Successfully..."))
                        } else {
                            delay(1000)
                            emit(NetworkResult.Failed("placed", "Server Error! Failed to place order"))
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
            emit(NetworkResult.Failed(e.message.toString(), null))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun placeSubscription(
        subscription: Subscription,
        userName: String,
        transactionID: String
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            when (fbRepository.generateSubscription(subscription, userName, transactionID)) {
                is NetworkResult.Success -> {
                    PushNotificationUseCase(fbRepository).sendPushNotification(
                        subscription.customerID,
                        "New Subscription Created",
                        "Thanks for Subscribing to our product ${subscription.productName} - ${subscription.variantName} starting from ${TimeUtil().getCustomDate(dateLong = subscription.startDate)}. You can manage your subscriptions from Subscription History Page",
                        Constants.SUB_HISTORY_PAGE
                    )
                    true
                }
                is NetworkResult.Failed -> false
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun makeTransactionEntry(transactionHistory: TransactionHistory, productName: String, variantName: String): String {
        return try {
            when(val result = fbRepository.updateTransaction(transactionHistory, "$productName $variantName New Subscription")) {
                is NetworkResult.Success -> result.data.toString()
                is NetworkResult.Failed -> "failed"
                else -> "failed"
            }
        } catch (e: Exception) {
            "failed"
        }
    }
}