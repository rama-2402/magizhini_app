package com.voidapp.magizhiniorganics.magizhiniorganics.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Cart
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TestimonialsEntity

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM ProductEntity WHERE id = :id")
    fun getID(id: String): ProductEntity

    @Query("DELETE FROM UserProfileEntity")
    fun deleteUserProfile()
    @Query("DELETE FROM ActiveOrders")
    fun deleteActiveOrdersTable()
    @Query("DELETE FROM ActiveSubscriptions")
    fun deleteActiveSubTable()
    @Query("DELETE FROM OrderEntity")
    fun deleteOrdersTable()
    @Query("DELETE FROM SubscriptionEntity")
    fun deleteSubscriptionsTable()
    @Query("DELETE FROM BannerEntity")
    fun deleteBanners()
    @Query("DELETE FROM CouponEntity")
    fun deleteCoupons()
    @Query("DELETE FROM ProductCategoryEntity")
    fun deleteAllCategories()
    @Query("DELETE FROM ProductEntity")
    fun deleteAllProducts()
    @Query("DELETE FROM ProductEntity WHERE id = :id")
    fun deleteProductByID(id: String)
    @Query("DELETE FROM ProductCategoryEntity WHERE id = :id")
    fun deleteCategoryByID(id: String)

    @Query("DELETE FROM CartEntity WHERE id = :id")
    fun deleteCartItem(id: Int)

    @Query("DELETE FROM CartEntity WHERE productId = :productId AND variant = :variantName")
    fun deleteProductFromCart(productId: String, variantName: String)

    @Query("DELETE FROM CartEntity")
    fun clearCart()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertProfile(userProfile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertCartItem(cartEntity: CartEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertCategory(category: ProductCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertCoupon(coupon: CouponEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertBanner(banner: BannerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertOrder(orderEntity: OrderEntity)

    @Query("UPDATE OrderEntity SET orderStatus = :status WHERE orderId = :id")
    fun orderCancelled(id: String, status: String)

    @Query("UPDATE CartEntity SET quantity = :count WHERE id = :id")
    fun updateCartItem(id: Int, count: Int)

    @Query("UPDATE CartEntity SET price = :price WHERE id = :id")
    fun updateCartItemPrice(id: Int, price: Float)

    @Query("SELECT * FROM UserProfileEntity")
    fun getProfileData(): UserProfileEntity?

    @Query("SELECT * FROM CartEntity WHERE variant = :id")
    fun getCartItem(id: Int): CartEntity

    @Query("SELECT * FROM CartEntity")
    fun getAllCartItems(): LiveData<List<CartEntity>>

    @Query("SELECT * FROM ProductEntity WHERE activated ORDER BY name")
    fun getAllProducts(): LiveData<List<ProductEntity>>

    @Query("SELECT * FROM ProductEntity WHERE activated ORDER BY name")
    fun getAllProductsStatic(): List<ProductEntity>

    @Query("SELECT * FROM ProductEntity")
    fun getAllProductsForCleaning(): List<ProductEntity>

    @Query("SELECT * FROM ProductCategoryEntity")
    fun getAllCategoryForCleaning(): List<ProductCategoryEntity>

    @Query("SELECT * FROM productEntity WHERE id = :id")
    fun getProductWithId(id: String): LiveData<ProductEntity>

    @Query("SELECT * FROM ProductCategoryEntity WHERE activated")
    fun getAllProductCategories(): LiveData<List<ProductCategoryEntity>>

    @Query("SELECT * FROM CouponEntity")
    fun getAllCoupons(): LiveData<List<CouponEntity>>

    @Query("SELECT * FROM BannerEntity")
    fun getAllBanners(): LiveData<List<BannerEntity>>

    @Query("SELECT * FROM ProductEntity WHERE category = :category AND activated")
    fun getAllProductsInCategory(category: String): LiveData<List<ProductEntity>>

    @Query("SELECT * FROM ProductEntity WHERE discountAvailable ORDER BY name")
    fun getAllDiscountProducts(): List<ProductEntity>

    @Query("SELECT * FROM CouponEntity WHERE status = :status")
    fun getAllActiveCoupons(status: String): List<CouponEntity>

    @Query("SELECT * FROM OrderEntity WHERE monthYear = :filter ")
    fun getOrderHistory(filter: String): List<OrderEntity>

    @Query("SELECT * FROM OrderEntity WHERE orderId = :id")
    fun getOrderByID(id: String): OrderEntity?

    //updating the entity based on the user preference
    @Query("UPDATE ProductEntity SET inCart = :status , appliedCoupon = :coupon WHERE id = :id")
    fun updateCartItemsToEntity(id: String, status: Boolean, coupon: String)

    @Query("SELECT * FROM CartEntity")
    fun getAllCartItemsForEntityUpdate() : List<CartEntity>

    @Query("SELECT * FROM productEntity WHERE id = :id")
    fun getProductWithIdForUpdate(id: String) : ProductEntity

    @Query("SELECT * FROM ProductEntity WHERE productType = :filter")
    fun getAllSubscriptions(filter: String): List<ProductEntity>

    @Query("UPDATE ProductEntity SET favorite = :status WHERE id = :id")
    fun updateProductFavoriteStatus(id: String, status: Boolean)

    @Query("SELECT * FROM ProductEntity WHERE category = :category AND activated ORDER BY name")
    fun getAllProductByCategoryStatic(category: String): List<ProductEntity>

    @Query("SELECT name FROM ProductCategoryEntity WHERE id = :id")
    fun getCategoryByID(id: String): String?

    @Query("SELECT * FROM ProductEntity WHERE favorite ORDER BY name")
    fun getAllFavoritesStatic(): List<ProductEntity>

    @Query("SELECT name FROM ProductCategoryEntity WHERE activated ORDER BY name")
    fun getAllCategoryNames(): List<String>

    @Query("SELECT SUM(price * quantity) FROM CartEntity")
    fun getCartPrice(): LiveData<Float>

    @Query("SELECT * FROM CouponEntity WHERE code = :code")
    fun getCouponByCode(code: String): CouponEntity?

    //pincodes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertPinCodes(PinCodes: PinCodesEntity)

    @Query("SELECT * FROM PinCodesEntity WHERE areaCode = :areaCode")
    fun getDeliveryCharge(areaCode: String): PinCodesEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertFavorite(id: Favorites)

    @Query("DELETE FROM Favorites WHERE id = :id")
    fun deleteFavorite(id: String)

    @Query("SELECT * FROM Favorites")
    fun getFavorites(): List<Favorites>?

    //subscription
    @Insert(onConflict =  OnConflictStrategy.REPLACE)
    fun upsertSubscription(subscriptionEntity: SubscriptionEntity)

    @Query("SELECT * FROM SubscriptionEntity WHERE status = :status")
    fun getAllSubscriptionsHistory(status: String): List<SubscriptionEntity>

    @Query("SELECT * FROM SubscriptionEntity WHERE id = :id")
    fun getSubscription(id: String): SubscriptionEntity

    @Query("UPDATE SubscriptionEntity SET endDate = :newDate WHERE id = :id")
    fun updateSubscription(id: String, newDate: Long)

    //Active orders and active subscriptions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertActiveOrders(id: ActiveOrders)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertActiveSubscription(id: ActiveSubscriptions)

    @Query("SELECT id FROM ActiveOrders")
    fun getAllActiveOrders(): LiveData<List<String>>

    @Query("SELECT id FROM ActiveSubscriptions")
    fun getAllActiveSubscriptions(): LiveData<List<String>>

    @Query("SELECT id FROM ActiveOrders")
    fun getAllActiveOrdersStatic(): List<String>

    @Query("SELECT id FROM ActiveSubscriptions")
    fun getAllActiveSubscriptionsStatic(): List<String>

    @Query("DELETE FROM ActiveOrders WHERE id = :id")
    fun cancelActiveOrder(id: String)

    @Query("DELETE FROM ActiveSubscriptions WHERE id = :id")
    fun cancelActiveSubscription(id: String)

    //specials
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertBestSellers(bestSeller: BestSellers)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSpecialsOne(bestSeller: SpecialsOne)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSpecialsTwo(bestSeller: SpecialsTwo)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSpecialsThree(bestSeller: SpecialsThree)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertSpecialBanners(banners: SpecialBanners)

    @Query("DELETE FROM BestSellers")
    fun deleteBestSellers()
    @Query("DELETE FROM SpecialsOne")
    fun deleteSpecialsOne()
    @Query("DELETE FROM SpecialsTwo")
    fun deleteSpecialsTwo()
    @Query("DELETE FROM SpecialsThree")
    fun deleteSpecialsThree()
    @Query("DELETE FROM SpecialBanners")
    fun deleteSpecialBanners()

    @Query("SELECT * FROM BestSellers")
    fun getBestSellers(): BestSellers
    @Query("SELECT * FROM SpecialsOne")
    fun getSpecialsOne(): SpecialsOne
    @Query("SELECT * FROM SpecialsTwo")
    fun getSpecialsTwo(): SpecialsTwo
    @Query("SELECT * FROM SpecialsThree")
    fun getSpecialsThree(): SpecialsThree
    @Query("SELECT * FROM SpecialBanners ORDER BY `order`")
    fun getSpecialBanners(): List<SpecialBanners>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertNotification(userNotificationEntity: UserNotificationEntity)
    @Query("DELETE FROM UserNotificationEntity")
    fun deleteAllNotifications()
    @Query("DELETE FROM UserNotificationEntity WHERE id = :id")
    fun deleteNotificationsByID(id: String)
    @Query("SELECT * FROM UserNotificationEntity")
    fun getAllNotifications(): List<UserNotificationEntity>?
    @Query("SELECT * FROM UserNotificationEntity where timestamp <= :date")
    fun getAllNotificationsBeforeDate(date: Int): List<UserNotificationEntity>?

    //Testimonials
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertTestimonial(testimonial: TestimonialsEntity)
    @Query("SELECT * FROM TestimonialsEntity ORDER BY `order`")
    fun getAllTestimonials(): List<TestimonialsEntity>
    @Query("DELETE FROM TestimonialsEntity")
    fun deleteAllTestimonials()
}