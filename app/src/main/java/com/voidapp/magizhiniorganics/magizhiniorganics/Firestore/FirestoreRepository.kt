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
    suspend fun getProfile(id: String): UserProfileEntity = firestore.getProfile(id)

    suspend fun uploadProfile(profile: UserProfile): Boolean = firestore.uploadProfile(profile)

    fun addFavorites(id: String, item: String) = firestore.addFavorites(id, item)

    fun removeFavorites(id: String, item: String) = firestore.removeFavorites(id, item)

    fun addAddress(id: String ,address: Address) = firestore.addAddress(id, address)

    fun updateAddress(id: String, address: ArrayList<Address>)  = firestore.updateAddress(id, address)



    fun getLimitedItems(viewModel: ViewModel) = firestore.getLimitedItems(viewModel)

    fun addReview(id: String, review: Review) = firestore.addReview(id, review)

    suspend fun limitedItemsUpdater(cartEntity: List<CartEntity>, viewModel: CheckoutViewModel) = firestore.limitedItemsUpdater(cartEntity, viewModel)

    fun placeOrder(order: Order, viewModel: CheckoutViewModel) = firestore.placeOrder(order, viewModel)



    suspend fun validateItemAvailability(cartItems: List<CartEntity>): List<CartEntity> = firestore.validateItemAvailability(cartItems)

    suspend fun updateRecentPurchases(recentPurchaseIDs: ArrayList<String>, subscriptionIDs: ArrayList<String>) = firestore.updateRecentPurchases(recentPurchaseIDs, subscriptionIDs)

    suspend fun cancelOrder(orderEntity: OrderEntity, viewModel: PurchaseHistoryViewModel) = firestore.cancelOrder(orderEntity, viewModel)

    suspend fun generateSubscription(viewModel: SubscriptionProductViewModel, subscription: Subscription) = firestore.generateSubscription(viewModel, subscription)

    suspend fun addCancellationDates(sub: SubscriptionEntity, date: Long): Boolean = firestore.addCancellationDates(sub, date)

    suspend fun cancelSubscription(sub: SubscriptionEntity): Boolean = firestore.cancelSubscription(sub)
//    fun getAllData(viewModel: HomeViewModel) = firestore.getAllData(viewModel)

    //wallet
    suspend fun createWallet(wallet: Wallet) = firestore.createWallet(wallet)

    suspend fun getWalletAmount(id: String): Float = firestore.getWalletAmount(id)

    suspend fun getWallet(id: String): Wallet = firestore.getWallet(id)

    suspend fun getTransactions(id: String): List<TransactionHistory> = firestore.getTransactions(id)

    suspend fun makeTransactionFromWallet(amount: Float, id: String, status: String): Boolean = firestore.makeTransactionFromWallet(amount, id, status)

    suspend fun generateOrderID(id: String): String = firestore.generateOrderID(id)

    suspend fun generateSubscriptionID(id: String): String = firestore.generateSubscriptionID(id)

    suspend fun renewSubscription(id: String, monthYear: String, newDate: Long): Boolean = firestore.renewSubscription(id, monthYear, newDate)

    suspend fun updateTransaction(transaction: TransactionHistory): String = firestore.updateTransaction(transaction)

}