package com.voidapp.magizhiniorganics.magizhiniorganics.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.INT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class CleanDatabaseService(
    val context: Context,
    workerParameters: WorkerParameters
): CoroutineWorker(context, workerParameters), KodeinAware {

    override val kodein: Kodein by kodein(context)
    private val repository: DatabaseRepository by instance()

    data class Trash(
        var id: ArrayList<String> = arrayListOf(),
    )

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                val cleanProductsDB = async { cleanProductsDB() }
                val cleanCategoryDB = async { cleanCategoryDB() }

                return@withContext if (cleanCategoryDB.await() && cleanProductsDB.await()) {
                    SharedPref(context = context).putData("month", INT, TimeUtil().getMonthNumber())
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun cleanProductsDB(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val products = repository.getAllProductsForCleaning()
            for (product in products) {
                if (!product.activated) {
                    repository.deleteProductByID(product.id)
                }
            }
            true
        } catch (e: Exception) {
            return@withContext false
        }
    }
    private suspend fun cleanCategoryDB(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val categories = repository.getAllCategoryForCleaning()
            for (category in categories) {
                if (!category.activated) {
                    repository.deleteCategoryByID(category.id)
                }
            }
            true
        } catch (e: Exception) {
            return@withContext false
        }
    }
}