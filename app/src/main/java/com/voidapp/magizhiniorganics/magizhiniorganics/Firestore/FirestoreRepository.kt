package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.aminography.primecalendar.PrimeCalendar
import com.google.firebase.auth.PhoneAuthCredential
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Subscription
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.ProfileActivity
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

    fun checkUserProfileDetails(activity: ProfileActivity) = firestore.checkUserProfileDetails(activity)

    fun uploadImage(activity: Activity, path: String, uri: Uri) = firestore.uploadImage(activity, path, uri)

    fun uploadData(activity: Activity, data: Any, content: String = "") = firestore.uploadData(activity, data, content)

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

}