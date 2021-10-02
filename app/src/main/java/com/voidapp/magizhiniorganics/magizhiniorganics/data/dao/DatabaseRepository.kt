package com.voidapp.magizhiniorganics.magizhiniorganics.data.dao

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

    fun updateCartItem(id: Int, count: Int) = db.getUserProfileDao().updateCartItem(id, count)

    fun updateCartItemPrice(id: Int, price: Float) = db.getUserProfileDao().updateCartItemPrice(id, price)

    fun getProfileData() = db.getUserProfileDao().getProfileData()

    fun getAllCartItems() = db.getUserProfileDao().getAllCartItems()

    fun getCartItem(id: Int) = db.getUserProfileDao().getCartItem(id)

    fun getAllDefaultVariantsList() = db.getUserProfileDao().getAllDefaultVariantsList()

    fun getAllproducts() = db.getUserProfileDao().getAllProducts()

    fun getProductWithId(id: String) = db.getUserProfileDao().getProductWithId(id)

    fun getAllProductCategories() = db.getUserProfileDao().getAllProductCategories()

    fun getAllCoupons() = db.getUserProfileDao().getAllCoupons()

    fun getAllBanners() = db.getUserProfileDao().getAllBanners()

    fun getAllProductsInCategory(category: String) = db.getUserProfileDao().getAllProductsInCategory(category)

    fun getAllDiscountProducts() = db.getUserProfileDao().getAllDiscountProducts()

    fun getAllActiveCoupons(status: String) = db.getUserProfileDao().getAllActiveCoupons(status)

    fun getOrderHistory() = db.getUserProfileDao().getOrderHistory()

    //updating the product entity with the user prefence favorites and cart items
    fun updateFavorites(favorites: String, status: Boolean) = db.getUserProfileDao().updateFavorties(favorites, status)

    fun updateCartItemsToEntity(id: String, status: Boolean, coupon: String) = db.getUserProfileDao().updateCartItemsToEntity(id, status, coupon)

    fun getAllCartItemsForEntityUpdate() = db.getUserProfileDao().getAllCartItemsForEntityUpdate()

    fun getProductWithIdForUpdate(id: String) = db.getUserProfileDao().getProductWithIdForUpdate(id)

    fun getFavorites() = db.getUserProfileDao().getFavorites()

    fun getAllProductsStatic() = db.getUserProfileDao().getAllProductsStatic()

    fun getAllProductByCategoryStatic(category: String) = db.getUserProfileDao().getAllProductByCategoryStatic(category)

    fun getAllFavoritesStatic() = db.getUserProfileDao().getAllFavoritesStatic()

    fun getAllCategoryNames() = db.getUserProfileDao().getAllCategoryNames()

    fun getCartPrice() = db.getUserProfileDao().getCartPrice()

    fun getCouponByCode(code: String) = db.getUserProfileDao().getCouponByCode(code)

    //wallet
    fun getWallet() = db.getUserProfileDao().getWallet()

    fun upsertWallet(walletEntity: WalletEntity) = db.getUserProfileDao().upsertWallet(walletEntity)

    //pincodes
    fun upsertPinCodes(pinCodes: PinCodesEntity) = db.getUserProfileDao().upsertPinCodes(pinCodes)

    fun getDeliveryCharge(areaCode: String) = db.getUserProfileDao().getDeliveryCharge(areaCode)
}