package com.voidapp.magizhiniorganics.magizhiniorganics.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Cart

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM ProductEntity WHERE id = :id")
    fun getID(id: String): ProductEntity

    @Query("DELETE FROM UserProfileEntity")
    fun deleteUserProfile()

    @Query("DELETE FROM CartEntity WHERE id = :id")
    fun deleteCartItem(id: Int)

    @Query("DELETE FROM CartEntity WHERE productId = :productId AND variant = :variantName")
    fun deleteCartItemFromShoppingMain(productId: String, variantName: String)

    @Query("DELETE FROM CartEntity")
    fun clearCart()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertProfile(userProfile: UserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertCartItem(cartEntity: CartEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertDefaultVariant(variant: VariantEntity)

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

    @Query("SELECT * FROM VariantEntity")
    fun getAllDefaultVariantsList(): LiveData<List<VariantEntity>>

    @Query("SELECT * FROM ProductEntity WHERE activated")
    fun getAllProducts(): LiveData<List<ProductEntity>>

    @Query("SELECT * FROM ProductEntity WHERE activated ORDER BY name")
    fun getAllProductsStatic(): List<ProductEntity>

    @Query("SELECT * FROM productEntity WHERE id = :id")
    fun getProductWithId(id: String): LiveData<ProductEntity>

    @Query("SELECT * FROM ProductCategoryEntity WHERE activated")
    fun getAllProductCategories(): LiveData<List<ProductCategoryEntity>>

    @Query("SELECT * FROM CouponEntity")
    fun getAllCoupons(): LiveData<List<CouponEntity>>

    @Query("SELECT * FROM BannerEntity")
    fun getAllBanners(): LiveData<List<BannerEntity>>

    @Query("SELECT * FROM PRODUCTENTITY WHERE category = :category AND activated")
    fun getAllProductsInCategory(category: String): LiveData<List<ProductEntity>>

    @Query("SELECT * FROM PRODUCTENTITY WHERE discountAvailable ORDER BY name")
    fun getAllDiscountProducts(): List<ProductEntity>

    @Query("SELECT * FROM CouponEntity WHERE status = :status")
    fun getAllActiveCoupons(status: String): List<CouponEntity>

    @Query("SELECT * FROM OrderEntity WHERE monthYear = :filter ")
    fun getOrderHistory(filter: String): List<OrderEntity>

    @Query("SELECT * FROM OrderEntity WHERE orderId = :id")
    fun getOrderByID(id: String): OrderEntity?

    //updating the entity based on the user preference
    @Query("UPDATE ProductEntity SET favorite = :status WHERE id = :favorites")
    fun updateFavorties(favorites: String, status: Boolean)

    @Query("UPDATE ProductEntity SET inCart = :status , appliedCoupon = :coupon WHERE id = :id")
    fun updateCartItemsToEntity(id: String, status: Boolean, coupon: String)

    @Query("SELECT * FROM CartEntity")
    fun getAllCartItemsForEntityUpdate() : List<CartEntity>

    @Query("SELECT * FROM productEntity WHERE id = :id")
    fun getProductWithIdForUpdate(id: String) : ProductEntity

    @Query("SELECT * FROM ProductEntity WHERE appliedCoupon = :filter")
    fun getAllSubscriptions(filter: String): List<ProductEntity>

    @Query("SELECT * FROM productentity WHERE favorite ORDER BY name")
    fun getFavorites(): LiveData<List<ProductEntity>>

    @Query("SELECT * FROM PRODUCTENTITY WHERE category = :category AND activated ORDER BY name")
    fun getAllProductByCategoryStatic(category: String): List<ProductEntity>

    @Query("SELECT * FROM ProductEntity WHERE favorite ORDER BY name")
    fun getAllFavoritesStatic(): List<ProductEntity>

    @Query("SELECT name FROM ProductCategoryEntity WHERE activated ORDER BY name")
    fun getAllCategoryNames(): List<String>

    @Query("SELECT SUM(price * quantity) FROM CartEntity")
    fun getCartPrice(): LiveData<Float>

    @Query("SELECT * FROM CouponEntity WHERE code = :code")
    fun getCouponByCode(code: String): CouponEntity?


    //wallet
    @Query("SELECT * FROM WalletEntity")
    fun getWallet(): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertWallet(wallet: WalletEntity)

    //pincodes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertPinCodes(PinCodes: PinCodesEntity)

    @Query("SELECT * FROM PinCodesEntity WHERE areaCode = :areaCode")
    fun getDeliveryCharge(areaCode: String): PinCodesEntity

}