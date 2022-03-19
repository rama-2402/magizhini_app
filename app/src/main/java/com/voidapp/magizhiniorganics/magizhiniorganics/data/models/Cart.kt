package com.voidapp.magizhiniorganics.magizhiniorganics.data.models

import android.os.Parcelable
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import kotlinx.parcelize.Parcelize

@Parcelize
data class Cart(
    var id: Int = 0,
    var variant: String = "",
    var productId: String = "",
    var productName: String = "",
    var thumbnailUrl: String = "",
    var quantity: Int = 0,
    var maxOrderQuantity: Int = 0,
    var price: Float = 0F,
    var originalPrice: Float = 0F,
    var variantIndex: Int = 0
): Parcelable

private fun Cart.toCartEntity() = CartEntity(
    id = id,
    variant = variant,
    productId = productId,
    productName = productName,
    thumbnailUrl = thumbnailUrl,
    quantity = quantity,
    maxOrderQuantity = maxOrderQuantity,
    price = price,
    originalPrice = originalPrice
)