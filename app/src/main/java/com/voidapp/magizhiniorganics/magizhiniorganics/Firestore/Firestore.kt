package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.services.GetDataIntentService
import com.voidapp.magizhiniorganics.magizhiniorganics.services.GetOrderHistoryService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation.ConversationActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ThreadPoolExecutor

class Firestore(
    private val repository: DatabaseRepository
) {

    private val mFirebaseAuth = FirebaseAuth.getInstance()
    private val mFireStore = FirebaseFirestore.getInstance()
    private val mFireStoreStorage = FirebaseStorage.getInstance().reference

    fun signOut() = CoroutineScope(Dispatchers.IO).launch {
        repository.deleteUserProfile()
        mFirebaseAuth.signOut()
    }

    fun signInWithPhoneAuthCredential(activity: SignInActivity, credential: PhoneAuthCredential) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mFirebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        // Logged In
                        activity.loggedIn()
                    }
                    .addOnFailureListener { _ ->
                        // Failed
                        activity.onFirestoreFailure("Log In Failed Try Later")
                    }
            } catch (e: Exception) {
                // Failed
                activity.onFirestoreFailure("Log In Failed Try Later")
            }
        }

    // Checks if the current entered phone number is already present in DB before sending the OTP
    fun getCurrentUserId(): String {
        var currentUserId = ""
        val currentUser = mFirebaseAuth.currentUser
//        if (currentUser != null) {
//            currentUserId = currentUser.uid
//        }
        return currentUser!!.uid
    }

    fun getPhoneNumer(): String {
        val currentUser = mFirebaseAuth.currentUser
        var phNumber = ""
        if (currentUser != null) {
            phNumber = currentUser.phoneNumber.toString()
        }
        return phNumber
    }

    //check if the user profile exists and getting the data from store
    fun checkUserProfileDetails(activity: ProfileActivity) = CoroutineScope(Dispatchers.IO).launch {
        try {
            //setting the user id to shared preference for later editing the profile data
            SharedPref(activity.baseContext).putData(
                Constants.USER_ID,
                Constants.STRING,
                mFirebaseAuth.currentUser!!.uid
            )
            val snapShot = mFireStore.collection(Constants.USERS)
                .document(mFirebaseAuth.currentUser!!.uid).get().await()
            //getting the profile data and if exists we do the code below or we dismiss the progress dialog for new profile creation
            if (snapShot.exists()) {
                //we create a profile object of the snapshot and converting it to profile entity class to update it in room database
                val profile = snapShot.toObject(UserProfile::class.java)
                val userProfileEntity = profile!!.toUserProfileEntity()
                repository.upsertProfile(userProfileEntity)

                //calling a work manager background service to get the current month's order history from store
                //since it is a long running background task we are calling a work manager to do the task

                val currentMonthYear = "${Time().getMonth()}${Time().getYear()}"
                val workRequest: WorkRequest =
                    OneTimeWorkRequestBuilder<GetOrderHistoryService>()
                        .setInputData(workDataOf(
                            "filter" to currentMonthYear
                        ))
                        .build()

                WorkManager.getInstance(activity.applicationContext).enqueue(workRequest)

                //once the profile data is updated in the room database we skip the profile part and move the user to Home page
                withContext(Dispatchers.Main) {
                    activity.hideProgressDialog()
                    activity.newUserTransitionFromProfile()

                }
            } else {
                activity.hideProgressDialog()
            }
        } catch (e: Exception) {
            Log.e("exception", e.message.toString())
        }
    }

    fun uploadImage(activity: Activity, path: String, uri: Uri) =
        CoroutineScope(Dispatchers.IO).launch {

            when (activity) {
                is ProfileActivity -> {
                    val name = getCurrentUserId()
                    try {
                        val sRef: StorageReference = mFireStoreStorage.child(
                            "$path$name.${
                                GlideLoader().imageExtension(activity, uri)
                            }"
                        )

                        val url = sRef.putFile(uri)
                            .await().task.snapshot.metadata!!.reference!!.downloadUrl.await()

                        activity.onSuccessfulImageUpload(url.toString())

                    } catch (e: Exception) {
                        activity.onDataTransactionFailure(e.message.toString())
                    }
                }
                is ConversationActivity -> {
                    val name = getCurrentUserId()
                    try {
                        val sRef: StorageReference = mFireStoreStorage.child(
                            "$path$name.${
                                GlideLoader().imageExtension(activity, uri)
                            }"
                        )

                        val url = sRef.putFile(uri)
                            .await().task.snapshot.metadata!!.reference!!.downloadUrl.await()

                        activity.onSuccessfulImageUpload(url.toString())

                    } catch (e: Exception) {
                        activity.onDataTransactionFailure(e.message.toString())
                    }
                }
            }

        }

    fun uploadData(activity: Activity, data: Any, content: String = "") =
        CoroutineScope(Dispatchers.IO).launch {
            when (activity) {
                is ProfileActivity -> {
                    try {
                        data as UserProfile
                        mFireStore.collection(Constants.USERS)
                            .document(data.id)
                            .set(data, SetOptions.merge()).await()
                        val userProfileEntity = data.toUserProfileEntity()
                        repository.upsertProfile(userProfileEntity)
                        withContext(Dispatchers.Main) {
                            activity.onDataTransactionSuccess("")
                        }
                    } catch (e: Exception) {
                        activity.onDataTransactionFailure(e.toString())
                    }
                }
            }
        }

    fun getProductAndCouponData() = CoroutineScope(Dispatchers.IO).launch {
        try {
            val categoryData = async { getAllData(Constants.CATEGORY) }
            val bannerData = async { getAllData(Constants.BANNER) }
            val productData = async { getAllData(Constants.PRODUCTS) }
            val couponData = async { getAllData(Constants.COUPON) }
            val deliveryChargeData = async { getAllData(Constants.DELIVERY_CHARGE) }

            val categorySnapshot = categoryData.await()
            val bannerSnapshot = bannerData.await()
            val productSnapshot = productData.await()
            val couponSnapshot = couponData.await()
            val deliveryChargeSnapshot = deliveryChargeData.await()

            val updateCategory =
                async { filterDataAndUpdateRoom(Constants.CATEGORY, categorySnapshot) }
            val updateBanner = async { filterDataAndUpdateRoom(Constants.BANNER, bannerSnapshot) }
            val updateProduct =
                async { filterDataAndUpdateRoom(Constants.PRODUCTS, productSnapshot) }
            val updateCoupon = async { filterDataAndUpdateRoom(Constants.COUPON, couponSnapshot) }
            val updateDeliveryCharge =
                async { filterDataAndUpdateRoom(Constants.DELIVERY_CHARGE, deliveryChargeSnapshot) }

            updateCategory.await()
            updateBanner.await()
            updateProduct.await()
            updateCoupon.await()
            updateDeliveryCharge.await()

            updateEntityData()
            GetDataIntentService.stopService()
        } catch (e: Exception) {
            Log.e("exception", e.message.toString())
        }

    }

    private suspend fun filterDataAndUpdateRoom(content: String, snapshot: QuerySnapshot) =
        withContext(Dispatchers.Default) {
            when (content) {
                Constants.CATEGORY -> {
                    for (d in snapshot.documents) {
                        //getting the id of the category and converting to Category class
                        val category = d.toObject(ProductCategory::class.java)
                        category!!.id = d.id
                        //converting the category onject to CategoryEntity for updating the field in the room database
                        val categoryEntity = category.toProductCategoryEntity()
                        //updating the room - this add the new category and updates the existing ones
                        repository.upsertProductCategory(categoryEntity)
                    }
                }
                Constants.BANNER -> {
                    for (d in snapshot.documents) {
                        val banner = d.toObject(Banner::class.java)
                        banner!!.id = d.id
                        //creating a generic banner items array
                        val bannerEntity = banner.toBannerEntity()
                        repository.upsertBanner(bannerEntity)
                    }
                }
                Constants.PRODUCTS -> {
                    for (d in snapshot.documents) {
                        val product = d.toObject(Product::class.java)
                        product!!.id = d.id
                        val productEntity: ProductEntity = product.toProductEntity()
                        repository.upsertProduct(productEntity)
                    }
                }
                Constants.COUPON -> {
                    for (d in snapshot.documents) {
                        val coupon = d.toObject(Coupon::class.java)
                        coupon!!.id = d.id
                        val couponEntity = coupon.toCouponEntity()
                        repository.upsertCoupon(couponEntity)
                    }
                }
                Constants.DELIVERY_CHARGE -> {
                    for (d in snapshot.documents) {
                        val deliveryCode = d.toObject(PinCodes::class.java)
                        deliveryCode!!.id = d.id
                        val deliveryCodeEntity: PinCodesEntity = deliveryCode.toPinCodesEntity()
                        repository.upsertPinCodes(deliveryCodeEntity)
                    }
                }
            }
        }

    private suspend fun getAllData(content: String): QuerySnapshot = withContext(Dispatchers.IO) {
        when (content) {
            Constants.CATEGORY -> {
                mFireStore.collection(Constants.CATEGORY)
                    .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
                    .get().await()
            }
            Constants.PRODUCTS -> {
                mFireStore.collection(Constants.PRODUCTS)
                    .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
                    .get().await()
            }
            Constants.BANNER -> {
                mFireStore.collection(Constants.BANNER)
                    .orderBy(Constants.BANNER_ORDER, Query.Direction.ASCENDING)
                    .get().await()
            }
            Constants.COUPON -> {
                mFireStore.collection(Constants.COUPON)
                    .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
                    .whereEqualTo(Constants.STATUS, Constants.ACTIVE)
                    .get().await()
            }
            Constants.DELIVERY_CHARGE -> {
                mFireStore.collection("pincode")
                    .get().await()
            }
            else -> {
                mFireStore.collection(Constants.CATEGORY)
                    .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
                    .get().await()
            }
        }
    }

    //Function to update the products entity with user preferences like favorites, cart items and coupons added
    private fun updateEntityData() {
        try {
            val profile: UserProfileEntity? = repository.getProfileData()
            if (profile != null) {
                profile.favorites.forEach {
                    repository.updateFavorites(it, status = true)
                }
            }
            repository.getAllCartItemsForEntityUpdate().forEach {
                with(it) {
                    val product = repository.getProductWithIdForUpdate(productId)
                    product.variantInCart.add(it.variant)
                    repository.upsertProduct(product)
                    repository.updateCartItemsToEntity(productId, true, couponName)
                }
            }
        } catch (e: Exception) {
            Log.e("exception", e.message.toString())
        }
    }

    //live update of the limited items
    fun getLimitedItems(viewModel: ViewModel) {
        mFireStore.collection(Constants.PRODUCTS)
            .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, fireSnapshotFailure ->
                //error handling
                fireSnapshotFailure?.let {
                    Log.e(Constants.APP_NAME, it.message.toString())
                }
                snapshot?.let {
                    val mutableLimitedItems: MutableList<ProductEntity> = mutableListOf()
                    loop@ for (d in it.documents) {
                        //we take each product object
                        val product = d.toObject(Product::class.java)
                        //if there is some content
                        product?.let {
                            product.id = d.id
                            for (i in 0 until product.variants.size) {
                                //we are checking all the variants of the product if the variant is limited or not.
                                //if atleast any one of the variant is limited then we add the whole product to the list
                                if (product.variants[i].status == Constants.LIMITED) {
                                    val productEntity = product.toProductEntity()
                                    mutableLimitedItems.add(productEntity)
                                    break
                                }
                            }
                        }
                    }
                    when (viewModel) {
                        is ShoppingMainViewModel -> viewModel.limitedProducts(mutableLimitedItems)
                        is ProductViewModel -> viewModel.limitedProducts(mutableLimitedItems)
                    }
                }
            }
    }

    suspend fun limitedItemsUpdater(cart: List<CartEntity>, viewModel: CheckoutViewModel) {
        try {
            withContext(Dispatchers.IO) {
                for (cartItem in cart) {
                    mFireStore.runTransaction { transaction ->
                        val productRef =
                            mFireStore.collection("products").document(cartItem.productId)
                        val product = transaction.get(productRef).toObject(Product::class.java)
                        val variants = product!!.variants
                        variants[cartItem.variantIndex].inventory =
                            variants[cartItem.variantIndex].inventory - cartItem.quantity
                        if (variants[cartItem.variantIndex].inventory <= 0) {
                            variants[cartItem.variantIndex].status = Constants.OUT_OF_STOCK
                            val productEntity =
                                repository.getProductWithIdForUpdate(cartItem.productId)
                            productEntity.variants[cartItem.variantIndex].status =
                                Constants.OUT_OF_STOCK
                            productEntity.variants[cartItem.variantIndex].inventory = 0
                            repository.upsertProduct(productEntity)
                        }
                        transaction.update(productRef, "variants", variants)
                        null
                    }
                }
                withContext(Dispatchers.Main) {
                    viewModel.limitedItemsUpdated()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                viewModel.orderPlacementFailed(e.message.toString())
            }
        }
    }

    fun placeOrder(order: Order, viewModel: CheckoutViewModel) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firebase: Firebase = Firebase
                val id = firebase.firestore.collection(Constants.ORDER_HISTORY).document().id
                order.orderId = id
                val updateOrderHistory = async { updateOrderHistory(order) }
//                val updateOrderTransaction = async { updateOrderTransaction(order, id) }
                val updateProfile = async { updateProfile(order) }

                updateOrderHistory.await()
//                updateOrderTransaction.await()
                updateProfile.await()

                withContext(Dispatchers.Main) {
                    viewModel.orderPlaced()
                }
            } catch (e: Exception) {
                viewModel.orderPlacementFailed(e.message.toString())
            }
        }

    private suspend fun updateOrderHistory(order: Order) = withContext(Dispatchers.IO) {
        val date = Time().getCurrentDateNumber()
        val id = "${Time().getMonth()}${Time().getYear()}"
        mFireStore.collection(Constants.ORDER_HISTORY)
            .document(id)
            .collection(date)
            .document(order.orderId)
            .set(order, SetOptions.merge())
        repository.upsertOrder(order.toOrderEntity())
//            .addOnSuccessListener {
//                val orderEntity = order.toOrderEntity()
//                viewModel.orderPlaced(orderEntity, id)
//            }
    }

