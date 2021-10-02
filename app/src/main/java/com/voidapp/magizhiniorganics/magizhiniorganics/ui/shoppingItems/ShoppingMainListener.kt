package com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems

import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity

interface ShoppingMainListener {
    fun limitedItemList(products: List<ProductEntity>)
//    fun updateFavorites(position: Int)
    fun moveToProductDetails(id: String, name: String)


}