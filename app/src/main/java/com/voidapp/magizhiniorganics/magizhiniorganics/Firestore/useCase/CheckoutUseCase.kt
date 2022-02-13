package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class CheckoutUseCase(
    private val fbRepository: FirestoreRepository
) {
    /*
    * userid, transaction id,
    * */
    suspend fun placeCodOrder(orderMap: HashMap<String, Any>, cart: CartEntity): Flow<NetworkResult> = withContext(Dispatchers.IO) {
        flow {
            Order(

            ).let { order ->
                val orderStatus = fbRepository.placeOrder(order = order)

            }
        }
    }

    suspend fun placeWalletOrder(orderMap: HashMap<String, Any>): Flow<NetworkResult> = withContext(Dispatchers.IO) {
        flow {

        }
    }

    suspend fun placeOnlineOrder(orderMap: HashMap<String, Any>): Flow<NetworkResult> = withContext(Dispatchers.IO) {
        flow {

        }
    }
}