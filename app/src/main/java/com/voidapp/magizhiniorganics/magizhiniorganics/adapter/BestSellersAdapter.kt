package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvHomeTopSellersBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref

class BestSellersAdapter(
    val context: Context,
    var products: List<ProductEntity>,
    val viewModel: HomeViewModel,
    var recycler: String
) : RecyclerView.Adapter<BestSellersAdapter.ProductHomeViewHolder>() {

    inner class ProductHomeViewHolder(val binding: RvHomeTopSellersBinding) :
        RecyclerView.ViewHolder(binding.root)

    var discountedPrice = 0f
    var variantDisplayName = ""
    val id: String =
        SharedPref(context).getData(Constants.USER_ID, Constants.STRING, "").toString()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductHomeViewHolder {
        val view =
            RvHomeTopSellersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductHomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductHomeViewHolder, position: Int) {
        val product = products[position]
        val productID = product.id
        val variantName = "${product.variants[0].variantName} ${product.variants[0].variantType}"
        variantDisplayName = when (product.variants[0].variantType) {
            "Kilogram" -> "${product.variants[0].variantName} Kg"
            "Gram" -> "${product.variants[0].variantName} G"
            "Liter" -> "${product.variants[0].variantName} L"
            else -> "${product.variants[0].variantName} mL"
        }
        var variantInCartPosition: Int = product.defaultVariant
        var variantPrice: Float = product.variants[0].variantPrice.toFloat()
        with(holder.binding) {
            tvProductName.text = product.name
            checkVariantAvailability(holder, product.variants[0])
            GlideLoader().loadUserPicture(context, product.thumbnailUrl, ivProductThumbnail)

//            //setting the favorties icon for the products
//            if (product.favorite) {
//                ivFavourite.setImageResource(R.drawable.ic_favorite_filled)
//            } else {
//                ivFavourite.setImageResource(R.drawable.ic_favorite_outline)
//            }

            if (!product.variantInCart.contains(variantName)) {
                with(holder.binding) {
                    ivAdd.backgroundTintList = ColorStateList.valueOf((ContextCompat.getColor(ivAdd.context, R.color.green_base)))
                    ivAdd.setImageDrawable(
                        ContextCompat.getDrawable(
                            ivAdd.context,
                            R.drawable.ic_add
                        )
                    )
                }
            } else {
                variantInCartPosition = 0
                ivAdd.backgroundTintList = ColorStateList.valueOf((ContextCompat.getColor(ivAdd.context, R.color.matteRed)))
                ivAdd.setImageDrawable(
                    ContextCompat.getDrawable(
                        ivAdd.context,
                        R.drawable.ic_delete
                    )
                )
            }

            //if discount is available then we make the discount layout visible and set the discount amount and percentage
            if (product.discountAvailable) {
                clDiscountLayout.visibility = View.VISIBLE
                setDiscountedValues(holder, product, variantPrice, variantInCartPosition)
            } else {
                clDiscountLayout.visibility = View.INVISIBLE
                tvPrice.text = "$variantDisplayName - Rs: $variantPrice"
            }
//
//            //favorites click listener
//            ivFavourite.setOnClickListener {
//                ivFavourite.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
//                product.favorite = !product.favorite
//                viewModel.updateFavorites(id, product)
//            }

            ivAdd.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
                if (product.variantInCart.contains(variantName)) {
                    product.variantInCart.remove(variantName)
                    if (product.variantInCart.isEmpty()) {
                        product.inCart = false
                    }
                    removeItem(product, viewModel, position)
                } else {
                    product.variantInCart.add(variantName)
                    addItem(product, viewModel, position)
                }
            }

            ivProductThumbnail.setOnClickListener {
                viewModel.moveToProductDetails(productID, product.name)
            }

        }
    }

    override fun getItemCount(): Int {
        return products.size
    }

    private fun addItem(
        product: ProductEntity,
        viewModel: HomeViewModel,
        position: Int
    ) {
        Toast.makeText(context, "Added to Cart", Toast.LENGTH_SHORT).show()
        viewModel.upsertCartItem(
            product,
            "${product.variants[0].variantName} ${product.variants[0].variantType}",
            1,
            discountedPrice,
            product.variants[0].variantPrice.toFloat(),
            0,
            position,
            recycler
        )
    }

    private fun removeItem(
        product: ProductEntity,
        viewModel: HomeViewModel,
        position: Int
    ) {
        Toast.makeText(context, "Removed from Cart", Toast.LENGTH_SHORT).show()
        viewModel.deleteCartItemFromShoppingMain(
            product,
            "${product.variants[0].variantName} ${product.variants[0].variantType}",
            position,
            recycler
        )
    }

    private fun checkVariantAvailability(
        holder: BestSellersAdapter.ProductHomeViewHolder,
        variant: ProductVariant
    ) {
        with(holder.binding) {
            when (variant.status) {
                Constants.LIMITED -> {
                    ivCountBg.visibility = View.VISIBLE
                    ivAdd.isEnabled = true
                }
                Constants.OUT_OF_STOCK -> {
                    ivCountBg.visibility = View.VISIBLE
                    tvProductName.visibility = View.VISIBLE
                    tvProductName.text = "Out of Stock"
                    ivAdd.isEnabled = false

                }
                Constants.NO_LIMIT -> {
                    ivCountBg.visibility = View.VISIBLE
                    ivAdd.isEnabled = true
                }
            }
        }
    }

    private fun setDiscountedValues(
        holder: BestSellersAdapter.ProductHomeViewHolder,
        product: ProductEntity,
        variantPrice: Float,
        position: Int
    ) {
        with(holder.binding) {
            if (product.discountAvailable) {
                tvDiscountAmt.text =
                    product.variants[position].discountPercent.toString()
                //setting up the product discount info
                if (product.variants[position].discountType == "Percentage") {
                    tvDiscountType.text = "%"
                    discountedPrice = calculateDiscountedAmount(
                        "Percentage",
                        product.variants[position].discountPercent,
                        product.variants[position].variantPrice
                    ).toFloat()
                    tvPrice.text =
                        "$variantDisplayName - Rs: $discountedPrice"
                } else {
                    tvDiscountType.text = "Rs"
                    discountedPrice = calculateDiscountedAmount(
                        "rupees",
                        product.variants[position].discountPercent,
                        product.variants[position].variantPrice
                    ).toFloat()
                    tvPrice.text = "$variantDisplayName - Rs: $discountedPrice"
                }
            } else {
                tvPrice.text = "$variantDisplayName - Rs: $variantPrice"
                discountedPrice = variantPrice
            }
        }
    }

    private fun calculateDiscountedAmount(
        discountType: String,
        discount: Int,
        price: String
    ): String {
        return when (discountType) {
            "Percentage" -> {
                "${(price.toFloat() - (price.toFloat() * discount) / 100)}"
            }
            "rupees" -> {
                "${price.toFloat() - discount}"
            }
            else -> "0"
        }
    }

}