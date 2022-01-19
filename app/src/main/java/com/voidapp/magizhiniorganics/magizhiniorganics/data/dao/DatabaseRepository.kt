package com.voidapp.magizhiniorganics.magizhiniorganics.data.dao

import androidx.lifecycle.LiveData
import com.voidapp.magizhiniorganics.magizhiniorganics.data.UserDatabase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TestimonialsEntity

class DatabaseRepository(
    private val db: UserDatabase
) {

    fun getid(id: String) = db.getUserProfileDao().getID(id)

    fun deleteUserProfile() = db.getUserProfileDao().deleteUserProfile()
    fun deleteActiveOrdersTable() = db.getUserProfileDao().deleteActiveOrdersTable()
    fun deleteActiveSubTable() = db.getUserProfileDao().deleteActiveSubTable()
    fun deleteOrdersTable() = db.getUserProfileDao().deleteOrdersTable()
    fun deleteSubscriptionsTable() = db.getUserProfileDao().deleteSubscriptionsTable()
    fun deleteBanners() = db.getUserProfileDao().deleteBanners()
    fun deleteCoupons() = db.getUserProfileDao().deleteCoupons()
    fun deleteAllCategories() = db.getUserProfileDao().deleteAllCategories()
    fun deleteAllProducts() = db.getUserProfileDao().deleteAllProducts()
    fun deleteProductByID(id: String) = db.getUserProfileDao().deleteProductByID(id)
    fun deleteCategoryByID(id: String) = db.getUserProfileDao().deleteCategoryByID(id)
    fun getAllProductsForCleaning(): List<ProductEntity> = db.getUserProfileDao().getAllProductsForCleaning()
    fun getAllCategoryForCleaning(): List<ProductCategoryEntity> = db.getUserProfileDao().getAllCategoryForCleaning()

    fun deleteCartItem(id: Int) = db.getUserProfileDao().deleteCartItem(id)

    fun deleteProductFromCart(productId: String, variantName: String) = db.getUserProfileDao().deleteProductFromCart(productId, variantName)

    fun clearCart() = db.getUserProfileDao().clearCart()

    fun upsertProfile(profile: UserProfileEntity) = db.getUserProfileDao().upsertProfile(profile)

    fun upsertCart(cartEntity: CartEntity) = db.getUserProfileDao().upsertCartItem(cartEntity)

    fun upsertProduct(product: ProductEntity) = db.getUserProfileDao().upsertProduct(product)

    fun upsertProductCategory(category: ProductCategoryEntity) = db.getUserProfileDao().upsertCategory(category)

    fun getCategoryByID(id: String): String? = db.getUserProfileDao().getCategoryByID(id)

    fun upsertCoupon(coupon: CouponEntity) = db.getUserProfileDao().upsertCoupon(coupon)

    fun upsertBanner(bannerEntity: BannerEntity) = db.getUserProfileDao().upsertBanner(bannerEntity)

    fun upsertOrder(orderEntity: OrderEntity) = db.getUserProfileDao().upsertOrder(orderEntity)

    fun orderCancelled(id: String, status: String) = db.getUserProfileDao().orderCancelled(id, status)

    fun updateCartItem(id: Int, count: Int) = db.getUserProfileDao().updateCartItem(id, count)

    fun updateCartItemPrice(id: Int, price: Float) = db.getUserProfileDao().updateCartItemPrice(id, price)

    fun getProfileData() = db.getUserProfileDao().getProfileData()

    fun getAllCartItems() = db.getUserProfileDao().getAllCartItems()

    fun getCartItem(id: Int) = db.getUserProfileDao().getCartItem(id)

    fun getAllproducts() = db.getUserProfileDao().getAllProducts()

    fun getProductWithId(id: String) = db.getUserProfileDao().getProductWithId(id)

    fun getAllSubscriptions(filter: String) = db.getUserProfileDao().getAllSubscriptions(filter)

    fun getAllProductCategories() = db.getUserProfileDao().getAllProductCategories()

    fun getAllCoupons() = db.getUserProfileDao().getAllCoupons()

    fun getAllBanners() = db.getUserProfileDao().getAllBanners()

    fun getAllProductsInCategory(category: String) = db.getUserProfileDao().getAllProductsInCategory(category)

    fun getAllDiscountProducts() = db.getUserProfileDao().getAllDiscountProducts()

    fun getAllActiveCoupons(status: String) = db.getUserProfileDao().getAllActiveCoupons(status)

    fun getOrderHistory(filter: String) = db.getUserProfileDao().getOrderHistory(filter)

    fun getOrderByID(id: String) = db.getUserProfileDao().getOrderByID(id)

    //updating the product entity with the user prefence favorites and cart items
    fun updateProductFavoriteStatus(id: String, status: Boolean) = db.getUserProfileDao().updateProductFavoriteStatus(id, status)

    fun updateCartItemsToEntity(id: String, status: Boolean, coupon: String) = db.getUserProfileDao().updateCartItemsToEntity(id, status, coupon)

    fun getAllCartItemsForEntityUpdate() = db.getUserProfileDao().getAllCartItemsForEntityUpdate()

    fun getProductWithIdForUpdate(id: String): ProductEntity? = db.getUserProfileDao().getProductWithIdForUpdate(id)

    fun getAllProductsStatic() = db.getUserProfileDao().getAllProductsStatic()

    fun getAllProductByCategoryStatic(category: String) = db.getUserProfileDao().getAllProductByCategoryStatic(category)

    fun getAllFavoritesStatic() = db.getUserProfileDao().getAllFavoritesStatic()

    fun upsertFavorite(id: Favorites) = db.getUserProfileDao().upsertFavorite(id)

    fun deleteFavorite(id: String) = db.getUserProfileDao().deleteFavorite(id)

    fun getFavorites(): List<Favorites>? = db.getUserProfileDao().getFavorites()

    fun deleteAllFavorites() = db.getUserProfileDao().deleteAllFavorites()

    fun getAllCategoryNames() = db.getUserProfileDao().getAllCategoryNames()

    fun getCartPrice() = db.getUserProfileDao().getCartPrice()

    fun getCouponByCode(code: String) = db.getUserProfileDao().getCouponByCode(code)

    //pincodes
    fun upsertPinCodes(pinCodes: PinCodesEntity) = db.getUserProfileDao().upsertPinCodes(pinCodes)

    fun getDeliveryCharge(areaCode: String) = db.getUserProfileDao().getDeliveryCharge(areaCode)

    //subscription
    fun upsertSubscription(subscriptionEntity: SubscriptionEntity) = db.getUserProfileDao().upsertSubscription(subscriptionEntity)

    fun getAllSubscriptionsHistory(status: String) = db.getUserProfileDao().getAllSubscriptionsHistory(status)

    fun getSubscription(id: String): SubscriptionEntity = db.getUserProfileDao().getSubscription(id)

    fun updateSubscription(id: String, newDate: Long) = db.getUserProfileDao().updateSubscription(id, newDate)

    //Active orders and subscriptions

    fun upsertActiveOrders(id: ActiveOrders) = db.getUserProfileDao().upsertActiveOrders(id)

    fun upsertActiveSubscription(id: ActiveSubscriptions) = db.getUserProfileDao().upsertActiveSubscription(id)

    fun getAllActiveSubscriptions(): LiveData<List<String>> = db.getUserProfileDao().getAllActiveSubscriptions()

    fun getAllActiveOrders(): LiveData<List<String>> = db.getUserProfileDao().getAllActiveOrders()

    fun getAllActiveSubscriptionsStatic(): List<String> = db.getUserProfileDao().getAllActiveSubscriptionsStatic()

    fun getAllActiveOrdersStatic(): List<String> = db.getUserProfileDao().getAllActiveOrdersStatic()

    fun cancelActiveOrder(id: String) = db.getUserProfileDao().cancelActiveOrder(id)

    fun cancelActiveSubscription(id: String) = db.getUserProfileDao().cancelActiveSubscription(id)

    //specials
    fun upsertBestSellers(bestSellers: BestSellers) = db.getUserProfileDao().upsertBestSellers(bestSellers)
    fun upsertSpecialsOne(bestSellers: SpecialsOne) = db.getUserProfileDao().upsertSpecialsOne(bestSellers)
    fun upsertSpecialsTwo(bestSellers: SpecialsTwo) = db.getUserProfileDao().upsertSpecialsTwo(bestSellers)
    fun upsertSpecialsThree(bestSellers: SpecialsThree) = db.getUserProfileDao().upsertSpecialsThree(bestSellers)
    fun upsertSpecialBanners(banners: SpecialBanners) = db.getUserProfileDao().upsertSpecialBanners(banners)

    fun deleteBestSellers() = db.getUserProfileDao().deleteBestSellers()
    fun deleteSpecialsOne() = db.getUserProfileDao().deleteSpecialsOne()
    fun deleteSpecialsTwo() = db.getUserProfileDao().deleteSpecialsTwo()
    fun deleteSpecialsThree() = db.getUserProfileDao().deleteSpecialsThree()
    fun deleteSpecialBanners() = db.getUserProfileDao().deleteSpecialBanners()

    fun getBestSellers(): BestSellers = db.getUserProfileDao().getBestSellers()
    fun getSpecialsOne(): SpecialsOne = db.getUserProfileDao().getSpecialsOne()
    fun getSpecialsTwo(): SpecialsTwo = db.getUserProfileDao().getSpecialsTwo()
    fun getSpecialsThree(): SpecialsThree = db.getUserProfileDao().getSpecialsThree()
    fun getSpecialBanners(): List<SpecialBanners> = db.getUserProfileDao().getSpecialBanners()


    //notifications
    fun upsertNotification(userNotificationEntity: UserNotificationEntity) = db.getUserProfileDao().upsertNotification(userNotificationEntity)
    fun deleteAllNotifications() = db.getUserProfileDao().deleteAllNotifications()
    fun deleteNotificationsByID(id: String) = db.getUserProfileDao().deleteNotificationsByID(id)
    fun getAllNotifications(): List<UserNotificationEntity>?  = db.getUserProfileDao().getAllNotifications()
    fun getAllNotificationsBeforeDate(date: Int): List<UserNotificationEntity>? = db.getUserProfileDao().getAllNotificationsBeforeDate(date)

    //testimonials
    fun upsertTestimonial(testimonial: TestimonialsEntity) = db.getUserProfileDao().upsertTestimonial(testimonial)
    fun getAllTestimonials(): List<TestimonialsEntity> = db.getUserProfileDao().getAllTestimonials()
    fun deleteAllTestimonials() = db.getUserProfileDao().deleteAllTestimonials()


}