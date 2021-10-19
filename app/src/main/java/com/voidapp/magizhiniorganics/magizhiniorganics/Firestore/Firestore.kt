package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class Firestore(
    private val repository: DatabaseRepository
) {

    private val mFirebaseAuth = FirebaseAuth.getInstance()
    private val mFireStore = FirebaseFirestore.getInstance()
    private val mFireStoreStorage = FirebaseStorage.getInstance().reference

    suspend fun getProfile(id: String): UserProfileEntity {
        return mFireStore.collection(Constants.USERS)
            .document(id)
            .get().await().toObject(UserProfile::class.java)!!.toUserProfileEntity()
    }

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
    suspend fun checkUserProfileDetails(): Boolean = withContext(Dispatchers.IO) {
        try {
            val snapShot = mFireStore.collection(Constants.USERS)
                .document(mFirebaseAuth.currentUser!!.uid).get().await()
            //getting the profile data and if exists we do the code below or we dismiss the progress dialog for new profile creation
            if (snapShot.exists()) {
                //we create a profile object of the snapshot and converting it to profile entity class to update it in room database
                val profile = snapShot.toObject(UserProfile::class.java)
                val userProfileEntity = profile!!.toUserProfileEntity()
                repository.upsertProfile(userProfileEntity)
                return@withContext true
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e("exception", e.message.toString())
            return@withContext false
        }
    }

    suspend fun createWallet(wallet: Wallet) {
        try {
            mFireStore.collection("Wallet")
                .document("Wallet")
                .collection("Users")
                .document(wallet.id)
                .set(wallet, SetOptions.merge())
                .await()
        } catch (e: Exception) {}
    }

    suspend fun uploadImage (
        path: String,
        uri: Uri,
        extension: String
    ): String = withContext(Dispatchers.IO) {
                    val name = getCurrentUserId()
                    try {
                        val sRef: StorageReference = mFireStoreStorage.child(
                            "$path$name.$extension"
                        )

                        val url = sRef.putFile(uri)
                            .await().task.snapshot.metadata!!.reference!!.downloadUrl.await()

                       return@withContext url.toString()

                    } catch (e: Exception) {
                        return@withContext "failed"
                    }
                }

    suspend fun uploadProfile(profile: UserProfile): Boolean =
        withContext(Dispatchers.IO) {
            try {
                mFireStore.collection(Constants.USERS)
                    .document(profile.id)
                    .set(profile, SetOptions.merge()).await()
                val userProfileEntity = profile.toUserProfileEntity()
                repository.upsertProfile(userProfileEntity)
                return@withContext true
            } catch (e: Exception) {
                return@withContext false
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
                val updateProfileMonthYear = async { updateProfileMonthYear(order) }
                val updateProfileOrderID = async { updateProfileOrderID(order) }
                val updateLocalProfile = async { updateLocalProfile(order) }

                updateOrderHistory.await()
                updateProfileOrderID.await()
                updateProfileMonthYear.await()
                updateLocalProfile.await()

                withContext(Dispatchers.Main) {
                    viewModel.orderPlaced()
                }
            } catch (e: Exception) {
                viewModel.orderPlacementFailed(e.message.toString())
            }
        }

    private suspend fun updateProfileOrderID(order: Order) = withContext(Dispatchers.IO) {
        mFireStore.collection("users").document(order.customerId)
            .update("purchaseHistory", FieldValue.arrayUnion(order.orderId)).await()
    }

    private suspend fun updateProfileMonthYear(order: Order) = withContext(Dispatchers.IO) {
        mFireStore.collection("users").document(order.customerId)
            .update("purchasedMonths", FieldValue.arrayUnion(order.monthYear)).await()
    }

    private suspend fun updateLocalProfile(order: Order) = withContext(Dispatchers.IO) {
        val profile = repository.getProfileData()!!
        profile.purchaseHistory.add(order.orderId)
        if (!profile.purchasedMonths.contains(order.monthYear)) {
            profile.purchasedMonths.add(order.monthYear)
        }
        repository.upsertProfile(profile)
    }

    suspend fun cancelOrder(orderEntity: OrderEntity, viewModel: PurchaseHistoryViewModel) {
        try {
            withContext(Dispatchers.IO) {
                val cancelOrderStatus = async { cancelOrderStatus(orderEntity) }
                val removeActiveOrderFromProfile = async { updateCloudProfileRecentPurchases(orderEntity.orderId, orderEntity.customerId) }
                cancelOrderStatus.await()
                removeActiveOrderFromProfile.await()
                withContext(Dispatchers.Main) {
                    viewModel.orderCancelledCallback(true)
                }
            }
        } catch (e: Exception) {
            viewModel.orderCancelledCallback(false)
        }
    }

    private suspend fun cancelOrderStatus(order: OrderEntity) = withContext(Dispatchers.IO) {
        mFireStore.collection(Constants.ORDER_HISTORY)
            .document(order.monthYear)
            .collection(order.purchaseDate.take(2))
            .document(order.orderId)
            .update("orderStatus", Constants.CANCELLED).await()
    }

    private suspend fun updateOrderHistory(order: Order) = withContext(Dispatchers.IO) {
        val date = Time().getCurrentDateNumber()
        val id = "${Time().getMonth()}${Time().getYear()}"
        mFireStore.collection(Constants.ORDER_HISTORY)
            .document(id)
            .collection(date)
            .document(order.orderId)
            .set(order, SetOptions.merge()).await()
        repository.upsertOrder(order.toOrderEntity())
    }

    //updating the recent purchase status from the store when app is opened everytime
    suspend fun updateRecentPurchases(
        recentPurchaseIDs: ArrayList<String>,
        subscriptionIDs: ArrayList<String>
    ) = withContext(Dispatchers.Default) {

        val orders = async { getOrdersUpdate(recentPurchaseIDs) }
        val subscriptions = async { getSubscriptionsUpdate(subscriptionIDs) }

        orders.await()
        subscriptions.await()
    }

    private suspend fun getOrdersUpdate(recentPurchaseIDs: ArrayList<String>) =
        withContext(Dispatchers.IO) {
            try {
                if (recentPurchaseIDs.isNotEmpty()) {
                    for (orderID in recentPurchaseIDs) {
                        withContext(Dispatchers.IO) {
                            val orderRepo = repository.getOrderByID(orderID)
                            orderRepo?.let {
                                val docID = orderRepo.monthYear
                                val date = orderRepo.purchaseDate.take(2)
                                val doc = mFireStore.collection(Constants.ORDER_HISTORY)
                                    .document(docID)
                                    .collection(date)
                                    .document(orderID)
                                    .get().await()
                                val orderEntity = doc.toObject(Order::class.java)?.toOrderEntity()
                                orderEntity?.let { order ->
                                    if (order.orderStatus != Constants.PENDING) {
                                        val updateLocalProfileRecentPurchases =
                                            async { updateLocalProfileRecentPurchases(order.orderId) }
                                        val updateCloudProfileRecentPurchases = async {
                                            updateCloudProfileRecentPurchases(
                                                order.orderId,
                                                order.customerId
                                            )
                                        }
                                        val updateLocalOrderRepository =
                                            async { updateLocalOrderRepository(order) }
                                        updateLocalProfileRecentPurchases.await()
                                        updateCloudProfileRecentPurchases.await()
                                        updateLocalOrderRepository.await()
                                    }
                                }
                            }
                        }
                    }
                } else {

                }
            } catch (e: Exception) {
                Log.e("exception", "updateRecentPurchases: ${e.message}")
            }
        }

    private suspend fun getSubscriptionsUpdate(subscriptionIDs: ArrayList<String>) =
        withContext(Dispatchers.IO) {
            try {
                if (subscriptionIDs.isNotEmpty()) {
                    val profile = repository.getProfileData()!!
                    val path = FirebaseFirestore.getInstance()
                        .collection("Subscription")
                        .document("Active")
                    for (month in profile.subscribedMonths) {
                        val documents = path
                            .collection(month)
                            .whereEqualTo("customerID", profile.id)
                            .get().await()
                        for (doc in documents) {
                            val sub = doc.toObject(Subscription::class.java).toSubscriptionEntity()
                            repository.upsertSubscription(sub)
                        }
                    }
                } else {
                }
            } catch (e: Exception) {
                Log.e("exception", "updateRecentPurchases: ${e.message}")
            }
        }

    private suspend fun updateLocalOrderRepository(order: OrderEntity) =
        withContext(Dispatchers.IO) {
            repository.upsertOrder(order)
        }

    private suspend fun updateLocalProfileRecentPurchases(id: String?) =
        withContext(Dispatchers.IO) {
            val profile = repository.getProfileData()!!
            profile.purchaseHistory.remove(id)
            repository.upsertProfile(profile)
        }

    private suspend fun updateCloudProfileRecentPurchases(item: String?, docID: String) =
        withContext(Dispatchers.IO) {
            mFireStore.collection(Constants.USERS)
                .document(docID).update("purchaseHistory", FieldValue.arrayRemove(item)).await()
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

    suspend fun addCancellationDates(sub: SubscriptionEntity, date: Long): Boolean {
        try {
            mFireStore.collection(Constants.SUBSCRIPTION)
                .document(Constants.SUB_ACTIVE).collection(sub.monthYear).document(sub.id)
                .update("cancelledDates", FieldValue.arrayUnion(date)).await()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun cancelSubscription(sub: SubscriptionEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val deleteSub = async { deleteSubscription(sub) }
            val createCancellationSub = async { createCancellationSub(sub) }
            val removeActiveSubFromProfile = async { removeActiveSubFromProfile(sub) }

            deleteSub.await()
            createCancellationSub.await()
            removeActiveSubFromProfile.await()

            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    private suspend fun createCancellationSub(sub: SubscriptionEntity) {
        mFireStore.collection(Constants.SUBSCRIPTION)
            .document(Constants.SUB_CANCELLED)
            .collection(sub.monthYear)
            .document(sub.id)
            .set(sub, SetOptions.merge())
            .await()
    }

    private suspend fun deleteSubscription(sub: SubscriptionEntity) {
        mFireStore.collection(Constants.SUBSCRIPTION)
            .document(Constants.SUB_ACTIVE)
            .collection(sub.monthYear)
            .document(sub.id)
            .delete().await()
    }

    private suspend fun removeActiveSubFromProfile(sub: SubscriptionEntity) {
        mFireStore.collection(Constants.USERS)
            .document(sub.customerID).update("subscriptions", FieldValue.arrayRemove(sub.id)).await()
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

    suspend fun generateSubscription(
        viewModel: SubscriptionProductViewModel,
        subscription: Subscription
    ) = withContext(Dispatchers.IO) {
        val sub = subscription.toSubscriptionEntity()
        try {
//            sub.id = mFireStore.collection(Constants.SUBSCRIPTION)
//                .document(Constants.SUB_ACTIVE)
//                .collection(subscription.monthYear)
//                .document().id

            val updateStore = async { updateStoreSubscription(sub) }
            val updateLocal = async { updateLocalSubscription(sub) }
            val updateCloud = async { updateCloudProfileSubscription(sub) }

            updateStore.await()
            updateLocal.await()
            updateCloud.await()

            withContext(Dispatchers.Main) {
                viewModel.subscriptionAdded(sub)
            }
        } catch (e: Exception) {
            viewModel.subscriptionFailed("Server Error! Try again later")
        }
    }

    private suspend fun updateLocalSubscription(sub: SubscriptionEntity) =
        withContext(Dispatchers.IO) {
            val profile = repository.getProfileData()!!
            profile.subscriptions.add(sub.id)
            if (!profile.subscribedMonths.contains(sub.monthYear)) {
                profile.subscribedMonths.add(sub.monthYear)
            }
            repository.upsertProfile(profile)
        }

    private suspend fun updateStoreSubscription(sub: SubscriptionEntity) {
        mFireStore.collection(Constants.SUBSCRIPTION)
            .document(Constants.SUB_ACTIVE)
            .collection(sub.monthYear)
            .document(sub.id)
            .set(sub, SetOptions.merge()).await()

    }

    private suspend fun updateCloudProfileSubscription(sub: SubscriptionEntity) =
        withContext(Dispatchers.IO) {
            mFireStore.collection(Constants.USERS)
                .document(sub.customerID)
                .update("subscribedMonths", FieldValue.arrayUnion(sub.monthYear)).await()
            mFireStore.collection(Constants.USERS)
                .document(sub.customerID).update("subscriptions", FieldValue.arrayUnion(sub.id))
                .await()
        }

    suspend fun getWalletAmount(id: String): Float {
        return mFireStore.collection("Wallet")
            .document("Wallet")
            .collection("Users")
            .document(id).get().await().toObject(Wallet::class.java)!!.amount
    }

    suspend fun getWallet(id: String): Wallet =
        mFireStore.collection("Wallet")
            .document("Wallet")
            .collection("Users")
            .document(id).get().await().toObject(Wallet::class.java)!!

    suspend fun getTransactions(id: String): List<TransactionHistory> = withContext(Dispatchers.IO) {
        try {
            val transactions = mutableListOf<TransactionHistory>()
            val docs = mFireStore.collection("Wallet")
                .document("Transaction")
                .collection(id)
                .get().await()
            for (doc in docs) {
                val transaction = doc.toObject(TransactionHistory::class.java)
                transactions.add(transaction)
            }
            return@withContext transactions
        } catch (e: Exception) {
            return@withContext mutableListOf()
        }
    }

    suspend fun makeTransactionFromWallet(amount: Float, id: String, status: String): Boolean {
        try {
            withContext(Dispatchers.IO) {
                val path = mFireStore.collection("Wallet")
                    .document("Wallet")
                    .collection("Users")
                mFireStore.runTransaction { transaction ->
                    val wallet = transaction.get(path.document(id)).toObject(Wallet::class.java)
                    if (status == "Add") {
                        wallet!!.amount = wallet.amount + amount
                    } else {
                        wallet!!.amount = wallet.amount - amount
                    }
                    transaction.update(path.document(id), "amount", wallet.amount)
                    null
                }
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    suspend fun updateTransaction(transaction: TransactionHistory): String =
        withContext(Dispatchers.IO) {
            try {
                val path = mFireStore.collection("Wallet")
                    .document("Transaction")
                    .collection(transaction.fromID)
                transaction.id = path.document().id

                path.document(transaction.id).set(transaction, SetOptions.merge()).await()
                return@withContext path.id
            } catch (e: Exception) {
                return@withContext "failed"
            }
        }

    suspend fun generateOrderID(id: String): String =
        mFireStore.collection(Constants.ORDER_HISTORY).document().id

    suspend fun generateSubscriptionID(id: String): String =
        mFireStore.collection(Constants.SUBSCRIPTION)
            .document(Constants.SUB_ACTIVE)
            .collection(id)
            .document().id

}