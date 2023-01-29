package com.voidapp.magizhiniorganics.magizhiniorganics.services

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.Favorites
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.PinCodesEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BEST_SELLERS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCT_SPECIALS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.QUARTER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SPECIALS_ONE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SPECIALS_THREE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SPECIALS_TWO
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.TESTIMONIALS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class UpdateDataService (
     val context: Context,
    workerParameters: WorkerParameters
        ) : CoroutineWorker(context, workerParameters), KodeinAware {

    override val kodein: Kodein by kodein(context)

    private val repository: DatabaseRepository by instance()
    private val fbRepository: FirestoreRepository by instance()

    private val mFireStore by lazy {
        FirebaseFirestore.getInstance()
    }

    private val wipe = inputData.getString("wipe")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (wipe == "wipe") {
            repository.deleteAllProducts()
            repository.deleteAllCategories()
        }

        try {
            val categoryData = async { getAllData(Constants.CATEGORY) }
            val bannerData = async { getAllData(Constants.BANNER) }
            val productData = async { getAllData(Constants.PRODUCTS) }
            val getBestSellers = async { getBestSellers() }
            val getSpecialsOne = async { specialsOne() }
            val getSpecialsTwo = async { specialsTwo() }
            val getSpecialsThree = async { specialsThree() }
            val getSpecialsBanners = async { specialBanners() }
//            val getAllNotifications = async { getAllData(USER_NOTIFICATIONS) }
            val couponData = async { getAllData(Constants.COUPON) }
            val deliveryChargeData = async { getAllData(Constants.DELIVERY_CHARGE) }
            val testimonialsData = async { getAllData(TESTIMONIALS) }

            val categorySnapshot = categoryData.await()
            val bannerSnapshot = bannerData.await()
            val productSnapshot = productData.await()
            val testimonialsSnapshot = testimonialsData.await()
            getBestSellers.await()
            getSpecialsOne.await()
            getSpecialsTwo.await()
            getSpecialsThree.await()
            getSpecialsBanners.await()
//            val notificationSnapshot = getAllNotifications.await()
            val couponSnapshot = couponData.await()
            val deliveryChargeSnapshot = deliveryChargeData.await()

            val updateCategory =
                async { filterDataAndUpdateRoom(Constants.CATEGORY, categorySnapshot) }
            val updateBanner =
                async { filterDataAndUpdateRoom(Constants.BANNER, bannerSnapshot) }
            val updateProduct =
                async { filterDataAndUpdateRoom(Constants.PRODUCTS, productSnapshot) }
//            val updateNotifications =
//                async { filterDataAndUpdateRoom(Constants.USER_NOTIFICATIONS, notificationSnapshot) }
            val updateCoupon =
                async { filterDataAndUpdateRoom(Constants.COUPON, couponSnapshot) }
            val updateDeliveryCharge =
                async { filterDataAndUpdateRoom(Constants.DELIVERY_CHARGE, deliveryChargeSnapshot) }
            val updateTestimonials =
                async { filterDataAndUpdateRoom(Constants.TESTIMONIALS, testimonialsSnapshot) }

            updateCategory.await()
            updateProduct.await()
            updateBanner.await()
//            updateNotifications.await()
            updateTestimonials.await()
            updateCoupon.await()
            updateDeliveryCharge.await()

            updateEntityData()

        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("update data service parent job", it) }
            return@withContext Result.failure()
        }
        SharedPref(context).putData(QUARTER, STRING, TimeUtil().getQuarterOfTheDay().toString())
        return@withContext Result.success()
    }

    private suspend fun specialBanners() {
        try {
            val snapshot = mFireStore.collection(Constants.SPECIAL_BANNER)
                .get().await()
            for (doc in snapshot.documents) {
                repository.upsertSpecialBanners(
                    doc.toObject(Banner::class.java)!!.toSpecialBanners()
                )
            }
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("update data service: get specials banners", it) }
        }
    }

    private suspend fun specialsOne() {
        try {
            val doc = mFireStore.collection(PRODUCT_SPECIALS)
                .document(SPECIALS_ONE).get().await().toObject(ProductSpecials::class.java)!!
                .toSpecialsOne()
            repository.deleteSpecialsOne()
            repository.upsertSpecialsOne(doc)
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("update data service: get specials One", it) }
        }
    }
    private suspend fun specialsTwo() {
        try {
            val doc = mFireStore.collection(PRODUCT_SPECIALS)
            .document(SPECIALS_TWO).get().await().toObject(ProductSpecials::class.java)!!
            .toSpecialsTwo()
            repository.deleteSpecialsTwo()
            repository.upsertSpecialsTwo(doc)
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("update data service: get specials two", it) }
        }
    }
    private suspend fun specialsThree() {
        try {
            val doc = mFireStore.collection(PRODUCT_SPECIALS)
                .document(SPECIALS_THREE).get().await().toObject(ProductSpecials::class.java)!!
                .toSpecialsThree()
            repository.deleteSpecialsThree()
            repository.upsertSpecialsThree(doc)
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("update data service: get specials three", it) }
        }
    }

    private suspend fun getBestSellers() {
        try {
            val doc = mFireStore.collection(BEST_SELLERS)
                .document(BEST_SELLERS).get().await()
                .toObject(ProductSpecials::class.java)!!.toBestSellers()
            repository.deleteBestSellers()
            repository.upsertBestSellers(doc)
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("update data service: get best sellers", it) }
        }
    }


    private suspend fun filterDataAndUpdateRoom(content: String, snapshot: QuerySnapshot) =
        withContext(Dispatchers.Default) {
            try {
                when (content) {
                    Constants.CATEGORY -> {
//                    repository.deleteAllCategories()
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
                        repository.deleteBanners()
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
                            product?.let {
//                                product.id = d.id
                                val productEntity: ProductEntity = it.toProductEntity()
//                                Log.e("tag", "filterDataAndUpdateRoom: ${productEntity.id}--${productEntity.name}")
                                repository.upsertProduct(productEntity)
                            }
                        }
                    }
//                    USER_NOTIFICATIONS -> {
//                        repository.deleteAllNotifications()
//                        for (d in snapshot.documents) {
//                            val notification = d.toObject(UserNotification::class.java)
//                            notification!!.id = d.id
//                            val notificationEntity: UserNotificationEntity = notification.toUserNotificationEntity()
//                            repository.upsertNotification(notificationEntity)
//                        }
//                    }
                    Constants.COUPON -> {
                        repository.deleteCoupons()
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
                    Constants.TESTIMONIALS -> {
                        repository.deleteAllTestimonials()
                        for (d in snapshot.documents) {
                            val testimonial = d.toObject(Testimonials::class.java)
                            testimonial?.let {
                                val testimonialsEntity: TestimonialsEntity = testimonial.toTestimonialEntity()
                                repository.upsertTestimonial(testimonialsEntity)
                            }
                        }
                    }
                    else -> Unit
                }
            } catch (e: Exception) {
                e.message?.let { fbRepository.logCrash("update data service: updating the DB with data", it) }
            }
        }

    private suspend fun getAllData(content: String): QuerySnapshot = withContext(Dispatchers.IO) {
       return@withContext try {
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
//                USER_NOTIFICATIONS -> {
//                    mFireStore.collection(USER_NOTIFICATIONS)
//                        .document(USER_NOTIFICATIONS)
//                        .collection(userID)
//                        .whereLessThanOrEqualTo("timestamp", TimeUtil().getCurrentYearMonthDate()).get().await()
//                }
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
                TESTIMONIALS -> {
                    mFireStore.collection(TESTIMONIALS)
                        .get().await()
                }
                else -> {
                    mFireStore.collection(Constants.CATEGORY)
                        .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
                        .get().await()
                }
            }
        } catch (e: Exception) {
           Log.e("TAG", "getAllDataError: ${e.message}", )
           e.message?.let { fbRepository.logCrash("update data service: getting query snapshot from store", it) }
           mFireStore.collection(Constants.CATEGORY)
               .orderBy(Constants.PROFILE_NAME, Query.Direction.ASCENDING)
               .get().await()
       }
    }

    //Function to update the products entity with user preferences like favorites, cart items and coupons added
    private suspend fun updateEntityData() = withContext(Dispatchers.IO) {
        try {
            val favorites: List<Favorites>? = repository.getFavorites()
            favorites?.forEach {
                repository.updateProductFavoriteStatus(it.id, status = true)
            }
            repository.getAllCartItemsForEntityUpdate().forEach {
                with(it) {
                    val entity = repository.getProductWithIdForUpdate(productId)
                    entity?.let { product ->
                        product.variantInCart.add(it.variant)
                        repository.upsertProduct(product)
                        repository.updateCartItemsToEntity(productId, true, couponName)
                    }
                }
            }
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("update data service: updating the product favorites and in cart", it) }
        }
    }

}