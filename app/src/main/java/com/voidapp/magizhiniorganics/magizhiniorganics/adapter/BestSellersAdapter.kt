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

    private var variantDisplayName = ""
    var discountAppliedPrice = 0f
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
        var variantInCartPosition: Int = 0
        for (i in product.variants.indices) {
            if (product.variants[0].status != Constants.LIMITED) {
                variantInCartPosition = i
                break
            }
        }
        val variantName = "${product.variants[variantInCartPosition].variantName} ${product.variants[variantInCartPosition].variantType}"
        variantDisplayName = when (product.variants[variantInCartPosition].variantType) {
            "Kilogram" -> "${product.variants[variantInCartPosition].variantName} Kg"
            "Gram" -> "${product.variants[variantInCartPosition].variantName} G"
            "Liter" -> "${product.variants[variantInCartPosition].variantName} L"
            else -> "${product.variants[variantInCartPosition].variantName} mL"
        }

        getVariantPrice(product.variants[variantInCartPosition])

//        var variantPrice: Float = product.variants[0].variantPrice.toFloat()
        with(holder.binding) {
            tvProductName.text = product.name
            checkVariantAvailability(holder, product.variants[variantInCartPosition])
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
                ivAdd.backgroundTintList = ColorStateList.valueOf((ContextCompat.getColor(ivAdd.context, R.color.matteRed)))
                ivAdd.setImageDrawable(
                    ContextCompat.getDrawable(
                        ivAdd.context,
                        R.drawable.ic_delete
                    )
                )
            }

            //if discount is available then we make the discount layout visible and set the discount amount and percentage
            if (product.variants[variantInCartPosition].discountPrice != 0.0) {
                clDiscountLayout.visibility = View.VISIBLE
//                setDiscountedValues(holder, product, variantPrice, variantInCartPosition)

                with(product.variants[variantInCartPosition]) {
                    tvDiscountAmt.text =
                        getDiscountPercent(variantPrice.toFloat(), discountPrice.toFloat()).toString()
                    if (discountPrice != 0.0) {
                        tvPrice.text = "$variantDisplayName - Rs: ${discountPrice}"
                    }
                }
            } else {
                clDiscountLayout.visibility = View.INVISIBLE
                tvPrice.text = "$variantDisplayName - Rs: ${product.variants[variantInCartPosition].variantPrice}"
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
                    addItem(product, viewModel, position, variantInCartPosition)
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

    private fun getDiscountPercent(price: Float, discountPrice: Float): Float
            = ((price-discountPrice)/price)*100

    private fun addItem(
        product: ProductEntity,
        viewModel: HomeViewModel,
        position: Int,
        variantPosition: Int
    ) {
        Toast.makeText(context, "Added to Cart", Toast.LENGTH_SHORT).show()
        viewModel.upsertCartItem(
            product,
            "${product.variants[variantPosition].variantName} ${product.variants[variantPosition].variantType}",
            1,
            discountAppliedPrice,
            product.variants[variantPosition].variantPrice.toFloat(),
            0,
            position,
            recycler
        )
    }

    private fun getVariantPrice(variant: ProductVariant) : Float {
        discountAppliedPrice = if (variant.discountPrice != 0.0) {
            variant.variantPrice.toFloat()
        } else {
            variant.discountPrice.toFloat()
        }
        return discountAppliedPrice
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
}