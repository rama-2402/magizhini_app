package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import java.math.RoundingMode
import java.text.DecimalFormat

object Utils {
    fun String.addCharAtIndex(char: Char, index: Int) =
        StringBuilder(this).apply { insert(index, char) }.toString()

    fun createOrderForWhatsapp(cartItems: ArrayList<CartEntity>): String {
        var cartString = ""
        var no: Int = 0
        cartItems.forEach { item ->
            no += 1
            cartString = "$cartString \n"
            cartString = "$cartString >>>$no. ${item.productName}"
            cartString = "\t $cartString ${item.variant}"
            cartString = "\t $cartString QTY: ${item.quantity}"
            cartString = "\t $cartString Original Price: ${item.originalPrice}"
            cartString = "\t $cartString  Order Price: ${item.price}"
        }
        return cartString
    }

    fun toStringForSharedPref(address: Address): String {
        var stringAddress = address.userId
        stringAddress = "$stringAddress:::${address.addressLineOne}"
        stringAddress = "$stringAddress:::${address.addressLineTwo}"
        stringAddress = "$stringAddress:::${address.city}"
        stringAddress = "$stringAddress:::${address.LocationCode}"
        return  stringAddress
    }

    fun toAddressDataClass(addressString: String): Address {
        val address: Address = Address()
        val addressSplitList = addressString.split(":::")
        address.let {
            it.userId = addressSplitList[0]
            it.addressLineOne = addressSplitList[1]
            it.addressLineTwo = addressSplitList[2]
            it.city = addressSplitList[3]
            it.LocationCode = addressSplitList[4]
        }
        return  address
    }

    fun roundPrice(price: Float): Float {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(price).toFloat()
    }
}