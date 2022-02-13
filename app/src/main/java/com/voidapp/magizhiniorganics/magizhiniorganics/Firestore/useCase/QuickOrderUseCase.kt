package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase

import android.net.Uri
import android.util.Log
import androidx.work.Operation
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.QuickOrder
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ORDER_ESTIMATE_PATH
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.QUICK_ORDER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.Exception

class QuickOrderUseCase {
    private val fireStore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val firebaseStorage by lazy {
        FirebaseStorage.getInstance().reference
    }

    private var quickOrder: QuickOrder? = null

    fun sendGetEstimateRequest (
        orderListUri: List<Uri>,
        orderListExtension: List<String>,
        userID: String,
        name: String,
        phoneNumber: String
        ): Flow<NetworkResult> = flow {

        emit(NetworkResult.Loading(""))

        val imageUrl = arrayListOf<String>()

        try {
            for (i in orderListExtension.indices) {
                emit(NetworkResult.Success("uploading", i+1))
                val reference : StorageReference = firebaseStorage.child(
                    "${ORDER_ESTIMATE_PATH}${userID}/Page${i + 1}.${orderListExtension[i]}"
                )

                val url = reference.putFile(orderListUri[i])
                    .await().task.snapshot.metadata!!.reference!!.downloadUrl.await()

                imageUrl.add(url.toString())
            }

            quickOrder = QuickOrder(
                customerID = userID,
                customerName = name,
                phoneNumber = phoneNumber,
                mailID = "",
                timeStamp = System.currentTimeMillis(),
                cart = arrayListOf(),
                imageUrl = imageUrl,
                note = ""
            )

            quickOrder?.let {
                fireStore
                    .collection(QUICK_ORDER)
                    .document(it.customerID)
                    .set(it, SetOptions.merge())
                    .await()

                emit(NetworkResult.Success("complete", imageUrl))
            }

            quickOrder = null
        } catch (e: Exception) {
            emit(NetworkResult.Failed("complete", e.message.toString()))
        }
    }

    suspend fun checkForPreviousEstimate(userID: String): NetworkResult = withContext(Dispatchers.IO) {
        return@withContext try {

                val quickOrderDoc = fireStore
                    .collection(QUICK_ORDER)
                    .document(userID)
                    .get().await()

                if (quickOrderDoc.exists()) {
                    quickOrder = quickOrderDoc.toObject(QuickOrder::class.java)
                    Log.e("qw", "est", )
                    NetworkResult.Success("estimate", quickOrder)
                } else {
                    NetworkResult.Success("empty", null)
                }
            } catch (e: Exception) {
                NetworkResult.Failed("estimate", e.message.toString())
            }
        }

}