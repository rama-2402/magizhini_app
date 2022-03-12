package com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ShoppingMainAdapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvShoppingItemsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.diffUtils.DiffUtils

open class ShoppingMainAdapter(
    private val context: Context,
    var products: MutableList<ProductEntity>,
    var limited: Boolean = false,
    private val onItemClickListener: ShoppingMainListener
) : RecyclerView.Adapter<ShoppingMainAdapter.ShoppingMainViewHolder>() {

    //we are defining this as a global variable coz this has to be accesible by the extension function that calculated the discounted price
    var discountedPrice: Float = 0F
    var totalPrice: Float = 0f
    //getting the current user id so that favorites can be added to firestore data
    val id: String =
        SharedPref(context).getData(Constants.USER_ID, Constants.STRING, "").toString()

    inner class ShoppingMainViewHolder(val binding: RvShoppingItemsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingMainViewHolder {
        val view =
            RvShoppingItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShoppingMainViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingMainViewHolder, position: Int) {

        val product = products[position]
        var selectedVariant: Int = 0

        //setting the variant details by default
        val variantNames = mutableListOf<String>()
        var variantName =
            "${product.variants[0].variantName} ${product.variants[0].variantType}"
        var variantPrice: Float = product.variants[0].variantPrice.toFloat()

        holder.binding.apply {
            //setting the thumbnail
            GlideLoader().loadUserPicture(ivProductThumbnail.context, product.thumbnailUrl, ivProductThumbnail)

            //details view
            tvProductName.text = product.name

            //setting the favorites icon for the products
            if (product.favorite) {
                ivFavourite.setImageResource(R.drawable.ic_favorite_filled)
            } else {
                ivFavourite.setImageResource(R.drawable.ic_favorite_outline)
            }

            //create a mutable list of variant names for the spinner
            for (i in 0 until product.variants.size) {
                val variant = "${product.variants[i].variantName} ${product.variants[i].variantType}"
                if (product.variantInCart.contains(variant)) {
                    selectedVariant = i
                }
                variantNames.add(variant)
            }

            //setting the adapter for the spinner with the mutable list of variant names
            val adapter = ArrayAdapter(
                spProductVariant.popupContext,
                R.layout.support_simple_spinner_dropdown_item,
                variantNames
            )
            spProductVariant.adapter = adapter
            spProductVariant.setSelection(selectedVariant)

            //click listener for the spinner item selection
            spProductVariant.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    variantposition: Int,
                    id: Long
                ) {
                    selectedVariant = variantposition
                    //setting the selected variant price and applying the discount for that variant
                    val variant = product.variants[selectedVariant]

                    variantName =
                        "${variant.variantName} ${variant.variantType}"
                    variantPrice = variant.variantPrice.toFloat()

                    //setting the original price without discount
                    tvDiscount.setTextAnimation("Rs: $variantPrice")
                    tvDiscount.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                    setDiscountedValues(holder, product, variantposition)
                    setCartItems(product, variantName, selectedVariant, holder)
                    checkVariantAvailability(holder, variant)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    checkVariantAvailability(holder, product.variants[selectedVariant])
                    tvDiscount.setTextAnimation("Rs: $variantPrice")
                    tvDiscount.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                    setDiscountedValues(holder, product, 0)
                    setCartItems(product, variantName, holder = holder)
                }
            }

            //favorites click listener
            ivFavourite.setOnClickListener {
                ivFavourite.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
                product.favorite = !product.favorite
                onItemClickListener.updateFavorites(id, product, position)
            }

            tvAddItem.setOnClickListener {
                val maxOrderQuantity = product.variants[selectedVariant].inventory
                if (limited) {
                    selectedVariant = product.defaultVariant
                }

                //checking if it is limited item. If yes, then item will not be added
                with(tvAddItem) {
                    when(text) {
                        "View" -> {
                            onItemClickListener.navigateToProduct(product)
                        }
                        "Add" -> {
                                onItemClickListener.upsertCartItem(
                                    product,
                                    position,
                                    variantName,
                                    1,
                                    totalPrice,
                                    variantPrice,
                                    selectedVariant,
                                    maxOrderQuantity
                                )
                        }
                        else -> {
                            onItemClickListener.deleteCartItemFromShoppingMain(product, variantName, position)
                        }
                    }
                }
            }

            ivProductThumbnail.setOnClickListener {
                onItemClickListener.navigateToProduct(product)
            }
        }
    }

    private fun setDiscountedValues(
        holder: ShoppingMainAdapter.ShoppingMainViewHolder,
        product: ProductEntity,
        position: Int
    ) {
        val currentVariant = product.variants[position]

        holder.binding.apply {
            //setting up the product discount info
            if (currentVariant.discountPrice != 0.0) {
                tvDiscountAmt.text =
                    getDiscountPercent(currentVariant.variantPrice.toFloat(), currentVariant.discountPrice.toFloat()).toString()
                discountedPrice = currentVariant.discountPrice.toFloat()
                totalPrice = currentVariant.discountPrice.toFloat()
                tvPrice.setTextAnimation(totalPrice.toString(), 200)
                clDiscountLayout.fadInAnimation(200)
//                clDiscountLayout.visibility = View.VISIBLE
                tvDiscount.fadInAnimation(200)
//                tvDiscount.visibility = View.VISIBLE
            } else {
                discountedPrice = currentVariant.variantPrice.toFloat()
                totalPrice = discountedPrice
                tvPrice.setTextAnimation(totalPrice.toString())
                clDiscountLayout.fadOutAnimation(200)
                tvDiscount.fadOutAnimation(200)
            }
        }
    }

    private fun getDiscountPercent(price: Float, discountPrice: Float): Float
            = ((price-discountPrice)/price)*100

    private fun checkVariantAvailability(
        holder: ShoppingMainAdapter.ShoppingMainViewHolder,
        variant: ProductVariant
    ) {
        holder.binding.apply {
            when (variant.status) {
                Constants.LIMITED -> {
                    ivCountBg.visibility = View.VISIBLE
                    tvItemCount.visibility = View.VISIBLE
                    tvItemCount.setTextAnimation("Available: ${variant.inventory}")
                    tvAddItem.isEnabled = true
                }
                Constants.OUT_OF_STOCK -> {
                    ivCountBg.visibility = View.VISIBLE
                    tvItemCount.visibility = View.VISIBLE
                    tvItemCount.setTextAnimation("Out of Stock")
                    tvAddItem.isEnabled = false

                }
                Constants.NO_LIMIT -> {
                    ivCountBg.visibility = View.GONE
                    tvItemCount.visibility = View.GONE
                    tvItemCount.setTextAnimation("")
                    tvAddItem.isEnabled = true
                }
            }
        }
    }

    private fun setCartItems(
        product: ProductEntity,
        variantName: String,
        selectedVariant: Int = 0,
        holder: ShoppingMainAdapter.ShoppingMainViewHolder
    ) {
        when {
            product.productType == Constants.SUBSCRIPTION ->
                with(holder.binding.tvAddItem) {
                    visibility = View.VISIBLE
                    text = "View"
                    backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green_base))
                    setBackgroundResource(R.drawable.shape_round_rectangle_8)
            }
            product.variants[selectedVariant].status == Constants.OUT_OF_STOCK -> {
                holder.binding.tvAddItem.visibility = View.INVISIBLE
            }
            else -> {
                holder.binding.tvAddItem.apply {
                    visibility = View.VISIBLE
                    if (product.variantInCart.contains(variantName)) {
                        text = "Remove"
                        backgroundTintList =
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.matteRed))
                        setBackgroundResource(R.drawable.shape_round_rectangle_8)
                    } else {
                        text = "Add"
                        backgroundTintList =
                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green_base))
                        setBackgroundResource(R.drawable.shape_round_rectangle_8)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return products.size
    }

    fun setData(newList: MutableList<ProductEntity>) {
        val diffUtil = DiffUtils(products, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        products = newList
        diffResult.dispatchUpdatesTo(this)
    }

    interface ShoppingMainListener {
        fun limitedItemList(products: List<ProductEntity>)
        fun updateFavorites(id: String, product: ProductEntity, position: Int)
        fun navigateToProduct(product: ProductEntity)
        fun upsertCartItem(product: ProductEntity, position: Int , variant: String, count: Int, price: Float, originalPrice: Float, variantIndex: Int, maxOrderQuantity: Int)
        fun deleteCartItemFromShoppingMain(product: ProductEntity, variantName: String, position: Int)
    }
}