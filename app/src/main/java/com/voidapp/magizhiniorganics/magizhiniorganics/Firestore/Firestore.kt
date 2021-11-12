package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore

import android.net.Uri
import android.os.Build
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
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.text.SimpleDateFormat

class Firestore(
    private val repository: DatabaseRepository
) {

    private val mFirebaseAuth = FirebaseAuth.getInstance()
    private val mFireStore = FirebaseFirestore.getInstance()
    private val mFireStoreStorage = FirebaseStorage.getInstance().reference

    fun signOut() = CoroutineScope(Dispatchers.IO).launch {
        val deleteLocalDB = async { deleteLocalDB() }
        deleteLocalDB.await()
        mFirebaseAuth.signOut()
    }

    private suspend fun deleteLocalDB() {
        repository.deleteUserProfile()
        repository.deleteActiveOrdersTable()
        repository.deleteActiveSubTable()
        repository.deleteOrdersTable()
        repository.deleteSubscriptionsTable()
    }

    suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential):Boolean {
        return try {
            var status: Boolean = false
            mFirebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener { status = true }
                .addOnCanceledListener { status = false }.await()
            status
        } catch (e: Exception) {
            e.message?.let { logCrash("signIn - phone Auth", it) }
            false
        }
    }

    // Checks if the current entered phone number is already present in DB before sending the OTP
    fun getPhoneNumber(): String? =
        mFirebaseAuth.currentUser!!.phoneNumber

    fun getCurrentUserId(): String? =
        mFirebaseAuth.currentUser?.uid

    //image upload
    suspend fun uploadImage(
        path: String,
        uri: Uri,
        extension: String,
        data: String = ""
    ): String = withContext(Dispatchers.IO) {
        val name = when (data) {
            "review" -> {
                val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
                "img_${(1..10).map { charset.random() }.joinToString("")}"
            }
            else -> getCurrentUserId()
        }

        try {
            val sRef: StorageReference = mFireStoreStorage.child(
                "$path$name.$extension"
            )

            val url = sRef.putFile(uri)
                .await().task.snapshot.metadata!!.reference!!.downloadUrl.await()

            return@withContext url.toString()

        } catch (e: Exception) {
            e.message?.let { logCrash("image upload", it) }
            return@withContext "failed"
        }
    }

    //check if the user profile exists and getting the data from store
    suspend fun checkUserProfileDetails(): String = withContext(Dispatchers.IO) {
        try {
            val snapShot = mFireStore.collection(Constants.USERS)
                .document(mFirebaseAuth.currentUser!!.uid).get().await()
            //getting the profile data and if exists we do the code below or we dismiss the progress dialog for new profile creation
            if (snapShot.exists()) {
                //we create a profile object of the snapshot and converting it to profile entity class to update it in room database
                val profile = snapShot.toObject(UserProfile::class.java)
                val userProfileEntity = profile!!.toUserProfileEntity()
                repository.upsertProfile(userProfileEntity)

                val uploadFavorites =
                    async { uploadFavorites(userProfileEntity.favorites) }
                val uploadActiveOrders =
                    async { uploadActiveOrders(userProfileEntity.purchaseHistory) }
                val uploadActiveSubscriptions =
                    async { uploadActiveSubscriptions(userProfileEntity.subscriptions) }

                return@withContext if(!(!uploadFavorites.await() ||
                        !uploadActiveOrders.await() ||
                        !uploadActiveSubscriptions.await())) {
                    profile.id
                } else {
                    "Failed"
                }
            } else {
                return@withContext ""
            }
        } catch (e: Exception) {
            e.message?.let { logCrash("checking user profile details", it) }
            return@withContext "Failed"
        }
    }

    private suspend fun uploadFavorites(favorites: List<String>) = withContext(Dispatchers.IO) {
        return@withContext try {
            favorites.forEach { fav ->
                Favorites(fav).also {
                    repository.upsertFavorite(it)
                }
            }
            true
        } catch (e: IOException) {
            e.message?.let { logCrash("uploading favorites to table in db", it) }
            false
        }
    }

    private suspend fun uploadActiveOrders(orders: List<String>) = withContext(Dispatchers.IO) {
        return@withContext try {
            orders.forEach { order ->
                ActiveOrders(order).also {
                    repository.upsertActiveOrders(it)
                }
            }
            true
        } catch (e: IOException) {
            e.message?.let { logCrash("uploading active orders to table in db", it) }
            false
        }
    }

    private suspend fun uploadActiveSubscriptions(subscriptions: List<String>) =
        withContext(Dispatchers.IO) {
            return@withContext try {
                subscriptions.forEach { sub ->
                    ActiveSubscriptions(sub).also {
                        repository.upsertActiveSubscription(it)
                    }
                }
                true
            } catch (e: IOException) {
                e.message?.let { logCrash("uploading active subs to table in db", it) }
                false
            }
        }

    //profile
