package com.voidapp.magizhiniorganics.magizhiniorganics.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TotalOrder
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time

class UpdateTotalOrderItemService(
    context: Context,
    workerParameters: WorkerParameters
): CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val stringConvertedOrder = inputData.getString("order")
        val cartItems = toOrderConverter(stringConvertedOrder!!).cart
        val date = Time().getCurrentDateNumber()
        val docID = "${Time().getMonth()}${Time().getYear()}"

        val fireStore = FirebaseFirestore.getInstance()
        val path = fireStore.collection("totalOrders")
            .document(docID)
            .collection(date)

        try {
            for (cartItem in cartItems) {
                val id = "${cartItem.productId}${cartItem.variantIndex}"
                fireStore.runTransaction { transaction ->
                    val item = transaction.get(path.document(id)).toObject(TotalOrder::class.java)
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
                        item.orderCount = item.orderCount + cartItem.quantity
                        transaction.update(path.document(id), "orderCount", item.orderCount)
                        null
                    }
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun toOrderConverter(value: String): Order {
        val listType = object : TypeToken<Order>() {}.type
        return Gson().fromJson(value, listType)
    }
}