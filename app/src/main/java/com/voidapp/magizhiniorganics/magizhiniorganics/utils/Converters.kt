package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.view.Menu
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.allCWM.CWMViewModel

class Converters {
//
//    @TypeConverter
//    fun fromString(value: String?): ArrayList<String> {
//        val listType = TypeToken.getParameterized(ArrayList::class.java, String::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//
//    @TypeConverter
//    fun fromArrayList(list: ArrayList<String?>): String {
//        return Gson().toJson(list)
//    }
//
//    @TypeConverter
//    fun fromStringToArrayListLong(value: String?): ArrayList<Long> {
//        val listType = TypeToken.getParameterized(ArrayList::class.java, Long::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//
//    @TypeConverter
//    fun fromArrayListLongToString(list: ArrayList<Long?>): String {
//        return Gson().toJson(list)
//    }
//
//    @TypeConverter
//    fun fromStringToProductEntity(value: String?): ProductEntity {
//        val listType = TypeToken.getParameterized(ProductEntity::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//
//    @TypeConverter
//    fun fromProductEntity(list: ProductEntity): String {
//        return Gson().toJson(list)
//    }
//
//    @TypeConverter
//    fun fromStringToVariants(value: String?): ArrayList<ProductVariant> {
//        val listType = TypeToken.getParameterized(ProductVariant::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//
//    @TypeConverter
//    fun fromVariantList(list: ArrayList<ProductVariant?>): String {
//        return Gson().toJson(list)
//    }
//
//    @TypeConverter
//    fun fromStringToDefaultVariants(value: String?): ArrayList<DefaultVariant> {
//        val listType = TypeToken.getParameterized(DefaultVariant::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//
//    @TypeConverter
//    fun fromDefaultVariantList(list: ArrayList<DefaultVariant?>): String {
//        return Gson().toJson(list)
//    }
//
//    @TypeConverter
//    fun fromStringToCart(value: String?): ArrayList<Order> {
//        val listType = TypeToken.getParameterized(Order::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//
//    @TypeConverter
//    fun fromCartList(list: ArrayList<Order?>): String {
//        return Gson().toJson(list)
//    }
//
//    @TypeConverter
//    fun fromStringToReviewsArray(value: String?): ArrayList<Review> {
//        val listType = TypeToken.getParameterized(ArrayList::class.java, Review::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//    @TypeConverter
//    fun fromReviewArray(list: ArrayList<Review?>): String {
//        return Gson().toJson(list)
//    }
//
//    @TypeConverter
//    fun fromStringToAddressArray(value: String?): ArrayList<Address> {
//        val listType = TypeToken.getParameterized(ArrayList::class.java, Address::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//    @TypeConverter
//    fun fromAddressArray(list: ArrayList<Address?>): String {
//        return Gson().toJson(list)
//    }
//    @TypeConverter
//    fun fromStringToCartEntityArray(value: String?): ArrayList<Cart> {
//        val listType = TypeToken.getParameterized(ArrayList::class.java, Cart::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//    @TypeConverter
//    fun fromCartEntityArray(list: ArrayList<Cart?>): String {
//        return Gson().toJson(list)
//    }
//    @TypeConverter
//    fun fromStringToCartEntityList(value: String?): List<CartEntity> {
//        val listType = TypeToken.getParameterized(List::class.java, CartEntity::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//    @TypeConverter
//    fun fromCartEntityList(list: List<CartEntity?>): String {
//        return Gson().toJson(list)
//    }
//
//
//    @TypeConverter
//    fun fromStringToAddress(value: String?): Address {
//        val listType = TypeToken.getParameterized(Address::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//
//    @TypeConverter
//    fun fromAddress(list: Address): String {
//        return Gson().toJson(list)
//    }
//
//    @TypeConverter
//    fun fromStringToTransactionClass(value: String?): List<TransactionHistory> {
//        val listType = TypeToken.getParameterized(List::class.java, TransactionHistory::class.java).type
//        return Gson().fromJson(value, listType)
//    }
//
//    @TypeConverter
//    fun fromTransactionClass(list: List<TransactionHistory?>): String {
//        return Gson().toJson(list)
//    }
//
//    fun stringToCartConverter(value: String): MutableList<CartEntity> {
//        val listType = TypeToken.getParameterized(MutableList::class.java, CartEntity::class.java).type
//        return Gson().fromJson(value, listType)
//    }

