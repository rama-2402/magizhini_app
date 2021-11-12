package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.PhoneAuthCredential
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryViewModel
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

    fun addFavorites(id: String, item: String) = firestore.addFavorites(id, item)

    fun removeFavorites(id: String, item: String) = firestore.removeFavorites(id, item)

    suspend fun addAddress(id: String ,address: Address) = firestore.addAddress(id, address)

    suspend fun updateAddress(id: String, address: ArrayList<Address>)  = firestore.updateAddress(id, address)



    fun getLimitedItems(viewModel: ViewModel) = firestore.getLimitedItems(viewModel)

    fun addReview(id: String, review: Review) = firestore.addReview(id, review)

    //checkout
    suspend fun limitedItemsUpdater(cartEntity: List<CartEntity>): NetworkResult = firestore.limitedItemsUpdater(cartEntity)

    suspend fun placeOrder(order: Order): NetworkResult = firestore.placeOrder(order)

    suspend fun validateItemAvailability(cartItems: List<CartEntity>): NetworkResult = firestore.validateItemAvailability(cartItems)

    //purchase history
    suspend fun cancelOrder(orderEntity: OrderEntity): NetworkResult = firestore.cancelOrder(orderEntity)

    //subscription
    suspend fun generateSubscription(viewModel: SubscriptionProductViewModel, subscription: Subscription) = firestore.generateSubscription(viewModel, subscription)

    //subscription history
    suspend fun addCancellationDates(sub: SubscriptionEntity, date: Long): Boolean = firestore.addCancellationDates(sub, date)

    suspend fun cancelSubscription(sub: SubscriptionEntity): Boolean = firestore.cancelSubscription(sub)
//    fun getAllData(viewModel: HomeViewModel) = firestore.getAllData(viewModel)

    //wallet
    suspend fun createWallet(wallet: Wallet) = firestore.createWallet(wallet)

    suspend fun getWalletAmount(id: String): Float = firestore.getWalletAmount(id)

//    suspend fun getWallet(id: String): Wallet = firestore.getWallet(id)
    suspend fun getWallet(id: String): NetworkResult = firestore.getWallet(id)

    suspend fun getTransactions(id: String): List<TransactionHistory> = firestore.getTransactions(id)

    suspend fun makeTransactionFromWallet(amount: Float, id: String, status: String): Boolean = firestore.makeTransactionFromWallet(amount, id, status)

    suspend fun generateOrderID(): String = firestore.generateOrderID()

    suspend fun generateSubscriptionID(id: String): String = firestore.generateSubscriptionID(id)

    suspend fun renewSubscription(id: String, monthYear: String, newDate: Long): Boolean = firestore.renewSubscription(id, monthYear, newDate)

    suspend fun updateTransaction(transaction: TransactionHistory): NetworkResult = firestore.updateTransaction(transaction)
//    suspend fun updateTransaction(transaction: TransactionHistory): String = firestore.updateTransaction(transaction)

}