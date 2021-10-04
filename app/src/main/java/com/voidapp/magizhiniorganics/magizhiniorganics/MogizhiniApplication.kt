package com.voidapp.magizhiniorganics.magizhiniorganics

import android.app.Application
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.Firebase
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirebaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.Firestore
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.UserDatabase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation.ConversationViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModelFactory
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletViewModelFactory
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

class MagizhiniApplication: Application(), KodeinAware {

    override val kodein: Kodein = Kodein.lazy {
        import(androidXModule(this@MagizhiniApplication))

        bind() from singleton { UserDatabase(instance()) }
        bind() from singleton { DatabaseRepository(instance()) }
        bind() from singleton { Firestore(instance()) }
        bind() from singleton { FirestoreRepository(instance()) }
        bind() from singleton { Firebase(instance()) }
        bind() from singleton { FirebaseRepository(instance()) }

        //viewModels
        bind() from provider { HomeViewModelFactory(instance(), instance()) }
        bind() from provider { ShoppingMainViewModelFactory(instance(), instance()) }
        bind() from provider { ProductViewModelFactory(instance(), instance()) }
        bind() from provider { CheckoutViewModelFactory(instance(), instance()) }
        bind() from provider { PurchaseHistoryViewModelFactory(instance(), instance()) }
        bind() from provider { ChatViewModelFactory(instance(), instance(), instance()) }
        bind() from provider { ConversationViewModelFactory(instance(), instance(), instance()) }
        bind() from provider { WalletViewModelFactory(instance(), instance()) }
        bind() from provider { SubscriptionProductViewModelFactory(instance(), instance()) }
    }
}