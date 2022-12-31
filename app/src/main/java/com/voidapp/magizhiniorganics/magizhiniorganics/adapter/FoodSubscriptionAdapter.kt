package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.graphics.Paint
import android.service.controls.templates.ThumbnailTemplate
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.AmmaSpecial
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.MenuImage
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvFoodMenuItemsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadOriginal
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadSimple

class FoodSubscriptionAdapter(
    var ammaSpecials: List<MenuImage>,
    var onItemClickListener: FoodSubscriptionItemClickListener
): RecyclerView.Adapter<FoodSubscriptionAdapter.FoodDayViewHolder>() {

    inner class FoodDayViewHolder(var binding: RvFoodMenuItemsBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodDayViewHolder {
        val view = RvFoodMenuItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FoodDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodDayViewHolder, position: Int) {
        val item = ammaSpecials[position]
        holder.binding.apply {
            tvMenuName.text = item.name
            ivMenuThumbnail.loadOriginal(item.thumbnailUrl)
//            tvProductName.text = "${item.foodDay} - ${item.foodTime}"
//            tvPrice.text = if (item.discountedPrice != 0.0) {
//                tvDiscount.visibility = View.VISIBLE
//                tvDiscount.text = "Rs: ${item.price}"
//                tvDiscount.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
//                "${item.discountedPrice}"
//            } else {
//                tvDiscount.visibility = View.GONE
//                item.price.toString()
//            }
//            tvIngredients.text = listIngredients(item.ingredients)
            cvMenuItem.setOnClickListener {
                onItemClickListener.itemClicked(item.thumbnailUrl, ivMenuThumbnail)
            }
        }
    }

//    private fun listIngredients(ingredients: ArrayList<String>): String {
//        var ingredientListString = ""
//        for (item in 0 until ingredients.size) {
//            ingredientListString = if (item == 0) {
//                ingredients[item]
//            } else {
//                "$ingredientListString, ${ingredients[item]}"
//            }
//        }
//        ingredients.forEach {
////            ingredientListString = "$ingredientListString \n * $it"
//            ingredientListString = "$ingredientListString, $it"
//        }
//        return ingredientListString
//    }

    override fun getItemCount(): Int {
        return ammaSpecials.size
    }
}


interface  FoodSubscriptionItemClickListener {
    fun itemClicked(url: String, thumbnail: ShapeableImageView)
}
