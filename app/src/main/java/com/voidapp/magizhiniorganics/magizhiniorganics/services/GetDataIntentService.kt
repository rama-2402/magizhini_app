package com.voidapp.magizhiniorganics.magizhiniorganics.servicesimport android.app.IntentServiceimport android.content.Contextimport android.content.Intentimport android.util.Logimport com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepositoryimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelperimport org.kodein.di.Kodeinimport org.kodein.di.KodeinAwareimport org.kodein.di.android.kodeinimport org.kodein.di.generic.instanceclass GetDataIntentService(): IntentService("GetData"), KodeinAware {    override val kodein: Kodein by kodein()    val firestoreRepository: FirestoreRepository by instance()    companion object {        private lateinit var instance: GetDataIntentService        var isRunning = false        fun stopService() {            isRunning = false            instance.stopSelf()        }    }    init {        instance = this    }    override fun onHandleIntent(intent: Intent?) {        try {            isRunning = true            if (isRunning) {                firestoreRepository.getProductsAndCouponsData()            }        } catch (e: Exception) {            Log.e("service", e.message.toString())        }    }}