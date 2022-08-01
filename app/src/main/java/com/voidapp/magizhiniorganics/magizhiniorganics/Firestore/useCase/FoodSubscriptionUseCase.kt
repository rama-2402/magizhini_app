package com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase

import com.google.firebase.firestore.FirebaseFirestore
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.AmmaSpecial
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Banner
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ALL_DISHES
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.AMMASPECIAL
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BANNER
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FoodSubscriptionUseCase(
    private val fbRepository: FirestoreRepository
) {
    private val fireStore by lazy {
        FirebaseFirestore.getInstance()
    }

    suspend fun getAllBanners(): List<Banner>? = withContext(Dispatchers.IO) {
        val banners = mutableListOf<Banner>()
        return@withContext try {
            fireStore.collection(AMMASPECIAL).document(AMMASPECIAL).collection(BANNER).get().await().let { snap ->
                snap.documents.forEach {
                    banners.add(it.toObject(Banner::class.java)!!)
                }
            }
            banners
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllAmmaSpecials(): List<AmmaSpecial>? = withContext(Dispatchers.IO) {
        val specials = mutableListOf<AmmaSpecial>()
        return@withContext try {
            fireStore.collection(AMMASPECIAL).document(AMMASPECIAL).collection(ALL_DISHES).get().await().let { snap ->
                snap.documents.forEach {
                    specials.add(it.toObject(AmmaSpecial::class.java)!!)
                }
            }
            specials
        } catch (e: Exception) {
            null
        }
    }

}