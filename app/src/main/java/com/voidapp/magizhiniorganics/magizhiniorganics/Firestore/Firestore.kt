package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.work.ListenableWorker
import androidx.work.Operation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ADD_MONEY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL_DISHES
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CWM
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.MONTHLY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NONE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.REFERRAL
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUB
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUB_ACTIVE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUCCESS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.TOKENS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.UNSUB
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USERS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_NOTIFICATIONS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.text.SimpleDateFormat

class Firestore(
    private val repository: DatabaseRepository,
    private val fbRepository: FirebaseRepository
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
        repository.deleteAllNotifications()
        repository.deleteAllFavorites()
    }

    suspend fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential): Boolean {
        return try {
            var status: Boolean = false
            mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                     status = task.isSuccessful
                }.await()
//                .addOnSuccessListener { status = true }
//                .addOnCanceledListener { status = false }.await()
            status
        } catch (e: Exception) {
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

                return@withContext if (!(!uploadFavorites.await() ||
                            !uploadActiveOrders.await() ||
                            !uploadActiveSubscriptions.await())
                ) {
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

    suspend fun uploadProfile(profile: UserProfile): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val uploadProfileToCloud = async { uploadProfileToCloud(profile) }
                val uploadProfileToLocal = async { uploadProfileToLocal(profile) }
                val upsertCustomerProfile = async { fbRepository.uploadUserProfile(profile.toCustomerProfile()) }
                return@withContext uploadProfileToCloud.await() &&
                        uploadProfileToLocal.await() &&
                        upsertCustomerProfile.await()
            } catch (e: Exception) {
                e.message?.let { logCrash("uploading profile", it) }
                return@withContext false
            }
        }

    private suspend fun uploadProfileToCloud(profile: UserProfile): Boolean {
        return try {
            mFireStore.collection(Constants.USERS)
                .document(profile.id)
                .set(profile, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun uploadProfileToLocal(profile: UserProfile): Boolean {
        return try {
            val userProfileEntity = profile.toUserProfileEntity()
            repository.upsertProfile(userProfileEntity)
            true
        } catch (e: IOException) {
            false
        }
    }

    suspend fun applyReferralNumber(currentUserID: String, code: String, fromHomeMenu: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val profileDoc = mFireStore.collection(USERS).whereEqualTo("phNumber", "+91$code").get().await()
            if (profileDoc.documents.isNullOrEmpty()) {
                false
            } else {
                val profile = profileDoc.documents[0].toObject(UserProfile::class.java)
                if (fromHomeMenu) {
                    mFireStore.collection(USERS).document(currentUserID).update("referrerNumber", code).await()
                    val currentUserProfile = repository.getProfileData()!!
                    currentUserProfile.referralId = code
                    repository.upsertProfile(currentUserProfile)
                }

                val checkReferralLimit = async { checkReferralLimit(profile!!.id)  }
                val addReferralBonusToReferrer = async { addReferralBonusToReferrer(profile!!) }
                val addReferralBonusToCurrentUser = async { addReferralBonusToCurrentUser(currentUserID) }

                if (checkReferralLimit.await()) {
                    addReferralBonusToReferrer.await() &&
                    addReferralBonusToCurrentUser.await()
                } else {
                    addReferralBonusToCurrentUser.await()
                }
            }
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: applying referral", it) }
            false
        }
    }

    private suspend fun checkReferralLimit(referrerID: String): Boolean = withContext(Dispatchers.IO) {
        data class Referral(
            var totalReferrals: Int = 0
        )
        return@withContext try {
            val referralLimit = mFireStore.collection(REFERRAL).document(referrerID).get().await()
            if (referralLimit.exists()) {
                val referral = referralLimit.toObject(Referral::class.java)!!
                if (referral.totalReferrals != 10) {
                    referral.totalReferrals = referral.totalReferrals + 1
                    mFireStore.collection(REFERRAL).document(referrerID).update("totalReferrals", referral.totalReferrals).await()
                    true
                } else {
                    false
                }
            } else {
                Referral(
                    1
                ).let {
                    mFireStore.collection(REFERRAL).document(referrerID).set(it, SetOptions.merge()).await()
                    true
                }
            }
        } catch (e: Exception) {
          e.message?.let { logCrash("firestore: verifying referral limit", it) }
          false
        }
    }

    data class Referral (
        var referralAmount: Float = 0f,
        var referrerAmount: Float = 0f
    )

    private suspend fun addReferralBonusToCurrentUser(currentUserID: String): Boolean = withContext(Dispatchers.IO){
        return@withContext try {
            if (currentUserID.isNullOrEmpty()) {
                return@withContext false
            }
            val amount = mFireStore.collection(REFERRAL).document(REFERRAL).get().await().toObject(Referral::class.java)!!.referralAmount
            val addReferralMoneyToWallet = async { makeTransactionFromWallet(amount, currentUserID, "Add") }
            val createTransactionEntry = async { TransactionHistory(
                id = "",
                timestamp = System.currentTimeMillis(),
                month = TimeUtil().getMonth(),
                year = TimeUtil().getYear().toLong(),
                amount = amount,
                fromID = currentUserID,
                fromUPI = "Magizhini Referral Program",
                status = SUCCESS,
                purpose = ADD_MONEY,
                transactionFor = "Magizhini Referral Bonus"
            ).let {
                when(updateTransaction(it)) {
                    is NetworkResult.Failed -> false
                    else -> true
                }
            } }
            val createNotification = async { UserNotification(
                id = "",
                userID = currentUserID,
                timestamp =  TimeUtil().getCurrentYearMonthDate().toString().toLong(),
                title = "Referral Bonus",
                message = "You have received a referral Bonus of Rs: $amount to your Wallet as a part of Magizhini Referral Program.",
                imageUrl = "",
                clickType = WALLET,
                clickContent = ""
            ).let {
                createNewNotification(it)
            } }

            addReferralMoneyToWallet.await() &&
            createTransactionEntry.await() &&
            createNotification.await()

        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: adding referral money to the new member", it) }
            false
        }
    }

    private suspend fun addReferralBonusToReferrer(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val amount = mFireStore.collection(REFERRAL).document(REFERRAL).get().await().toObject(Referral::class.java)!!.referrerAmount
            val addReferralMoneyToWallet = async { makeTransactionFromWallet(amount, profile.id, "Add") }
            val createTransactionEntry = async { TransactionHistory(
                id = "",
                timestamp = System.currentTimeMillis(),
                month = TimeUtil().getMonth(),
                year = TimeUtil().getYear().toLong(),
                amount = amount,
                fromID = profile.id,
                fromUPI = "Magizhini Referral Program",
                status = SUCCESS,
                purpose = ADD_MONEY
            ).let {
                when(updateTransaction(it)) {
                    is NetworkResult.Success -> true
                    else -> false
                }
            } }
            val createNotification = async { UserNotification(
                id = "",
                userID = profile.id,
                timestamp =  TimeUtil().getCurrentYearMonthDate().toString().toLong(),
                title = "Referral Bonus",
                message = "You have received a referral Bonus of Rs: $amount to your Wallet as a part of Magizhini Referral Program.",
                imageUrl = "",
                clickType = WALLET,
                clickContent = ""
            ).let {
                createNewNotification(it)
            } }

            addReferralMoneyToWallet.await() &&
            createTransactionEntry.await() &&
            createNotification.await()

        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: applying referral", it) }
            false
        }
    }

    suspend fun checkForReferral(userID: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserID = if (userID.isNullOrEmpty()) {
                getCurrentUserId()!!
            } else {
                userID
            }
            val doc = mFireStore.collection(WALLET)
                        .document(WALLET)
                        .collection("Users")
                        .document(currentUserID)
                .get().await().toObject(Wallet::class.java)
            if (doc != null) {
                return@withContext doc.amount != 0f
            }else {
                return@withContext false
            }
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: checking for existing referral", it) }
            return@withContext false
        }
    }

        //wallet
    suspend fun createWallet(wallet: Wallet) = withContext(Dispatchers.IO) {
        try {
            mFireStore.collection(WALLET)
                .document(WALLET)
                .collection("Users")
                .document(wallet.id)
                .set(wallet, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.message?.let { logCrash("creating wallet", it) }
        }
    }

    //live update of the limited items
    suspend fun getLimitedItems(viewModel: ShoppingMainViewModel) = withContext(Dispatchers.IO) {
        try {
            mFireStore.collection(Constants.PRODUCTS)
                .whereEqualTo("activated", true)
                .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, fireSnapshotFailure ->
                    //error handling
                    fireSnapshotFailure?.let {
                        Log.e(Constants.APP_NAME, it.message.toString())
                    }

                    snapshot?.let { filterProducts(snapshot, viewModel) }

//                    snapshot?.let {
//                        val mutableLimitedItems: MutableList<ProductEntity> = mutableListOf()
//                        for (d in it.documents) {
////                            val prod = it.documents.map { doc -> doc.toObject(ProductEntity::class.java) }
//                            //we take each product object
//                            val product = d.toObject(Product::class.java)
//                            //if there is some content
//                            product?.let { it ->
//                                it.id = d.id
//                                for (i in 0 until it.variants.size) {
//                                    //we are checking all the variants of the product if the variant is limited or not.
//                                    //if atleast any one of the variant is limited then we add the whole product to the list
//                                    if (it.variants[i].status == Constants.LIMITED) {
//                                        val productEntity = it.toProductEntity()
//                                        mutableLimitedItems.add(productEntity)
//                                        break
//                                    }
//                                }
//                            }
//                        }
//                    }

                }

        } catch (e: Exception) {

        }
    }

    private fun filterProducts(snapshot: QuerySnapshot, viewModel: ShoppingMainViewModel)
    = CoroutineScope(Dispatchers.Default).launch {
        val favorites = mutableListOf<String>()
        val cartItems = repository.getAllCartItemsForEntityUpdate()

        repository.getFavorites()?.let {
            it.forEach { fav ->
                favorites.add(fav.id)
            }
        }

        val mutableLimitedItems: MutableList<ProductEntity> = mutableListOf()
            for (d in snapshot.documents) {
//                            val prod = it.documents.map { doc -> doc.toObject(ProductEntity::class.java) }
                //we take each product object
                val product = d.toObject(Product::class.java)
                //if there is some content
                product?.let { it ->
                    it.id = d.id
                    for (i in 0 until it.variants.size) {
                        //we are checking all the variants of the product if the variant is limited or not.
                        //if atleast any one of the variant is limited then we add the whole product to the list
                        if (it.variants[i].status == Constants.LIMITED) {
                            val productEntity = it.toProductEntity()
                            if (favorites.contains(product.id)) {
                                productEntity.favorite = true
                            }
                            cartItems.forEach { item ->
                                if (item.productId == productEntity.id) {
                                    productEntity.inCart = true
                                    productEntity.variantInCart.add(item.variant)
                                }
                            }
                            mutableLimitedItems.add(productEntity)
                            break
                        }
                    }
                }
            }
        viewModel.limitedProducts(mutableLimitedItems)
    }