    @TypeConverter
    fun fromString(value: String?): ArrayList<String> {
        val listType = object : TypeToken<ArrayList<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<String?>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromStringToArrayListLong(value: String?): ArrayList<Long> {
        val listType = object : TypeToken<ArrayList<Long>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayListLongToString(list: ArrayList<Long?>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromStringToProductEntity(value: String?): ProductEntity {
        val listType = object : TypeToken<ProductEntity>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromProductEntity(list: ProductEntity): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromStringToVariants(value: String?): ArrayList<ProductVariant> {
        val listType = object : TypeToken<ArrayList<ProductVariant>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromVariantList(list: ArrayList<ProductVariant?>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromStringToDefaultVariants(value: String?): ArrayList<DefaultVariant> {
        val listType = object : TypeToken<ArrayList<DefaultVariant>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromDefaultVariantList(list: ArrayList<DefaultVariant?>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromStringToCart(value: String?): ArrayList<Order> {
        val listType = object : TypeToken<ArrayList<Order>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromCartList(list: ArrayList<Order?>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromStringToReviewsArray(value: String?): ArrayList<Review> {
        val listType = object : TypeToken<ArrayList<Review>>() {}.type
        return Gson().fromJson(value, listType)
    }
    @TypeConverter
    fun fromReviewArray(list: ArrayList<Review?>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromStringToAddressArray(value: String?): ArrayList<Address> {
        val listType = object : TypeToken<ArrayList<Address>>() {}.type
        return Gson().fromJson(value, listType)
    }
    @TypeConverter
    fun fromAddressArray(list: ArrayList<Address?>): String {
        return Gson().toJson(list)
    }
    @TypeConverter
    fun fromStringToCartEntityArray(value: String?): ArrayList<Cart> {
        val listType = object : TypeToken<ArrayList<Cart>>() {}.type
        return Gson().fromJson(value, listType)
    }
    @TypeConverter
    fun fromCartEntityArray(list: ArrayList<Cart?>): String {
        return Gson().toJson(list)
    }
    @TypeConverter
    fun fromStringToCartEntityList(value: String?): List<CartEntity> {
        val listType = object : TypeToken<List<CartEntity>>() {}.type
        return Gson().fromJson(value, listType)
    }
    @TypeConverter
    fun fromCartEntityList(list: List<CartEntity?>): String {
        return Gson().toJson(list)
    }


    @TypeConverter
    fun fromStringToAddress(value: String?): Address {
        val listType = object : TypeToken<Address>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromAddress(list: Address): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromStringToTransactionClass(value: String?): List<TransactionHistory> {
        val listType = object : TypeToken<List<TransactionHistory>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromTransactionClass(list: List<TransactionHistory?>): String {
        return Gson().toJson(list)
    }

    fun stringToCartConverter(value: String): MutableList<CartEntity> {
        val listType = object : TypeToken<MutableList<CartEntity>>() {}.type
        return Gson().fromJson(value, listType)
    }

    //CWM to string converter to pass between activities
    fun cwmToStringConverter(value: CWMFood): String {
        return Gson().toJson(value)
    }

    fun stringToCwmConverter(value: String): CWMFood {
        val listType = object : TypeToken<CWMFood>() {}.type
        return Gson().fromJson(value, listType)
    }

    //Menu image to string converter to pass between activities
    fun menuToStringConverter(value: MutableList<MenuImage>): String {
        return Gson().toJson(value)
    }

    fun stringToMenuConverter(value: String): MutableList<MenuImage> {
        val listType = object : TypeToken<MutableList<MenuImage>>() {}.type
        return Gson().fromJson(value, listType)
    }
}