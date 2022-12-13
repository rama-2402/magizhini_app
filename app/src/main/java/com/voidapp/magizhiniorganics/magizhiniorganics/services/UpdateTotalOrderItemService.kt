package com.voidapp.magizhiniorganics.magizhiniorganics.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TotalOrder
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Converters
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import kotlinx.coroutines.tasks.await

class UpdateTotalOrderItemService(
    context: Context,
    workerParameters: WorkerParameters
): CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val stringConvertedCart = inputData.getString("cart")
        val status = inputData.getBoolean(Constants.STATUS, true)
        val cartItems = Converters().stringToCartConverter(stringConvertedCart!!)
        val date = TimeUtil().getCurrentDateNumber()
        val docID = "${TimeUtil().getMonth()}${TimeUtil().getYear()}"

        val fireStore = FirebaseFirestore.getInstance()
        val path = fireStore.collection("totalOrders")
            .document(docID)
            .collection(date)

        try {
            for (cartItem in cartItems) {
                val id = "${cartItem.productId}${cartItem.variantIndex}"
                val doc = path.document(id).get().await()
                val item = doc.toObject(TotalOrder::class.java)
                if (item == null) {
                    val orderItem = TotalOrder (
                        id = id,
                        productID = cartItem.productId,
                        productName = cartItem.productName,
                        variant = cartItem.variant,
                        orderCount = cartItem.quantity
                    )
                    path
                        .document(id)
                        .set(orderItem, SetOptions.merge())
                } else {
                    fireStore.runTransaction { transaction ->
                        val transactionDoc = transaction.get(path.document(id)).toObject(TotalOrder::class.java)
                            if (status == true) {
                                transactionDoc!!.orderCount = transactionDoc.orderCount + cartItem.quantity
                            } else {
                                transactionDoc!!.orderCount = transactionDoc.orderCount - cartItem.quantity
                            }
                            transaction.update(path.document(id), "orderCount", transactionDoc.orderCount)
                            null
                        }
                    }
                }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}