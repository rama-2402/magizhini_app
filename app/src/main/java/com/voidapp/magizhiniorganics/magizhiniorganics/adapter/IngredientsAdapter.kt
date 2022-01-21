package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CWMFood
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Cart
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvDishItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvIngredientsItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader

class IngredientsAdapter(
    private val context: Context,
    var ingredients: MutableList<CartEntity>,
    private val onItemClickListener: IngredientsAdapter.IngredientsClickListener
): RecyclerView.Adapter<IngredientsAdapter.IngredientsViewHolder>() {

    inner class IngredientsViewHolder(val binding: RvIngredientsItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientsViewHolder {
        val view = RvIngredientsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IngredientsViewHolder(view)
    }

    override fun onBindViewHolder(holder: IngredientsViewHolder, position: Int) {
        val cartItem = ingredients[position]
        holder.binding.apply {
            GlideLoader().loadUserPicture(context, cartItem.thumbnailUrl, ivProductThumbnail)
            tvProductName.text = cartItem.productName
            tvVariantName.text = cartItem.variant
            tvQuantity.text = "Qty: x${cartItem.quantity}"
            tvPrice.text = "Rs: ${cartItem.price}"
            tvDiscount.visibility = View.INVISIBLE

            clProductItem.setOnClickListener {
                onItemClickListener.selectedItem(cartItem.productId)
            }
        }
    }

    override fun getItemCount(): Int {
        return ingredients.size
    }

    interface IngredientsClickListener {
        fun selectedItem(productID: String)
        fun setFavorites(productID: String)
    }
}