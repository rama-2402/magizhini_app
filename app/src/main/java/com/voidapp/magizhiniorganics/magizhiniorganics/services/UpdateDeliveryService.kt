package com.voidapp.magizhiniorganics.magizhiniorganics.servicesimport android.app.IntentServiceimport android.content.Contextimport android.content.Intentimport android.os.Buildimport android.util.Logimport androidx.work.CoroutineWorkerimport androidx.work.WorkerParametersimport com.google.firebase.firestore.FieldValueimport com.google.firebase.firestore.FirebaseFirestoreimport com.google.firebase.firestore.SetOptionsimport com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntityimport com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntityimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CrashLogimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Orderimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Subscriptionimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.*import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTIONimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUB_ACTIVEimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUB_CANCELLEDimport kotlinx.coroutines.Dispatchersimport kotlinx.coroutines.asyncimport kotlinx.coroutines.tasks.awaitimport kotlinx.coroutines.withContextimport org.kodein.di.Constantimport org.kodein.di.Kodeinimport org.kodein.di.KodeinAwareimport org.kodein.di.android.kodeinimport org.kodein.di.generic.instanceimport java.io.IOExceptionimport java.util.concurrent.Flowclass UpdateDeliveryService (    context: Context,    workerParameters: WorkerParameters): CoroutineWorker(context, workerParameters), KodeinAware {    override val kodein: Kodein by kodein(context)    private val repository: DatabaseRepository by instance()    private val fbRepository: FirestoreRepository by instance()    private val userID = SharedPref(context).getData(Constants.USER_ID, Constants.STRING, "").toString()    private val firestore by lazy {        FirebaseFirestore.getInstance()    }    companion object {        private const val TAG: String = "qqqq"    }    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {        try {            val activeSubscriptions = repository.getAllActiveSubscriptionsStatic() as ArrayList            val activeOrders = repository.getAllActiveOrdersStatic() as ArrayList            val subscriptions = async { subscriptions(activeSubscriptions) }            val orders = async { orders(activeOrders) }            subscriptions.await()            orders.await()            return@withContext Result.success()        } catch (e: Exception) {            return@withContext Result.retry()        }    }    private suspend fun subscriptions(subs: ArrayList<String>) = withContext(Dispatchers.IO) {        try {            val path = firestore.collection(SUBSCRIPTION).document(SUB_ACTIVE)                .collection(SUB)            if (subs.isNotEmpty()) {                for (sub in subs) {                    val gettingSubJob = async { repository.getSubscription(sub) }                    val localSub = gettingSubJob.await()                    if(localSub.endDate <= System.currentTimeMillis()) {                        localSub.status = SUB_CANCELLED                        repository.upsertSubscription(localSub)                        repository.cancelActiveSubscription(localSub.id)                    } else {                        val doc = path                            .document(localSub.id)                            .get()                            .await().toObject(Subscription::class.java)!!.toSubscriptionEntity()                       repository.upsertSubscription(doc)                    }                }            } else {                return@withContext            }        } catch (e: Exception) {            e.message?.let {                logCrash(userID, "updateDeliveryService: getting all active subscriptions",                    it                )            }        }    }    private suspend fun orders(orders: ArrayList<String>) = withContext(Dispatchers.IO) {        try {            val path = firestore.collection(Constants.ORDER_HISTORY)            if (orders.isNotEmpty()) {                for (i in 0 until orders.size) {                    val order = orders[i]                    val openOrder = repository.getOrderByID(order)!!                    val doc = path.document(openOrder.monthYear)                        .collection("Active")                        .document(openOrder.orderId)                        .get().await().toObject(Order::class.java)!!.toOrderEntity()                    if (                        doc.orderStatus == Constants.SUCCESS ||                        doc.orderStatus == Constants.FAILED                    ) {                        val updateLocalProfile = async { updateLocalProfile(doc) }                        val updateCloudProfile = async { updateCloudProfile(doc) }                        updateLocalProfile.await()                        updateCloudProfile.await()                        repository.upsertOrder(doc)                    } else {                        repository.upsertOrder(doc)                    }                }            } else {                Log.e(TAG, "orders: returned", )                return@withContext            }        } catch (e: Exception) {            Log.e(TAG, "orders: crash", )            e.message?.let {                logCrash(userID, "updateDeliveryService: getting all active orders",                    it                )            }        }    }    private suspend fun updateLocalProfile(order: OrderEntity) = withContext(Dispatchers.IO) {        try {            repository.cancelActiveOrder(order.orderId)        } catch (e: IOException) {            e.message?.let {                logCrash(userID, "updateDeliveryService: updating the success order in db",                    it                )            }        }        Log.e(TAG, "orders: localdb", )    }    private suspend fun updateCloudProfile(order: OrderEntity) = withContext(Dispatchers.IO) {        try {            firestore.collection(Constants.USERS)                .document(order.customerId).update("purchaseHistory", FieldValue.arrayRemove(order.orderId))        } catch (e: IOException) {            e.message?.let {                logCrash(userID, "updateDeliveryService: updating the success order in db",                    it                )            }        }        Log.e(TAG, "orders: cloud", )    }    private suspend fun logCrash(userID: String, location: String, message: String) {        CrashLog(            userID,            "${ Build.MANUFACTURER } ${ Build.MODEL } ${Build.VERSION.RELEASE} ${ Build.VERSION_CODES::class.java.fields[Build.VERSION.SDK_INT].name }",            TimeUtil().getCustomDate("",System.currentTimeMillis()),            TimeUtil().getTimeInHMS(dateLong = System.currentTimeMillis()),            location,            message        ).let {            try {                FirebaseFirestore.getInstance()                    .collection("crashLog")                    .document(userID)                    .collection("MagizhiniApp")                    .document()                    .set(it, SetOptions.merge()).await()            } catch (e: Exception) {                Log.e("Magizhini", "logCrash: $it ", )            }        }    }}