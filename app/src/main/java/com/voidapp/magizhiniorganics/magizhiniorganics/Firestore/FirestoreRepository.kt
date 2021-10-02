package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.PhoneAuthCredential
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel

class FirestoreRepository (
    private val firestore: Firestore
) {

    fun uid(): String = firestore.getCurrentUserId()

    val phNumber = firestore.getPhoneNumer()

    fun signInWithPhoneAuthCredential(activity: SignInActivity, credential: PhoneAuthCredential) = firestore.signInWithPhoneAuthCredential(activity, credential)

    fun checkUserProfileDetails(activity: ProfileActivity) = firestore.checkUserProfileDetails(activity)

    fun uploadImage(activity: Activity, path: String, uri: Uri) = firestore.uploadImage(activity, path, uri)

    fun uploadData(activity: Activity, data: Any, content: String = "") = firestore.uploadData(activity, data, content)

//    fun getAllData() = firestore.getAllData()

    fun getProductsAndCouponsData() = firestore.getProductAndCouponData()

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

//    fun getAllData(viewModel: HomeViewModel) = firestore.getAllData(viewModel)

}