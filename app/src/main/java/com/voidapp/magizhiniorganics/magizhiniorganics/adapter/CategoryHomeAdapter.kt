package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductCategoryEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvHomeCategoryItemsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvHomeProductItemsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg

class CategoryHomeAdapter(
    val context: Context,
    var categories: List<ProductCategoryEntity>,
    val onItemClickListener: CategoryItemClickListener
): RecyclerView.Adapter<CategoryHomeAdapter.ProductsHomeViewModel>() {

    inner class ProductsHomeViewModel(val binding: RvHomeProductItemsBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductsHomeViewModel {
        val view = RvHomeProductItemsBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductsHomeViewModel(view)
    }

    override fun onBindViewHolder(holder: ProductsHomeViewModel, position: Int) {

        val categoryEntity = categories[position]

        holder.binding.apply {
            tvProductName.text = categoryEntity.name

            //loading the product images
            ivProductThumbnail.loadImg(categoryEntity.thumbnailUrl)  {}
            ivProductThumbnail.clipToOutline = true

            cvProductItem.setOnClickListener {
                onItemClickListener.selectedCategory(categoryEntity.name)
            }
        }

    }

    override fun getItemCount(): Int {
        return categories.size
    }

    interface CategoryItemClickListener {
        fun selectedCategory(categoryName: String)
    }
}