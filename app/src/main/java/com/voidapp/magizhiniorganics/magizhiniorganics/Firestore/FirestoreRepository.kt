package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.PhoneAuthCredential
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult

class FirestoreRepository (
    private val firestore: Firestore
) {

    fun signOut() = firestore.signOut()

    suspend fun logCrash(location: String, message: String) = firestore.logCrash(location, message)

    fun getPhoneNumber(): String? = firestore.getPhoneNumber()

    fun getCurrentUserId(): String? = firestore.getCurrentUserId()

    //upload image
    suspend fun uploadImage(path: String, uri: Uri, extension: String, data: String = ""): String = firestore.uploadImage(path, uri, extension, data)

    //sign in check
    suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential): Boolean = firestore.signInWithPhoneAuthCredential(credential)

    suspend fun checkUserProfileDetails(): String = firestore.checkUserProfileDetails()

    //profile
    suspend fun uploadProfile(profile: UserProfile): Boolean = firestore.uploadProfile(profile)

    suspend fun addFavorites(id: String, item: String): Boolean = firestore.addFavorites(id, item)

    suspend fun removeFavorites(id: String, item: String): Boolean = firestore.removeFavorites(id, item)

    suspend fun addAddress(id: String ,address: Address) = firestore.addAddress(id, address)

    suspend fun updateAddress(id: String, address: ArrayList<Address>)  = firestore.updateAddress(id, address)

    suspend fun applyReferralNumber(currentUserID: String, code: String): Boolean = firestore.applyReferralNumber(currentUserID, code)

    suspend fun checkForReferral(userID: String): Boolean = firestore.checkForReferral(userID)

    suspend fun getLimitedItems(viewModel: ShoppingMainViewModel) = firestore.getLimitedItems(viewModel)

    suspend fun productListener(id: String, viewModel: ProductViewModel) = firestore.productListener(id, viewModel)

    suspend fun addReview(id: String, review: Review): NetworkResult = firestore.addReview(id, review)

    //checkout
    suspend fun limitedItemsUpdater(cartEntity: List<CartEntity>): NetworkResult = firestore.limitedItemsUpdater(cartEntity)

    suspend fun placeOrder(order: Order): NetworkResult = firestore.placeOrder(order)

    suspend fun validateItemAvailability(cartItems: List<CartEntity>): NetworkResult = firestore.validateItemAvailability(cartItems)

    //purchase history
    suspend fun cancelOrder(orderEntity: OrderEntity): NetworkResult = firestore.cancelOrder(orderEntity)

    //subscription
    suspend fun generateSubscription(subscription: Subscription): NetworkResult = firestore.generateSubscription(subscription)

    suspend fun generateSubscriptionID(id: String): String = firestore.generateSubscriptionID(id)

    suspend fun renewSubscription(id: String,productName: String, monthYear: String, newDate: Long): NetworkResult = firestore.renewSubscription(id, productName, monthYear, newDate)

    //subscription history
    suspend fun addCancellationDates(sub: SubscriptionEntity, date: Long): Boolean = firestore.addCancellationDates(sub, date)

    suspend fun cancelSubscription(sub: SubscriptionEntity): NetworkResult = firestore.cancelSubscription(sub)
//    fun getAllData(viewModel: HomeViewModel) = firestore.getAllData(viewModel)

    //wallet
    suspend fun createWallet(wallet: Wallet) = firestore.createWallet(wallet)

    suspend fun getWalletAmount(id: String): Float = firestore.getWalletAmount(id)

    //review listener
    suspend fun productReviewsListener(id: String, viewModel: ViewModel) = firestore.productReviewsListener(id, viewModel)

//    suspend fun getWallet(id: String): Wallet = firestore.getWallet(id)
    suspend fun getWallet(id: String): NetworkResult = firestore.getWallet(id)

    suspend fun getTransactions(id: String): List<TransactionHistory> = firestore.getTransactions(id)

    suspend fun makeTransactionFromWallet(amount: Float, id: String, status: String): Boolean = firestore.makeTransactionFromWallet(amount, id, status)

    suspend fun generateOrderID(): String = firestore.generateOrderID()

    suspend fun updateTransaction(transaction: TransactionHistory): NetworkResult = firestore.updateTransaction(transaction)
//    suspend fun updateTransaction(transaction: TransactionHistory): String = firestore.updateTransaction(transaction)

    //notifications
    suspend fun deleteNotification(notification: UserNotificationEntity): NetworkResult = firestore.deleteNotification(notification)
    suspend fun clearAllNotifications(allNotifications:MutableList<UserNotificationEntity>): NetworkResult = firestore.clearAllNotifications(allNotifications)


    //updateToken
    suspend fun updateToken(token: String) = firestore.updateToken(token)
    suspend fun getSupportToken(id: String): String = firestore.getSupportToken(id)

}