//    private suspend fun updateOrderTransaction(order: Order, id: String) =
//        withContext(Dispatchers.IO) {
//            //run the transaction to update the total ordered items to display the number in the dashboard
//        }

    private suspend fun updateProfile(order: Order) = withContext(Dispatchers.IO) {
        mFireStore.collection("users").document(order.customerId)
            .update("purchaseHistory", FieldValue.arrayUnion(order.orderId))
        val profile = repository.getProfileData()
        profile!!.purchaseHistory.add(order.orderId)
        repository.upsertProfile(profile)
    }

    fun addFavorites(id: String, item: String) = CoroutineScope(Dispatchers.IO).launch {
        //This function will add a new data if it is not present in the array
        mFireStore.collection(Constants.USERS)
            .document(id).update(Constants.FAVORITES, FieldValue.arrayUnion(item))
    }

    fun removeFavorites(id: String, item: String) = CoroutineScope(Dispatchers.IO).launch {
        mFireStore.collection(Constants.USERS)
            .document(id).update(Constants.FAVORITES, FieldValue.arrayRemove(item))
    }

    fun addReview(id: String, review: Review) = CoroutineScope(Dispatchers.IO).launch {
        mFireStore.collection(Constants.PRODUCTS)
            .document(id).update(Constants.REVIEWS, FieldValue.arrayUnion(review))
    }

    fun addAddress(id: String, address: Address) = CoroutineScope(Dispatchers.IO).launch {
        mFireStore.collection(Constants.USERS)
            .document(id).update(Constants.ADDRESS, FieldValue.arrayUnion(address))
    }

    fun updateAddress(id: String, address: ArrayList<Address>) {
        mFireStore.collection(Constants.USERS)
            .document(id).update("address", address)
    }

    suspend fun validateItemAvailability(cartItems: List<CartEntity>): List<CartEntity> {
        val outOfStockItems: MutableList<CartEntity> = mutableListOf()
        outOfStockItems.clear()
        for (cartItem in cartItems) {
            val product =
                mFireStore.collection(Constants.PRODUCTS).document(cartItem.productId).get().await()
                    .toObject(Product::class.java)
            for (variant in product!!.variants) {
                val variantName = "${variant.variantName} ${variant.variantType}"
                if (cartItem.variant == variantName && variant.status == Constants.OUT_OF_STOCK) {
                    outOfStockItems.add(cartItem)
                    break
                }
            }
        }
        return outOfStockItems
    }
}