//    //Function to update the products entity with user preferences like favorites, cart items and coupons added
//    private suspend fun updateEntityData(viewModel: ShoppingMainViewModel, products: MutableList<ProductEntity>) = withContext(Dispatchers.IO) {
//        try {
//            val favorites: List<Favorites>? = repository.getFavorites()
//            favorites?.forEach {
//                repository.updateProductFavoriteStatus(it.id, status = true)
//            }
//            repository.getAllCartItemsForEntityUpdate().forEach {
//                with(it) {
//                    val product = repository.getProductWithIdForUpdate(productId)
//                    product.variantInCart.add(it.variant)
//                    repository.upsertProduct(product)
//                    repository.updateCartItemsToEntity(productId, true, couponName)
//                }
//            }
//        } catch (e: Exception) {
//            Log.e("exception", e.message.toString())
//        }
//    }

    suspend fun productListener(id: String, viewModel: ProductViewModel) = withContext(Dispatchers.IO) {
        try {
            mFireStore.collection(Constants.PRODUCTS)
                .document(id)
                .addSnapshotListener { snapshot, fireSnapshotFailure ->
                    //error handling
                    fireSnapshotFailure?.let { error ->
                        throw (error)
                    }
                    snapshot?.let {
                        it.toObject(Product::class.java)?.variants?.let { variants ->
                            viewModel.updateLimitedVariant(
                                variants
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            e.message?.let { logCrash("setting limited Item listener for the product", it) }
        }
    }

    //invoice - checking if all the products are available
    suspend fun validateItemAvailability(cartItems: List<CartEntity>): NetworkResult =
        withContext(Dispatchers.IO) {
            val outOfStockItems: MutableList<CartEntity> = mutableListOf()
            outOfStockItems.clear()
            return@withContext try {
                for (cartItem in cartItems) {
                    val product =
                        mFireStore.collection(Constants.PRODUCTS).document(cartItem.productId).get()
                            .await()
                            .toObject(Product::class.java)
                    for (variant in product!!.variants) {
                        val variantName = "${variant.variantName} ${variant.variantType}"
                        if (cartItem.variant == variantName && variant.status == Constants.OUT_OF_STOCK) {
                            outOfStockItems.add(cartItem)
                            break
                        }
                    }
                }
                NetworkResult.Success("ootItems", outOfStockItems as List<CartEntity>)
            } catch (e: Exception) {
                e.message?.let { logCrash("checkout: getting limited Items", it) }
                NetworkResult.Failed("ootItems", "Server Error! Failed to validate purchase")
            }
        }

    //after placing order reduces the limited items in stock based on purchase quantity
    suspend fun limitedItemsUpdater(cart: List<CartEntity>): NetworkResult =
        withContext(Dispatchers.IO) {
            return@withContext try {
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
                            val entity =
                                repository.getProductWithIdForUpdate(cartItem.productId)
                            entity?.let { productEntity ->
                                productEntity.variants[cartItem.variantIndex].status =
                                    Constants.OUT_OF_STOCK
                                productEntity.variants[cartItem.variantIndex].inventory = 0
                                repository.upsertProduct(productEntity)
                            }
                        }
                        transaction.update(productRef, "variants", variants)
                        null
                    }
                }
                NetworkResult.Success("limitedItems", "updated")
            } catch (e: Exception) {
                e.message?.let { logCrash("firestore: fetching limited item count", it) }
                NetworkResult.Failed(
                    "limitedItems",
                    "Server Error! Failed to Validate Items Purchase"
                )
            }
        }

    //order placement
    suspend fun placeOrder(order: Order): NetworkResult =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val path = mFireStore.collection(Constants.ORDER_HISTORY)
                    .document(order.monthYear).get().await()

                val updateOrderHistory = async { updateOrderHistory(path, order) }
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
                e.message?.let { logCrash("firestore: order placement parent job", it) }
                NetworkResult.Failed("orderPlacing", "Server Error! Failed to Place Order")
            }
        }

    private suspend fun updateOrderHistory(path: DocumentSnapshot, order: Order): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (path.exists()) {
                    mFireStore.collection(Constants.ORDER_HISTORY)
                        .document(order.monthYear)
                        .collection("Active")
                        .document(order.orderId)
                        .set(order, SetOptions.merge())
                        .await()
                } else {
                    val dummyData = ActiveSubscriptions("")
                    mFireStore.collection(Constants.ORDER_HISTORY)
                        .document(order.monthYear).set(dummyData, SetOptions.merge()).await()
                    mFireStore.collection(Constants.ORDER_HISTORY)
                        .document(order.monthYear)
                        .collection("Active")
                        .document(order.orderId)
                        .set(order, SetOptions.merge())
                        .await()
                }
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

    private suspend fun updateProfileOrderID(order: Order): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
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

    private suspend fun updateProfileMonthYear(order: Order): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
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

    private suspend fun updateLocalProfile(order: Order): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
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

    private suspend fun updateLocalOrderRepository(order: OrderEntity): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                repository.upsertOrder(order)
                true
            } catch (e: IOException) {
                e.message?.let {
                    logCrash(
                        "firestore: adding order to order entity db",
                        it
                    )
                }
                false
            }
        }

    fun generateOrderID(): String =
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
            e.message?.let { logCrash("firestore: cancelling order parent job", it) }
            NetworkResult.Failed("cancel", false)
        }
    }

    private suspend fun changeToCancelStatus(order: OrderEntity): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                mFireStore.collection(Constants.ORDER_HISTORY)
                    .document(order.monthYear)
                    .collection("Active")
                    .document(order.orderId)
                    .update("orderStatus", Constants.CANCELLED).await()
                true
            } catch (e: Exception) {
                e.message?.let {
                    logCrash(
                        "firestore: change the order cancel status in cloud",
                        it
                    )
                }
                false
            }
        }

    private suspend fun updateCloudProfileRecentPurchases(item: String?, docID: String): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                mFireStore.collection(Constants.USERS)
                    .document(docID).update("purchaseHistory", FieldValue.arrayRemove(item)).await()
                true
            } catch (e: Exception) {
                e.message?.let {
                    logCrash(
                        "firestore: removing the order id from profile in cloud",
                        it
                    )
                }
                false
            }
        }

    private suspend fun cancelOrderFromLocalDB(id: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            repository.orderCancelled(id, Constants.CANCELLED)
            repository.cancelActiveOrder(id)
            true
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: making changes in the local db", it) }
            false
        }
    }



    //favorites
    suspend fun addFavorites(id: String, item: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserID = if (id.isNullOrEmpty()) {
                getCurrentUserId()!!
            } else {
                id
            }
            //This function will add a new data if it is not present in the array
            mFireStore.collection(Constants.USERS)
                .document(currentUserID).update(Constants.FAVORITES, FieldValue.arrayUnion(item)).await()
            true
        }catch (e: IOException) {
            e.message?.let {
                logCrash("firestore: adding fav to store",
                    it
                )
            }
            false
        }

    }

    suspend fun removeFavorites(id: String, item: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserID = if (id.isNullOrEmpty()) {
                getCurrentUserId()!!
            } else {
                id
            }
            mFireStore.collection(Constants.USERS)
                .document(currentUserID).update(Constants.FAVORITES, FieldValue.arrayRemove(item)).await()
            true
        }catch (e: IOException) {
            e.message?.let {
                logCrash("firestore: removing fav to store",
                    it
                )
            }
            false
        }
    }

    //reviews
    suspend fun productReviewsListener(id: String, viewModel: ViewModel) = withContext(Dispatchers.IO) {
        try {
            mFireStore
                .collection("Reviews")
                .document("Products")
                .collection(id)
                .addSnapshotListener { snapshot, fireSnapshotFailure ->
                    //error handling
                    fireSnapshotFailure?.let { e ->
                        throw e
                    }
                    snapshot?.let {
                        val reviews = arrayListOf<Review>()
                        reviews.clear()
                        for (doc in it) {
                            val review = doc.toObject(Review::class.java)
                            review.id = doc.id
                            reviews.add(review)
                        }
                        when(viewModel) {
                            is SubscriptionProductViewModel -> viewModel.reviewListener(reviews)
                            is ProductViewModel -> viewModel.reviewListener(reviews)
                        }
                    }
                }
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: getting product reviews", it) }
        }
    }

    suspend fun addReview(id: String, review: Review): NetworkResult = withContext(Dispatchers.IO) {
        try {
            mFireStore.collection("Reviews")
                .document("Products")
                .collection(id)
                .document()
                .set(review, SetOptions.merge()).await()
            NetworkResult.Success("review", "Thanks for the review :)")
        }catch (e: Exception) {
            e.message?.let { logCrash("firestore: add product review", it) }
            NetworkResult.Failed("review", "Server Error! Please try again later")
        }
    }



    //address
    suspend fun addAddress(id: String, address: Address) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val currentUserID = if (id.isNullOrEmpty()) {
                getCurrentUserId()!!
            } else {
                id
            }
            mFireStore.collection(Constants.USERS)
                .document(currentUserID).update(Constants.ADDRESS, FieldValue.arrayUnion(address))
        } catch (e: Exception) {
            e.message?.let { logCrash("Firestore: adding address", it) }
        }
    }

    suspend fun updateAddress(id: String, address: ArrayList<Address>) {
        try {
            val currentUserID = if (id.isNullOrEmpty()) {
                getCurrentUserId()!!
            } else {
                id
            }
            mFireStore.collection(Constants.USERS)
                .document(currentUserID).update("address", address)
        } catch (e: Exception) {
            e.message?.let { logCrash("Firestore: updating address", it) }
        }
    }



    //cancelling dates in subscription
    suspend fun addCancellationDates(sub: SubscriptionEntity, date: Long): Boolean
    = withContext(Dispatchers.IO) {
        return@withContext try {
            val addSubCancelledDate = async { addSubCancelledDate(sub, date) }
            val addSubIDToCancelledDate = async { addSubIDToCancelledDate(sub.id, date) }

            addSubCancelledDate.await() &&
            addSubIDToCancelledDate.await()
        } catch (e: Exception) {
            e.message?.let { logCrash("Firestore: cancelling date for sub parent job", it) }
            false
        }
    }

    private suspend fun addSubIDToCancelledDate(id: String, date: Long): Boolean
            = withContext(Dispatchers.IO) {
        return@withContext try {
            val dateDocID = SimpleDateFormat("dd")
            val docID = "${TimeUtil().getMonth(date)}${TimeUtil().getYear(date)}"

            val path = mFireStore.collection(SUBSCRIPTION)
                .document(Constants.CANCELLED).collection(docID).document(dateDocID.format(date))

            if (!path.get().await().exists()) {
                val cancelledIDs: MutableMap<String, ArrayList<String>> = mutableMapOf()
                cancelledIDs["cancelledIDs"] = arrayListOf()
                path.set(cancelledIDs, SetOptions.merge()).await()
            }
            path.update("cancelledIDs", FieldValue.arrayUnion(id)).await()
            true
        }catch (e: Exception) {
            e.message?.let { logCrash("Firestore: adding sub ID to date list in firestore", it) }
            false
        }
    }

    private suspend fun addSubCancelledDate(sub: SubscriptionEntity, date: Long): Boolean
        = withContext(Dispatchers.IO) {
            return@withContext try {
                mFireStore.collection(SUBSCRIPTION).document(SUB_ACTIVE)
                    .collection(SUB).document(sub.id)
                    .update("cancelledDates", FieldValue.arrayUnion(date)).await()
                true
            } catch (e: Exception) {
                e.message?.let { logCrash("Firestore: adding cancel date to sub in firestore", it) }
                false
            }
    }



    //cancel Subscription
    suspend fun cancelSubscription(sub: SubscriptionEntity): NetworkResult = withContext(Dispatchers.IO) {
        try {
            val createCancellationSub = async { createCancellationSub(sub) }
            val removeActiveSubFromProfile = async { removeActiveSubFromProfile(sub) }
            val removeActiveSubFromLocalDB = async {
                try {
                    repository.cancelActiveSubscription(sub.id)
                    repository.upsertSubscription(sub)
                    true
                } catch (e: IOException) {
                    e.message?.let {
                        logCrash(
                            "firestore: updating the cancel status for sub in firestore",
                            it
                        )
                    }
                    false
                }
            }
            if (
                createCancellationSub.await() &&
                removeActiveSubFromProfile.await() &&
                removeActiveSubFromLocalDB.await()
            ) {
                NetworkResult.Success("cancelled", null)
            } else {
                NetworkResult.Failed("cancelled", null)
            }
        } catch (e: Exception) {
            e.message?.let {
                logCrash("firestore: cancelling subscription parent job",
                    it
                )
            }
            NetworkResult.Failed("cancelled", null)
        }
    }

    private suspend fun createCancellationSub(sub: SubscriptionEntity) =
        withContext(Dispatchers.IO) {
            try {
                mFireStore.collection(SUBSCRIPTION).document(SUB_ACTIVE)
                    .collection(SUB)
                    .document(sub.id)
                    .delete().await()

                mFireStore.collection(SUBSCRIPTION).document(SUB_ACTIVE)
                    .collection(UNSUB)
                    .document(sub.id)
                    .set(sub, SetOptions.merge()).await()

                true
            } catch (e: Exception) {
                e.message?.let {
                    logCrash(
                        "firestore: updating the cancel status for sub in firestore",
                        it
                    )
                }
                false
            }
        }

    private suspend fun removeActiveSubFromProfile(sub: SubscriptionEntity) =
        withContext(Dispatchers.IO) {
            try {
                mFireStore.collection(Constants.USERS)
                    .document(sub.customerID)
                    .update("subscriptions", FieldValue.arrayRemove(sub.id))
                    .await()
                true
            } catch (e: Exception) {
                e.message?.let {
                    logCrash(
                        "firestore: updating the cancel status for sub in profile in firestore",
                        it
                    )
                }
                false
            }
        }

    suspend fun generateSubscription(subscription: Subscription):NetworkResult = withContext(Dispatchers.IO) {
        val sub = subscription.toSubscriptionEntity()
        try {
            val updateLocal = async { updateLocalSubscription(sub) }
            val updateStore = async { updateStoreSubscription(sub) }
            val updateCloud = async { updateCloudProfileSubscription(sub) }
            val createSubInDB = async { try {
                repository.upsertSubscription(sub)
                true
            } catch (e: IOException) {
                e.message?.let { logCrash("firestore: uploading the generated sub to local Db", it) }
                false
            } }

            if (
                updateStore.await() &&
                updateLocal.await() &&
                updateCloud.await() &&
                createSubInDB.await()
            ) {
                if (subscription.subType != MONTHLY) {
                    for (date in subscription.cancelledDates) {
                        if (addSubIDToCancelledDate(sub.id, date)) {
                            continue
                        }
                    }
                }
                NetworkResult.Success("sub", "Subscription Created Successfully")
            } else {
                NetworkResult.Failed("sub", "Server Error! Failed to create Subscription. Try Later")
            }
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: Creating subscription parent job", it) }
            NetworkResult.Failed("sub", "Server Error! Failed to create Subscription. Try Later")
        }
    }

    private suspend fun updateLocalSubscription(sub: SubscriptionEntity): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            ActiveSubscriptions(sub.id).also {
                repository.upsertActiveSubscription(it)
            }
            val profile = repository.getProfileData()!!
            if (!profile.subscribedMonths.contains(sub.monthYear)) {
                profile.subscribedMonths.add(sub.monthYear)
            }
            repository.upsertProfile(profile)
            true
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: uploading subscription in local db", it) }
            false
        }
    }

    private suspend fun updateStoreSubscription(sub: SubscriptionEntity): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            mFireStore.collection(SUBSCRIPTION).document(SUB_ACTIVE)
                .collection(SUB)
                .document(sub.id)
                .set(sub, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: uploading subscription to store", it) }
            false
        }
    }

    private suspend fun updateCloudProfileSubscription(sub: SubscriptionEntity): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            mFireStore.collection(Constants.USERS)
                .document(sub.customerID)
                .update("subscribedMonths", FieldValue.arrayUnion(sub.monthYear)).await()
            mFireStore.collection(Constants.USERS)
                .document(sub.customerID).update("subscriptions", FieldValue.arrayUnion(sub.id))
                .await()
            true
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: uploading subscription numbers in store profile", it) }
            false
        }
    }



    //Renew subscription
    suspend fun renewSubscription(sub: SubscriptionEntity, updatedCancelledDates: ArrayList<Long>): NetworkResult =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val renewSubInFirestore = async { renewSubInFirestore(sub.id, sub.monthYear, sub.endDate) }
                val renewSubInLocalDB = async {
                    try {
                        repository.upsertSubscription(sub)
                        true
                    } catch (e: IOException) {
                        e.message?.let {
                            logCrash(
                                "firestore: renewing the subscription in Local db",
                                it
                            )
                        }
                        false
                    }
                }
                val updateCancellationDates = async {
                    try {
                        if (updatedCancelledDates.isNullOrEmpty()) {
                            true
                        } else {
                            for (date in updatedCancelledDates) {
                                addCancellationDates(sub, date)
                            }
                            true
                        }
                    } catch (e: Exception) {
                        false
                    }
                }

                if (
                    renewSubInFirestore.await() &&
                    renewSubInLocalDB.await() &&
                    updateCancellationDates.await()
                ) {
                    NetworkResult.Success("renew", null)
                } else {
                    NetworkResult.Failed("renew", null)
                }
            } catch (e: Exception) {
                e.message?.let { logCrash("firestore: renewing the subscription parent job", it) }
                NetworkResult.Failed("renew", null)
            }
        }

    private suspend fun renewSubInFirestore(id: String, monthYear: String, newDate: Long): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                mFireStore.collection(SUBSCRIPTION).document(SUB_ACTIVE)
                    .collection(SUB)
                    .document(id)
                    .update("endDate", newDate).await()
                true
            } catch (e: Exception) {
                e.message?.let { logCrash("firestore: renewing the subscription in firestore", it) }
                false
            }
        }



    //wallet
    suspend fun getWalletAmount(id: String): Float {
        val currentUserID = if (id.isNullOrEmpty()) {
            getCurrentUserId()!!
        } else {
            id
        }
        return mFireStore.collection(WALLET)
            .document(WALLET)
            .collection("Users")
            .document(currentUserID).get().await().toObject(Wallet::class.java)!!.amount
    }

    suspend fun getWallet(id: String): NetworkResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentUserID = if (id.isNullOrEmpty()) {
                getCurrentUserId()!!
            } else {
                id
            }
            val wallet = mFireStore.collection(WALLET)
                .document(WALLET)
                .collection("Users")
                .document(currentUserID).get().await().toObject(Wallet::class.java)!!
            NetworkResult.Success("wallet", wallet)
        } catch (e: Exception) {
            e.message?.let { logCrash("checkout: getting wallet", it) }
            NetworkResult.Failed("wallet", "Server Error. Failed to fetch Wallet.")
        }
    }

    //wallet transactions
    suspend fun getTransactions(id: String): List<TransactionHistory> =
        withContext(Dispatchers.IO) {
            try {
                val currentUserID = if (id.isNullOrEmpty()) {
                    getCurrentUserId()!!
                } else {
                    id
                }
                val transactions = mutableListOf<TransactionHistory>()
                val docs = mFireStore.collection(WALLET)
                    .document("Transaction")
                    .collection(currentUserID)
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

    suspend fun makeTransactionFromWallet(amount: Float, id: String, status: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentUserID = if (id.isNullOrEmpty()) {
                getCurrentUserId()!!
            } else {
                id
            }
                val path = mFireStore.collection(WALLET)
                    .document(WALLET)
                    .collection("Users")

                    mFireStore.runTransaction { transaction ->
                        val wallet = transaction.get(path.document(currentUserID)).toObject(Wallet::class.java)
                        if (status == "Add") {
                            wallet!!.amount = wallet.amount + amount
                            wallet.lastRecharge = System.currentTimeMillis()
                            transaction.update(
                                path.document(currentUserID),
                                "amount",
                                wallet.amount,
                                "lastRecharge",
                                wallet.lastRecharge
                            )
                            null
                        } else {
                            wallet!!.amount = wallet.amount - amount
                            wallet.lastTransaction = System.currentTimeMillis()
                            transaction.update(
                                path.document(currentUserID),
                                "amount",
                                wallet.amount,
                                "lastTransaction",
                                wallet.lastTransaction
                            )
                            null
                        }
                    }

                return@withContext true
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: Making transaction for purchase", it) }
            return@withContext false
        }
    }

    suspend fun updateTransaction(transaction: TransactionHistory): NetworkResult =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val path = mFireStore.collection(WALLET)
                    .document("Transaction")
                    .collection(transaction.fromID)
                transaction.id = path.document().id

                val makeTransactionEntry = async {
                    path.document(transaction.id).set(transaction, SetOptions.merge()).await()
                    true
                }
                val makeGlobalTransactionEntry = async {
                        val profile = mFireStore.collection(USERS).document(transaction.fromID).get().await().toObject(UserProfile::class.java)!!
                        GlobalTransaction(
                            id = "",
                            userID = transaction.fromID,
                            userName = profile.name,
                            userMobileNumber = profile.phNumber,
                            transactionID = transaction.id,
                            transactionType = WALLET,
                            transactionAmount = transaction.amount,
                            transactionDirection = "Added Money to User Wallet",
                            timestamp = transaction.timestamp,
                            transactionReason = ""
                        ).let {
                            if (transaction.purpose == ADD_MONEY) {
                                it.transactionReason = "This transaction may be Adding extra Money to Wallet or REFUND"
                            }
                            createGlobalTransactionEntry(it)
                        }
                     }
                if (
                    makeTransactionEntry.await() &&
                    makeGlobalTransactionEntry.await()
                ) {
                    NetworkResult.Success("transactionID", transaction.id)
                } else {
                    NetworkResult.Failed("transactionID", null)
                }
            } catch (e: Exception) {
                e.message?.let { logCrash("firestore: updating the wallet transaction", it) }
                NetworkResult.Failed("transactionID", null)
            }
        }

    suspend fun generateSubscriptionID(id: String): String =
        mFireStore.collection(SUBSCRIPTION)
            .document(SUB_ACTIVE)
            .collection(id)
            .document().id



    //notifications
    private suspend fun createNewNotification(notification: UserNotification): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            mFireStore.collection(USER_NOTIFICATIONS)
                .document(USER_NOTIFICATIONS)
                .collection(notification.userID)
                .document()
                .set(notification, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: deleteing user notification", it) }
                false
        }
    }

    suspend fun deleteNotification(notification: UserNotificationEntity): NetworkResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val deleteNotificationByIDInStore = async {
                mFireStore.collection(USER_NOTIFICATIONS)
                    .document(USER_NOTIFICATIONS)
                    .collection(notification.userID)
                    .document(notification.id)
                    .delete().await()
            }
            val deleteNotificationByIDInDB = async { repository.deleteNotificationsByID(notification.id) }
            deleteNotificationByIDInStore.await()
            deleteNotificationByIDInDB.await()
            NetworkResult.Success("delete", "Notification removed")
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: deleteing user notification", it) }
            NetworkResult.Failed("delete", "Server Error. Try again later")
        }
    }

    suspend fun clearAllNotifications(allNotifications:MutableList<UserNotificationEntity>): NetworkResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val deleteAllNotificationInStore = async { val path =
                mFireStore.collection(USER_NOTIFICATIONS)
                    .document(USER_NOTIFICATIONS)
                    .collection(allNotifications[0].userID)
                for (notification in allNotifications) {
                    path
                        .document(notification.id)
                        .delete()
                }
            }
            val deleteAllNotificationInDB = async { repository.deleteAllNotifications() }

            deleteAllNotificationInStore.await()
            deleteAllNotificationInDB.await()

            NetworkResult.Success("deleteAll", "Notifications cleared")
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: deleteing all user notification", it) }
            NetworkResult.Failed("deleteAll", "Server Error. Try again later")
        }
    }

    //Tokens
    suspend fun updateToken(tokenString: String): BirthdayCard? = withContext(Dispatchers.IO) {
        val profileDB = repository.getProfileData()
        profileDB?.let { profile ->
            Token(
                profile.id, tokenString, profile.name, profile.phNumber
            ).let { token ->
                try {
                    mFireStore.collection(TOKENS)
                        .document(token.id)
                        .set(token, SetOptions.merge()).await()

                    Log.e("qw", "${profile.id}", )

                    val doc = mFireStore.collection(USERS)
                        .document(profile.id)
                        .collection("birthdayCard")
                        .document("user").get().await()

                    Log.e("fs", "$doc", )

                    if (doc.exists()) {
                        return@withContext doc.toObject(BirthdayCard::class.java)
                    } else {
                        return@withContext null
                    }
                } catch (e: Exception) {
                    e.message?.let { logCrash("firestore: adding banner in DB", it) }
                    return@withContext null
                }
            }
        }
    }

    suspend fun getSupportToken(id: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            mFireStore.collection(TOKENS)
                .document(id)
                .get().await().toObject(Token::class.java)?.token ?: ""
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: getting customer Token", it) }
            ""
        }
    }

    suspend fun logCrash(location: String, message: String) {
        CrashLog(
            getCurrentUserId()!!,
            "${Build.MANUFACTURER} ${Build.MODEL} ${Build.VERSION.RELEASE} ${Build.VERSION_CODES::class.java.fields[Build.VERSION.SDK_INT].name}",
            TimeUtil().getCustomDate(dateLong = System.currentTimeMillis()),
            TimeUtil().getTimeInHMS(dateLong = System.currentTimeMillis()),
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
                Log.e("Magizhini", "logCrash: $it ")
            }
        }
    }

    suspend fun createGlobalTransactionEntry(globalTransaction: GlobalTransaction): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val docID = "${TimeUtil().getMonth(globalTransaction.timestamp)}${TimeUtil().getYear(globalTransaction.timestamp)}"
            val dateFormat = SimpleDateFormat("dd")
            val collectionID = "${dateFormat.format(globalTransaction.timestamp)}"
            mFireStore.collection("GlobalTransaction")
                .document(docID)
                .collection(collectionID)
                .document()
                .set(globalTransaction, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: adding a global transaction entry", it) }
            false
        }
    }


    suspend fun getAllCWMDishes(): NetworkResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val dishes = mutableListOf<CWMFood>()
            val allDishesDoc = mFireStore.collection(CWM)
                .document(ALL_DISHES)
                .collection(CWM)
                .get().await()

            for (doc in allDishesDoc.documents) {
                val dish = doc.toObject(CWMFood::class.java)
                dish?.let {
                    it.id = doc.id
                    dishes.add(dish)
                }
            }

            NetworkResult.Success("status", dishes)
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: getting all CWM Dishes", it) }
            NetworkResult.Failed("status", "Server Error! Try later")
        }
    }

    suspend fun getDishDetails(dishID: String): NetworkResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val dishDoc = mFireStore.collection(CWM)
                .document(ALL_DISHES)
                .collection(CWM)
                .document(dishID).get().await()

            if (dishDoc.exists()) {
                val dish = dishDoc.toObject(CWMFood::class.java)
                dish!!.id = dishDoc.id
                NetworkResult.Success("dish", dish)
            } else {
                NetworkResult.Failed("dish", "Dish Details not available")
            }

        } catch (e: Exception){
            e.message?.let { logCrash("firestore: getting the dish details from store", it) }
            NetworkResult.Failed("dish", "Server Error! Failed to get Details")
        }
    }

    suspend fun updateBirthdayCard(customerID: String) = withContext(Dispatchers.IO){
        try {
            mFireStore
                .collection(USERS)
                .document(customerID)
                .collection("birthdayCard")
                .document("user").delete()
        } catch (e: Exception) {
            e.message?.let { logCrash("firestore: deleting birthday card greeting", it) }
        }
    }
}