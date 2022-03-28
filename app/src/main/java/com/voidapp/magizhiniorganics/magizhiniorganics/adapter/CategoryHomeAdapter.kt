package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductCategoryEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvHomeProductItemsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadSimple
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.http.Url
import java.io.InputStream
import java.net.URL


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
//            CoroutineScope(Dispatchers.IO).launch {
//                val url: URL = URL(categoryEntity.thumbnailUrl)
//                val img: InputStream = url.content as InputStream
//                val options = BitmapFactory.Options()
//                options.inPreferredConfig = Bitmap.Config.RGB_565
//                val image = BitmapFactory.decodeStream(img, null, options)
//                withContext(Dispatchers.Main) {
//                    image?.let { ivProductThumbnail.loadImg(it) {} }
//                }
//            }
            ivProductThumbnail.loadSimple(categoryEntity.thumbnailUrl)
//            ivProductThumbnail.loadImg(categoryEntity.thumbnailUrl) {}

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


    fun setCategoriesData(newList: List<ProductCategoryEntity>) {
        val diffUtil = CategoriesDiffUtil(categories, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        categories = newList as ArrayList<ProductCategoryEntity>
        diffResult.dispatchUpdatesTo(this)
    }

    class CategoriesDiffUtil(
        private val oldList: List<ProductCategoryEntity>,
        private val newList: List<ProductCategoryEntity>
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return when {
                oldList[oldItemPosition].id != newList[newItemPosition].id -> false
                else -> true
            }
        }
    }
}