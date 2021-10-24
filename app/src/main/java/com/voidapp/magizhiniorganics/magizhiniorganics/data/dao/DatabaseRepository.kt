package com.voidapp.magizhiniorganics.magizhiniorganics.data.dao

import androidx.lifecycle.LiveData
import com.voidapp.magizhiniorganics.magizhiniorganics.data.UserDatabase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*

class DatabaseRepository(
    private val db: UserDatabase
) {

    fun getid(id: String) = db.getUserProfileDao().getID(id)

    fun deleteUserProfile() = db.getUserProfileDao().deleteUserProfile()

    fun deleteCartItem(id: Int) = db.getUserProfileDao().deleteCartItem(id)

    fun deleteCartItemFromShoppingMain(productId: String, variantName: String) = db.getUserProfileDao().deleteCartItemFromShoppingMain(productId, variantName)

    fun clearCart() = db.getUserProfileDao().clearCart()

    fun upsertProfile(profile: UserProfileEntity) = db.getUserProfileDao().upsertProfile(profile)

    fun upsertCart(cartEntity: CartEntity) = db.getUserProfileDao().upsertCartItem(cartEntity)

    fun upsertDefaultVariant(variant: VariantEntity) = db.getUserProfileDao().upsertDefaultVariant(variant)

    fun upsertProduct(product: ProductEntity) = db.getUserProfileDao().upsertProduct(product)

    fun upsertProductCategory(category: ProductCategoryEntity) = db.getUserProfileDao().upsertCategory(category)

    fun upsertCoupon(coupon: CouponEntity) = db.getUserProfileDao().upsertCoupon(coupon)

    fun upsertBanner(bannerEntity: BannerEntity) = db.getUserProfileDao().upsertBanner(bannerEntity)

    fun upsertOrder(orderEntity: OrderEntity) = db.getUserProfileDao().upsertOrder(orderEntity)

    fun orderCancelled(id: String, status: String) = db.getUserProfileDao().orderCancelled(id, status)

    fun updateCartItem(id: Int, count: Int) = db.getUserProfileDao().updateCartItem(id, count)

    fun updateCartItemPrice(id: Int, price: Float) = db.getUserProfileDao().updateCartItemPrice(id, price)

    fun getProfileData() = db.getUserProfileDao().getProfileData()

    fun getAllCartItems() = db.getUserProfileDao().getAllCartItems()

    fun getCartItem(id: Int) = db.getUserProfileDao().getCartItem(id)

    fun getAllDefaultVariantsList() = db.getUserProfileDao().getAllDefaultVariantsList()

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

    fun getProductWithIdForUpdate(id: String) = db.getUserProfileDao().getProductWithIdForUpdate(id)

    fun getAllProductsStatic() = db.getUserProfileDao().getAllProductsStatic()

    fun getAllProductByCategoryStatic(category: String) = db.getUserProfileDao().getAllProductByCategoryStatic(category)

    fun getAllFavoritesStatic() = db.getUserProfileDao().getAllFavoritesStatic()

    fun upsertFavorite(id: Favorites) = db.getUserProfileDao().upsertFavorite(id)

    fun deleteFavorite(id: String) = db.getUserProfileDao().deleteFavorite(id)

    fun getFavorites(): List<Favorites>? = db.getUserProfileDao().getFavorites()

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

    fun cancelSubscription(subscriptionEntity: SubscriptionEntity) = db.getUserProfileDao().cancelSubscription(subscriptionEntity)

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


}