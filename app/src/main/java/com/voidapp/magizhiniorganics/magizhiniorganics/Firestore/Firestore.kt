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
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
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
    suspend fun getProfile(id: String): UserProfileEntity {
        return try {
            mFireStore.collection(Constants.USERS)
                .document(id)
                .get().await().toObject(UserProfile::class.java)!!.toUserProfileEntity()
        } catch (e: Exception) {
            e.message?.let { logCrash("getting profile", it) }
            UserProfileEntity()
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

    suspend fun limitedItemsUpdater(cart: List<CartEntity>, viewModel: CheckoutViewModel) {
        try {
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

    //order placement
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
                val updateLocalOrderRepository =
                    async { updateLocalOrderRepository(order.toOrderEntity()) }

                updateOrderHistory.await()
                updateProfileOrderID.await()
                updateProfileMonthYear.await()
                updateLocalProfile.await()
                updateLocalOrderRepository.await()

                withContext(Dispatchers.Main) {
                    viewModel.orderPlaced()
                }
            } catch (e: Exception) {
                viewModel.orderPlacementFailed(e.message.toString())
            }
        }

    private suspend fun updateOrderHistory(order: Order) = withContext(Dispatchers.IO) {
        mFireStore.collection(Constants.ORDER_HISTORY)
            .document(order.monthYear)
            .collection("Active")
            .document(order.orderId)
            .set(order, SetOptions.merge())
            .await()
        repository.upsertOrder(order.toOrderEntity())
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
        ActiveOrders(order.orderId).also {
            repository.upsertActiveOrders(it)
        }
        val profile = repository.getProfileData()!!
        if (!profile.purchasedMonths.contains(order.monthYear)) {
            profile.purchasedMonths.add(order.monthYear)
        }
        repository.upsertProfile(profile)
    }

    private suspend fun updateLocalOrderRepository(order: OrderEntity) =
        withContext(Dispatchers.IO) {
            repository.upsertOrder(order)
        }

    suspend fun generateOrderID(id: String): String =
        mFireStore.collection(Constants.ORDER_HISTORY).document().id


    //cancelling order
    suspend fun cancelOrder(orderEntity: OrderEntity, viewModel: PurchaseHistoryViewModel) {
        try {
            withContext(Dispatchers.IO) {
                val removeFromActiveOrder = async { removeFromActiveOrder(orderEntity) }
                val addToCancelledOrder = async { addToCancelledOrder(orderEntity) }
                val removeActiveOrderFromProfile = async {
                    updateCloudProfileRecentPurchases(
                        orderEntity.orderId,
                        orderEntity.customerId
                    )
                }

                removeFromActiveOrder.await()
                addToCancelledOrder.await()
                removeActiveOrderFromProfile.await()
                withContext(Dispatchers.Main) {
                    viewModel.orderCancelledCallback(true)
                }
            }
        } catch (e: Exception) {
            viewModel.orderCancelledCallback(false)
        }
    }

    private suspend fun removeFromActiveOrder(order: OrderEntity) = withContext(Dispatchers.IO) {
        order.orderStatus = Constants.CANCELLED
        mFireStore.collection(Constants.ORDER_HISTORY)
                .document(order.monthYear)
                .collection("Cancelled")
                .document(order.orderId)
                .set(order, SetOptions.merge())
    }

    private suspend fun addToCancelledOrder(order: OrderEntity) = withContext(Dispatchers.IO) {
        mFireStore.collection(Constants.ORDER_HISTORY)
            .document(order.monthYear)
            .collection("Active")
            .document(order.orderId)
            .delete()
    }

    private suspend fun updateCloudProfileRecentPurchases(item: String?, docID: String) =
        withContext(Dispatchers.IO) {
            mFireStore.collection(Constants.USERS)
                .document(docID).update("purchaseHistory", FieldValue.arrayRemove(item)).await()
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
    fun addAddress(id: String, address: Address) = CoroutineScope(Dispatchers.IO).launch {
        mFireStore.collection(Constants.USERS)
            .document(id).update(Constants.ADDRESS, FieldValue.arrayUnion(address))
    }

    fun updateAddress(id: String, address: ArrayList<Address>) {
        mFireStore.collection(Constants.USERS)
            .document(id).update("address", address)
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

    suspend fun getWallet(id: String): Wallet =
        mFireStore.collection("Wallet")
            .document("Wallet")
            .collection("Users")
            .document(id).get().await().toObject(Wallet::class.java)!!

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
        try {
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

    suspend fun generateSubscriptionID(id: String): String =
        mFireStore.collection(Constants.SUBSCRIPTION)
            .document(Constants.SUB_ACTIVE)
            .collection(id)
            .document().id

    private suspend fun logCrash(location: String, message: String) {
        CrashLog(
            getCurrentUserId()!!,
            "${ Build.MANUFACTURER } ${ Build.MODEL } ${Build.VERSION.RELEASE} ${ Build.VERSION_CODES::class.java.fields[Build.VERSION.SDK_INT].name }",
            System.currentTimeMillis(),
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