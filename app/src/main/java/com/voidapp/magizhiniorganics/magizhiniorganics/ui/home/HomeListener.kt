package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductCategoryEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Banner
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductCategory
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem

interface HomeListener {
    fun onDataTransactionFailure(message: String)
    fun displaySelectedCategory(category: String)
    fun moveToProductDetails(id: String, name: String)
}