//    suspend fun getProfile(id: String): UserProfileEntity {
//        return try {
//            mFireStore.collection(Constants.USERS)
//                .document(id)
//                .get().await().toObject(UserProfile::class.java)!!.toUserProfileEntity()
//        } catch (e: Exception) {
//            e.message?.let { logCrash("getting profile", it) }
//            UserProfileEntity()
//        }
//    }

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
                e.message?.let { logCrash("uploading profile", it) }
                return@withContext false
            }
        }

    //wallet
    suspend fun createWallet(wallet: Wallet) {
        try {
            mFireStore.collection("Wallet")
                .document("Wallet")
                .collection("Users")
                .document(wallet.id)
                .set(wallet, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.message?.let { logCrash("creating wallet", it) }
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

    suspend fun limitedItemsUpdater(cart: List<CartEntity>): NetworkResult {
        return try {
            withContext(Dispatchers.IO) {
                for (cartItem in cart) {
                    mFireStore.runTransaction { transaction ->
                        val productRef =
                            mFireStore.collection(Constants.PRODUCTS).document(cartItem.productId)
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
                NetworkResult.Success("limitedItems", "updated")
            }
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: fetching limited item count", it) }
            NetworkResult.Failed("limitedItems", "Server Error! Failed to Validate Items Purchase")
        }
    }

    //order placement
    suspend fun placeOrder(order: Order): NetworkResult =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val updateOrderHistory = async { updateOrderHistory(order) }
                val updateProfileOrderID = async { updateProfileOrderID(order) }
                val updateProfileMonthYear = async { updateProfileMonthYear(order) }
                val updateLocalProfile = async { updateLocalProfile(order) }
                val updateLocalOrderRepository =
                    async { updateLocalOrderRepository(order.toOrderEntity()) }

                if (
                    updateOrderHistory.await() &&
                    updateProfileOrderID.await() &&
                    updateProfileMonthYear.await() &&
                    updateLocalProfile.await() &&
                    updateLocalOrderRepository.await()
                ) {
                    NetworkResult.Success("orderPlacing", null)
                } else {
                    NetworkResult.Failed("orderPlacing", "Server Error! Failed to Place Order")
                }
            } catch (e: Exception) {
                e.message?.let { logCrash("firestore: order placement", it) }
                NetworkResult.Failed("orderPlacing", "Server Error! Failed to Place Order")
            }
        }

    private suspend fun updateOrderHistory(order: Order): Boolean {
        return try {
            mFireStore.collection(Constants.ORDER_HISTORY)
                .document(order.monthYear)
                .collection("Active")
                .document(order.orderId)
                .set(order, SetOptions.merge())
                .await()
            true
        } catch (e: IOException) {
            e.message?.let {
                logCrash("firestore: placing order cloud and db",
                    it
                )
            }
            false
        }
    }

    private suspend fun updateProfileOrderID(order: Order): Boolean {
        return try {
            mFireStore.collection("users").document(order.customerId)
                .update("purchaseHistory", FieldValue.arrayUnion(order.orderId)).await()
            true
        } catch (e: IOException) {
            e.message?.let {
                logCrash(
                    "firestore: adding orderID to profile in cloud",
                    it
                )
            }
            false
        }
    }

    private suspend fun updateProfileMonthYear(order: Order): Boolean {
        return try {
            mFireStore.collection("users").document(order.customerId)
                .update("purchasedMonths", FieldValue.arrayUnion(order.monthYear)).await()
            true
        } catch (e: IOException) {
            e.message?.let {
                logCrash(
                    "firestore: adding month in profile cloud",
                    it
                )
            }
            false
        }
    }

    private suspend fun updateLocalProfile(order: Order): Boolean {
        return try {
            ActiveOrders(order.orderId).also {
                repository.upsertActiveOrders(it)
            }
            val profile = repository.getProfileData()!!
            if (!profile.purchasedMonths.contains(order.monthYear)) {
                profile.purchasedMonths.add(order.monthYear)
            }
            repository.upsertProfile(profile)
            true
        } catch (e: IOException) {
            e.message?.let {
                logCrash(
                    "firestore: placing order cloud and db",
                    it
                )
            }
            false
        }
    }

    private suspend fun updateLocalOrderRepository(order: OrderEntity): Boolean {
        return try {
            repository.upsertOrder(order)
            true
        }catch (e: IOException) {
            e.message?.let {
                logCrash(
                    "firestore: adding order to order entitty db",
                    it
                )
            }
            false
        }
    }

    suspend fun generateOrderID(): String =
        mFireStore.collection(Constants.ORDER_HISTORY)
            .document("${TimeUtil().getMonth()}${TimeUtil().getYear()}")
            .collection("Active")
            .document().id

    //cancelling order
    suspend fun cancelOrder(orderEntity: OrderEntity): NetworkResult {
        return try {
            withContext(Dispatchers.IO) {
                val changeToCancelStatus = async { changeToCancelStatus(orderEntity) }
                val removeActiveOrderFromProfile = async {
                    updateCloudProfileRecentPurchases(
                        orderEntity.orderId,
                        orderEntity.customerId
                    )
                }
                val removeFromLocalDb = async { cancelOrderFromLocalDB(orderEntity.orderId) }

                if (
                    changeToCancelStatus.await() &&
                    removeActiveOrderFromProfile.await() &&
                    removeFromLocalDb.await()
                ) {
                    NetworkResult.Success("cancel", true)
                } else {
                    NetworkResult.Failed("cancel", false)
                }
            }
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: cancelling order", it) }
            NetworkResult.Failed("cancel", false)
        }
    }

    private suspend fun changeToCancelStatus(order: OrderEntity): Boolean {
        return try {
            mFireStore.collection(Constants.ORDER_HISTORY)
                .document(order.monthYear)
                .collection("Active")
                .document(order.orderId)
                .update("orderStatus", Constants.CANCELLED).await()
            true
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: change the order cancel status in cloud", it) }
            false
        }
    }

    private suspend fun updateCloudProfileRecentPurchases(item: String?, docID: String): Boolean {
        return try {
            mFireStore.collection(Constants.USERS)
                .document(docID).update("purchaseHistory", FieldValue.arrayRemove(item)).await()
            true
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: removing the order id from profile in cloud", it) }
            false
        }
    }

    private suspend fun cancelOrderFromLocalDB(id: String): Boolean {
        return try {
            repository.orderCancelled(id, Constants.CANCELLED)
            repository.cancelActiveOrder(id)
            true
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: making changes in the local db", it) }
            false
        }
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

    private suspend fun updateLocalProfileRecentPurchases(id: String?) =
        withContext(Dispatchers.IO) {
            val profile = repository.getProfileData()!!
            profile.purchaseHistory.remove(id)
            repository.upsertProfile(profile)
        }

    //favorites
    fun addFavorites(id: String, item: String) {
        //This function will add a new data if it is not present in the array
        mFireStore.collection(Constants.USERS)
            .document(id).update(Constants.FAVORITES, FieldValue.arrayUnion(item))
    }

    fun removeFavorites(id: String, item: String) {
        mFireStore.collection(Constants.USERS)
            .document(id).update(Constants.FAVORITES, FieldValue.arrayRemove(item))
    }

    //reviews
    fun addReview(id: String, review: Review) = CoroutineScope(Dispatchers.IO).launch {
        mFireStore.collection(Constants.PRODUCTS)
            .document(id).update(Constants.REVIEWS, FieldValue.arrayUnion(review))
    }

    //address
    suspend fun addAddress(id: String, address: Address) = CoroutineScope(Dispatchers.IO).launch {
        try {
            mFireStore.collection(Constants.USERS)
                .document(id).update(Constants.ADDRESS, FieldValue.arrayUnion(address))
        } catch (e: Exception) {
            e.message?.let { logCrash("Firestore: adding address", it) }
        }
    }

    suspend fun updateAddress(id: String, address: ArrayList<Address>) {
        try {
            mFireStore.collection(Constants.USERS)
                .document(id).update("address", address)
        } catch (e: Exception) {
            e.message?.let { logCrash("Firestore: updating address", it) }
        }
    }

    suspend fun addCancellationDates(sub: SubscriptionEntity, date: Long): Boolean {
        return try {
            mFireStore.collection(Constants.SUBSCRIPTION)
                .document(Constants.SUB_ACTIVE).collection(sub.monthYear).document(sub.id)
                .update("cancelledDates", FieldValue.arrayUnion(date)).await()

            val collectionID = SimpleDateFormat("dd")
            val docID = "${TimeUtil().getMonth()}${TimeUtil().getYear()}"

            mFireStore.collection("cancelledSubDelivery")
                .document(docID).collection(collectionID.format(date)).document(sub.id).set("").await()
            true
        } catch (e: Exception) {
            false
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
            .document(sub.customerID).update("subscriptions", FieldValue.arrayRemove(sub.id))
            .await()
        repository.cancelActiveSubscription(sub.id)
    }

    suspend fun validateItemAvailability(cartItems: List<CartEntity>): NetworkResult {
        val outOfStockItems: MutableList<CartEntity> = mutableListOf()
        outOfStockItems.clear()
        try {
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
            return NetworkResult.Success("ootItems", outOfStockItems as List<CartEntity>)
        } catch (e: Exception) {
            e.message?.let { logCrash("checkout: getting limited Items", it) }
            return NetworkResult.Failed("ootItems", "Server Error! Failed to validate purchase")
        }
    }
//    suspend fun validateItemAvailability(cartItems: List<CartEntity>): List<CartEntity> {
//        val outOfStockItems: MutableList<CartEntity> = mutableListOf()
//        outOfStockItems.clear()
//        for (cartItem in cartItems) {
//            val product =
//                mFireStore.collection(Constants.PRODUCTS).document(cartItem.productId).get().await()
//                    .toObject(Product::class.java)
//            for (variant in product!!.variants) {
//                val variantName = "${variant.variantName} ${variant.variantType}"
//                if (cartItem.variant == variantName && variant.status == Constants.OUT_OF_STOCK) {
//                    outOfStockItems.add(cartItem)
//                    break
//                }
//            }
//        }
//        return outOfStockItems
//    }

    suspend fun generateSubscription(
        viewModel: SubscriptionProductViewModel,
        subscription: Subscription
    ) = withContext(Dispatchers.IO) {
        val sub = subscription.toSubscriptionEntity()
        try {
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

    private suspend fun updateLocalSubscription(sub: SubscriptionEntity) {
        ActiveSubscriptions(sub.id).also {
            repository.upsertActiveSubscription(it)
        }
        val profile = repository.getProfileData()!!
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

    suspend fun renewSubscription(id: String, monthYear: String, newDate: Long):Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                mFireStore.collection(Constants.SUBSCRIPTION)
                    .document(Constants.SUB_ACTIVE)
                    .collection(monthYear)
                    .document(id)
                    .update("endDate", newDate).await()
                true
            }catch (e:Exception) {
                false
            }
        }

    //wallet
    suspend fun getWalletAmount(id: String): Float {
        return mFireStore.collection("Wallet")
            .document("Wallet")
            .collection("Users")
            .document(id).get().await().toObject(Wallet::class.java)!!.amount
    }

    suspend fun getWallet(id: String): NetworkResult {
        return try {
            val wallet = mFireStore.collection("Wallet")
                .document("Wallet")
                .collection("Users")
                .document(id).get().await().toObject(Wallet::class.java)!!
                NetworkResult.Success("wallet", wallet)
        } catch (e: Exception) {
            e.message?.let { logCrash("checkout: getting wallet", it) }
            NetworkResult.Failed("wallet", "Server Error. Failed to fetch Wallet.")
        }
    }

    //wallet transactions
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
        return try {
            withContext(Dispatchers.IO) {
                val path = mFireStore.collection("Wallet")
                    .document("Wallet")
                    .collection("Users")
                mFireStore.runTransaction { transaction ->
                    val wallet = transaction.get(path.document(id)).toObject(Wallet::class.java)
                    if (status == "Add") {
                        wallet!!.amount = wallet.amount + amount
                        wallet.lastRecharge = System.currentTimeMillis()
                        transaction.update(path.document(id), "amount", wallet.amount, "lastRecharge", wallet.lastRecharge)
                        null
                    } else {
                        wallet!!.amount = wallet.amount - amount
                        wallet.lastTransaction = System.currentTimeMillis()
                        transaction.update(path.document(id), "amount", wallet.amount, "lastTransaction", wallet.lastTransaction)
                        null
                    }
                }
                return@withContext true
            }
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: Making transaction for purchase", it) }
            return false
        }
    }

    suspend fun updateTransaction(transaction: TransactionHistory): NetworkResult =
        withContext(Dispatchers.IO) {
           try {
                val path = mFireStore.collection("Wallet")
                    .document("Transaction")
                    .collection(transaction.fromID)
                transaction.id = path.document().id

                path.document(transaction.id).set(transaction, SetOptions.merge()).await()
                return@withContext NetworkResult.Success("transactionID", path.id)
            } catch (e: Exception) {
               e.message?.let { logCrash("firestore: updating the wallet transaction", it) }
                return@withContext NetworkResult.Failed("transactionID", null)
            }
        }
//    suspend fun updateTransaction(transaction: TransactionHistory): String =
//        withContext(Dispatchers.IO) {
//            try {
//                val path = mFireStore.collection("Wallet")
//                    .document("Transaction")
//                    .collection(transaction.fromID)
//                transaction.id = path.document().id
//
//                path.document(transaction.id).set(transaction, SetOptions.merge()).await()
//                return@withContext path.id
//            } catch (e: Exception) {
//                return@withContext "failed"
//            }
//        }

    suspend fun generateSubscriptionID(id: String): String =
        mFireStore.collection(Constants.SUBSCRIPTION)
            .document(Constants.SUB_ACTIVE)
            .collection(id)
            .document().id

    suspend fun logCrash(location: String, message: String) {
        CrashLog(
            getCurrentUserId()!!,
            "${ Build.MANUFACTURER } ${ Build.MODEL } ${Build.VERSION.RELEASE} ${ Build.VERSION_CODES::class.java.fields[Build.VERSION.SDK_INT].name }",
            TimeUtil().getCustomDate("",System.currentTimeMillis()),
            location,
            message
        ).let {
            try {
                mFireStore.collection("crashLog")
                    .document(getCurrentUserId()!!)
                    .collection("MagizhiniApp")
                    .document()
                    .set(it, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.e("Magizhini", "logCrash: $it ", )
            }
        }
    }

}