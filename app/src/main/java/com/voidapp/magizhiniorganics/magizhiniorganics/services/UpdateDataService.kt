package com.voidapp.magizhiniorganics.magizhiniorganics.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.Favorites
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.PinCodesEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import kotlin.Exception

class UpdateDataService (
    context: Context,
    workerParameters: WorkerParameters
        ) : CoroutineWorker(context, workerParameters), KodeinAware {

    override val kodein: Kodein by kodein(context)

    val repository: DatabaseRepository by instance()

    private val mFireStore by lazy {
        FirebaseFirestore.getInstance()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {

            repository.deleteBanners()

            val categoryData = async { getAllData(Constants.CATEGORY) }
            val bannerData = async { getAllData(Constants.BANNER) }
            val productData = async { getAllData(Constants.PRODUCTS) }
            val getBestSellers = async { getBestSellers() }
            val getSpecialsOne = async { specialsOne() }
            val getSpecialsTwo = async { specialsTwo() }
            val getSpecialsThree = async { specialsThree() }
            val getSpecialsBanners = async { specialBanners() }
            val couponData = async { getAllData(Constants.COUPON) }
            val deliveryChargeData = async { getAllData(Constants.DELIVERY_CHARGE) }

            val categorySnapshot = categoryData.await()
            val bannerSnapshot = bannerData.await()
            val productSnapshot = productData.await()
            getBestSellers.await()
            getSpecialsOne.await()
            getSpecialsTwo.await()
            getSpecialsThree.await()
            getSpecialsBanners.await()
            val couponSnapshot = couponData.await()
            val deliveryChargeSnapshot = deliveryChargeData.await()

            val updateCategory =
                async { filterDataAndUpdateRoom(Constants.CATEGORY, categorySnapshot) }
            val updateBanner =
                async { filterDataAndUpdateRoom(Constants.BANNER, bannerSnapshot) }
            val updateProduct =
                async { filterDataAndUpdateRoom(Constants.PRODUCTS, productSnapshot) }
            val updateCoupon =
                async { filterDataAndUpdateRoom(Constants.COUPON, couponSnapshot) }
            val updateDeliveryCharge =
                async { filterDataAndUpdateRoom(Constants.DELIVERY_CHARGE, deliveryChargeSnapshot) }

            updateCategory.await()
            updateProduct.await()
            updateCoupon.await()
            updateDeliveryCharge.await()
            updateBanner.await()

            updateEntityData()

        }
        catch (e: Exception) {
            return@withContext Result.retry()
        }
        return@withContext Result.success()
    }
//
//    private suspend fun setProductListener() = withContext(Dispatchers.IO) {
//        try {
//            mFireStore.collection(Constants.PRODUCTS)
//                .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
//                .addSnapshotListener { snapshot, fireSnapshotFailure ->
//                    //error handling
//                    fireSnapshotFailure?.let {
//                        Log.e(Constants.APP_NAME, it.message.toString())
//                    }
//                    snapshot?.let { filterProducts(snapshot) }
//
//                }
//        } catch (e: Exception) {
//
//        }
//    }


    private suspend fun specialBanners() {
        try {
            val snapshot = mFireStore.collection("specialBanner")
                .get().await()
            for (doc in snapshot.documents) {
//            repository.deleteSpecialsOne()
                repository.upsertSpecialBanners(doc.toObject(SpecialBannersData::class.java)!!.toSpecialBanners())
            }
        } catch (e: Exception) {
            Log.e("TAG", "one:${e.message} ", )
        }
    }

    private suspend fun specialsOne() {
        try {
            val doc = mFireStore.collection("specialsOne")
                .document("One").get().await().toObject(ProductSpecials::class.java)!!.toSpecialsOne()
//            repository.deleteSpecialsOne()
            repository.upsertSpecialsOne(doc)
        } catch (e: Exception) {
            Log.e("TAG", "one:${e.message} ", )
        }
    }
    private suspend fun specialsTwo() {
        try {
            val doc = mFireStore.collection("specialsOne")
                .document("two").get().await().toObject(ProductSpecials::class.java)!!.toSpecialsTwo()
//            repository.deleteSpecialsTwo()
            repository.upsertSpecialsTwo(doc)
        } catch (e: Exception) {
            Log.e("TAG", "two:${e.message} ", )
        }
    }
    private suspend fun specialsThree() {
        try {
            val doc = mFireStore.collection("specialsOne")
                .document("three").get().await().toObject(ProductSpecials::class.java)!!.toSpecialsThree()
//            repository.deleteSpecialsThree()
            Log.e("TAG", "three: $doc", )
            repository.upsertSpecialsThree(doc)
        } catch (e: Exception) {
            Log.e("TAG", "three:${e.message} ", )
        }
    }

    private suspend fun getBestSellers() {
        try {
            val doc = mFireStore.collection("bestSellers")
                .document("2LVakx7dzw1zjKmPFy6m").get().await().toObject(ProductSpecials::class.java)!!.toBestSellers()
            Log.e("TAG", "bs: $doc", )
//            repository.deleteBestSellers()
            repository.upsertBestSellers(doc)
        } catch (e: Exception) {
            Log.e("TAG", "bserror:${e.message} ", )
        }
    }


    private suspend fun filterDataAndUpdateRoom(content: String, snapshot: QuerySnapshot) =
        withContext(Dispatchers.Default) {
            when (content) {
                Constants.CATEGORY -> {
                    Log.e("TAG", "getBestSellers: Cat", )
                    for (d in snapshot.documents) {
                        //getting the id of the category and converting to Category class
                        val category = d.toObject(ProductCategory::class.java)
                        category!!.id = d.id
                        //converting the category onject to CategoryEntity for updating the field in the room database
                        val categoryEntity = category.toProductCategoryEntity()
                        //updating the room - this add the new category and updates the existing ones
                        repository.upsertProductCategory(categoryEntity)
                    }
                }
                Constants.BANNER -> {
                    for (d in snapshot.documents) {
                        val banner = d.toObject(Banner::class.java)
                        banner!!.id = d.id
                        //creating a generic banner items array
                        val bannerEntity = banner.toBannerEntity()
                        repository.upsertBanner(bannerEntity)
                    }
                }
                Constants.PRODUCTS -> {
                    for (d in snapshot.documents) {
                        val product = d.toObject(Product::class.java)
                        product!!.id = d.id
                        val productEntity: ProductEntity = product.toProductEntity()
                        repository.upsertProduct(productEntity)
                    }
                }
                Constants.COUPON -> {
                    for (d in snapshot.documents) {
                        val coupon = d.toObject(Coupon::class.java)
                        coupon!!.id = d.id
                        val couponEntity = coupon.toCouponEntity()
                        repository.upsertCoupon(couponEntity)
                    }
                }
                Constants.DELIVERY_CHARGE -> {
                    for (d in snapshot.documents) {
                        val deliveryCode = d.toObject(PinCodes::class.java)
                        deliveryCode!!.id = d.id
                        val deliveryCodeEntity: PinCodesEntity = deliveryCode.toPinCodesEntity()
                        repository.upsertPinCodes(deliveryCodeEntity)
                    }
                }
            }
        }

    private suspend fun getAllData(content: String): QuerySnapshot = withContext(Dispatchers.IO) {
        when (content) {
            Constants.CATEGORY -> {
                mFireStore.collection(Constants.CATEGORY)
                    .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
                    .get().await()
            }
            Constants.PRODUCTS -> {
                mFireStore.collection(Constants.PRODUCTS)
                    .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
                    .get().await()
            }
            Constants.BANNER -> {
                mFireStore.collection(Constants.BANNER)
                    .orderBy(Constants.BANNER_ORDER, Query.Direction.ASCENDING)
                    .get().await()
            }
            Constants.COUPON -> {
                mFireStore.collection(Constants.COUPON)
                    .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
                    .whereEqualTo(Constants.STATUS, Constants.ACTIVE)
                    .get().await()
            }
            Constants.DELIVERY_CHARGE -> {
                mFireStore.collection("pincode")
                    .get().await()
            }
            else -> {
                mFireStore.collection(Constants.CATEGORY)
                    .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
                    .get().await()
            }
        }
    }

    //Function to update the products entity with user preferences like favorites, cart items and coupons added
    private fun updateEntityData() {
        try {
            val favorites: List<Favorites>? = repository.getFavorites()
            favorites?.forEach {
                repository.updateProductFavoriteStatus(it.id, status = true)
            }
            repository.getAllCartItemsForEntityUpdate().forEach {
                with(it) {
                    val product = repository.getProductWithIdForUpdate(productId)
                    product.variantInCart.add(it.variant)
                    repository.upsertProduct(product)
                    repository.updateCartItemsToEntity(productId, true, couponName)
                }
            }
        } catch (e: Exception) {
            Log.e("exception", e.message.toString())
        }
    }

}