package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.PhoneAuthCredential
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel

class FirestoreRepository (
    private val firestore: Firestore
) {

    fun uid(): String = firestore.getCurrentUserId()

    val phNumber = firestore.getPhoneNumer()

    suspend fun getProfile(id: String): UserProfileEntity = firestore.getProfile(id)

    fun signInWithPhoneAuthCredential(activity: SignInActivity, credential: PhoneAuthCredential) = firestore.signInWithPhoneAuthCredential(activity, credential)

    suspend fun checkUserProfileDetails(): Boolean = firestore.checkUserProfileDetails()

    suspend fun uploadImage(path: String, uri: Uri, extension: String, data: String = ""): String = firestore.uploadImage(path, uri, extension, data)

    suspend fun uploadProfile(profile: UserProfile): Boolean = firestore.uploadProfile(profile)

//    fun getAllData() = firestore.getAllData()

//    fun getUpdatedDeliveryDetails() = firestore.getUpdatedDeliveryDetails()

    fun getLimitedItems(viewModel: ViewModel) = firestore.getLimitedItems(viewModel)

    fun addFavorties(id: String, item: String) = firestore.addFavorites(id, item)

    fun addAddress(id: String ,address: Address) = firestore.addAddress(id, address)

    fun removeFavorites(id: String, item: String) = firestore.removeFavorites(id, item)

    fun addReview(id: String, review: Review) = firestore.addReview(id, review)

    suspend fun limitedItemsUpdater(cartEntity: List<CartEntity>, viewModel: CheckoutViewModel) = firestore.limitedItemsUpdater(cartEntity, viewModel)

    fun placeOrder(order: Order, viewModel: CheckoutViewModel) = firestore.placeOrder(order, viewModel)

    fun signOut() = firestore.signOut()

    fun updateAddress(id: String, address: ArrayList<Address>)  = firestore.updateAddress(id, address)

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

    suspend fun updateTransaction(transaction: TransactionHistory): String = firestore.updateTransaction(transaction)

}