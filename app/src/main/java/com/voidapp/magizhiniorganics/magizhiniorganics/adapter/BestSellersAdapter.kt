package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvHomeTopSellersBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadSimple

class BestSellersAdapter(
    val context: Context,
    var products: List<ProductEntity>,
    val onItemClickListener: BestSellerItemClickListener
) : RecyclerView.Adapter<BestSellersAdapter.ProductHomeViewHolder>() {

    inner class ProductHomeViewHolder(val binding: RvHomeTopSellersBinding) :
        RecyclerView.ViewHolder(binding.root)

    private var variantDisplayName = ""

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

        variantDisplayName = when (product.variants[variantInCartPosition].variantType) {
            "Kilogram" -> "${product.variants[variantInCartPosition].variantName}Kg"
            "Gram" -> "${product.variants[variantInCartPosition].variantName}g"
            "Liter" -> "${product.variants[variantInCartPosition].variantName}l"
            "Unit" -> "${product.variants[variantInCartPosition].variantName} Unit"
            else -> "${product.variants[variantInCartPosition].variantName}ml"
        }

        with(holder.binding) {
            tvProductName.text = product.name
            checkVariantAvailability(holder, product.variants[variantInCartPosition])

            ivProductThumbnail.loadSimple(product.thumbnailUrl)

            //if discount is available then we make the discount layout visible and set the discount amount and percentage
            if (product.variants[variantInCartPosition].discountPrice != 0.0) {
                clDiscountLayout.visibility = View.VISIBLE
//                setDiscountedValues(holder, product, variantPrice, variantInCartPosition)

                with(product.variants[variantInCartPosition]) {
                    tvDiscountAmt.text =
                        "${getDiscountPercent(variantPrice.toFloat(), discountPrice.toFloat()).toInt()}% Off"
                    if (discountPrice != 0.0) {
                        tvPrice.text = "$variantDisplayName - Rs: ${discountPrice}"
                    }
                }
            } else {
                clDiscountLayout.visibility = View.INVISIBLE
                tvPrice.text = "$variantDisplayName - Rs: ${product.variants[variantInCartPosition].variantPrice}"
            }

            ivProductThumbnail.setOnClickListener {
                onItemClickListener.moveToProductDetails(productID, product.name, ivProductThumbnail)
            }

        }
    }

    override fun getItemCount(): Int {
        return products.size
    }

    private fun getDiscountPercent(price: Float, discountPrice: Float): Float
            = ((price-discountPrice)/price)*100

    private fun checkVariantAvailability(
        holder: BestSellersAdapter.ProductHomeViewHolder,
        variant: ProductVariant
    ) {
        with(holder.binding) {
            when (variant.status) {
                Constants.LIMITED -> {
                    ivCountBg.visibility = View.VISIBLE
//                    ivAdd.isEnabled = true
                }
                Constants.OUT_OF_STOCK -> {
                    ivCountBg.visibility = View.VISIBLE
                    tvProductName.visibility = View.VISIBLE
                    tvProductName.text = "Out of Stock"
//                    ivAdd.isEnabled = false

                }
                Constants.NO_LIMIT -> {
                    ivCountBg.visibility = View.VISIBLE
//                    ivAdd.isEnabled = true
                }
            }
        }
    }

    interface BestSellerItemClickListener {
        fun moveToProductDetails(productID: String,productName: String ,thumbnail: ShapeableImageView)
    }
}