package com.voidapp.magizhiniorganics.magizhiniorganics.adapter.HomeRvAdapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductCategoryEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader

class CategoryHomeAdapter(
    val context: Context,
    var categories: List<ProductCategoryEntity>,
    val viewModel: HomeViewModel
): RecyclerView.Adapter<CategoryHomeAdapter.ProductsHomeViewModel>() {

    inner class ProductsHomeViewModel(itemView: View): RecyclerView.ViewHolder(itemView) {
        val categoryThumbNail = itemView.findViewById<ImageView>(R.id.ivProductThumbnail)
        val categoryName = itemView.findViewById<TextView>(R.id.tvProductName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductsHomeViewModel {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_home_product_items, parent, false)
        return ProductsHomeViewModel(view)
    }

    override fun onBindViewHolder(holder: ProductsHomeViewModel, position: Int) {

        val categoryEntity = categories[position]

        holder.categoryName.text = categoryEntity.name

        //loading the product images
        GlideLoader().loadUserPicture(
            context,
            categoryEntity.thumbnailUrl,
            holder.categoryThumbNail
        )
        holder.categoryThumbNail.clipToOutline = true

        holder.itemView.setOnClickListener {
            viewModel.selectedCategory(categoryEntity.name)
            Log.e("cat",categoryEntity.name)
        }

    }

    override fun getItemCount(): Int {
        return categories.size
    }
}