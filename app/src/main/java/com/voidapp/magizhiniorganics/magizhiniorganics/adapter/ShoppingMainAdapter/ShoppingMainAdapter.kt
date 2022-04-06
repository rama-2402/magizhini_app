package com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ShoppingMainAdapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvShoppingMainItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.OUT_OF_STOCK
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUBSCRIPTION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.diffUtils.DiffUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.imaginativeworld.whynotimagecarousel.utils.setImage


open class ShoppingMainAdapter(
    private val context: Context,
    var products: MutableList<ProductEntity>,
    var limited: Boolean = false,
    private val onItemClickListener: ShoppingMainListener
) : RecyclerView.Adapter<ShoppingMainAdapter.ShoppingMainViewHolder>() {

    //we are defining this as a global variable coz this has to be accesible by the extension function that calculated the discounted price
//    var totalPrice: Float = 0f
//    var discountedPrice: Float = 0F

    //getting the current user id so that favorites can be added to firestore data
    val id: String =
        SharedPref(context).getData(Constants.USER_ID, Constants.STRING, "").toString()

    inner class ShoppingMainViewHolder(val binding: RvShoppingMainItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingMainViewHolder {
        val view =
            RvShoppingMainItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShoppingMainViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingMainViewHolder, position: Int) {

        val product = products[position]
        var selectedVariant: Int = 0
        var productInCart: Boolean = false
        //setting the variant details by default
//        val variantNames = mutableListOf<String>()
        var variantName = ""
//            "${product.variants[0].variantName} ${product.variants[0].variantType}"
//        var variantPrice: Float = product.variants[0].variantPrice.toFloat()

        holder.binding.apply {
            //setting the thumbnail
            ivProductThumbnail.loadImg(product.thumbnailUrl) {}
            tvProductName.text = product.name

            for (i in product.variants.indices) {
                variantName =
                    "${product.variants[i].variantName} ${product.variants[i].variantType}"
                if (product.variantInCart.contains(variantName)) {
                    productInCart = true
                    ivCart.setImageDrawable(ContextCompat.getDrawable(ivCart.context, R.drawable.ic_delete))
                    ivCart.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ivCart.context, R.color.matteRed))
                    selectedVariant = i
                    setShortVariantName(holder, product, selectedVariant)
                    checkVariantAvailability(holder, product.variants[selectedVariant])
                    setDiscountedValues(holder, product, selectedVariant)
                    break
                }
            }

            if (!productInCart) {
                selectedVariant = 0
                variantName =
                    "${product.variants[selectedVariant].variantName} ${product.variants[selectedVariant].variantType}"
                setShortVariantName(holder, product, 0)
                ivCart.setImageDrawable(ContextCompat.getDrawable(ivCart.context, R.drawable.ic_add))
                ivCart.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ivCart.context, R.color.green_base))
                checkVariantAvailability(holder, product.variants[selectedVariant])
                setDiscountedValues(holder, product, 0)
            }

            if (product.productType == SUBSCRIPTION) {
                ivCart.setImageDrawable(ContextCompat.getDrawable(ivCart.context, R.drawable.ic_visible))
                ivCart.imageTintList =  ColorStateList.valueOf(Color.WHITE)
//                ivCart.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ivCart.context, R.color.green_base))
            }

//            CoroutineScope(Dispatchers.IO).launch {
//                val bitmap = Glide
//                    .with(context)
//                    .asBitmap()
//                    .load(product.thumbnailUrl)
//                    .submit()
//                    .get()
//                withContext(Dispatchers.Main) {
//                    Palette.from(bitmap).generate { palette: Palette? ->
////                        val vibrant = palette!!.getVibrantColor(0x000000) // <=== color you want
//                        val vibrantLight = palette!!.getLightVibrantColor(Color.WHITE)
//                        val swatch = palette.lightVibrantSwatch
////                val vibrantDark = palette!!.getDarkVibrantColor(0x000000)
////                val muted = palette!!.getMutedColor(0x000000)
////                val mutedLight = palette!!.getLightMutedColor(0x000000)
////                val mutedDark = palette!!.getDarkMutedColor(0x000000)
//                        val textColor = ContextCompat.getColor(context, R.color.gray700)
//                        body.backgroundTintList = ColorStateList.valueOf(vibrantLight)
//                        clProductItem.backgroundTintList = ColorStateList.valueOf(vibrantLight)
//                        tvProductName.setTextColor(swatch?.bodyTextColor ?: textColor)
//                        tvProductVariant.setTextColor(swatch?.titleTextColor ?: textColor)
//                        tvPrice.setTextColor(swatch?.bodyTextColor ?: textColor)
//                        tvRs.setTextColor(swatch?.bodyTextColor ?: textColor)
//                        tvDiscount.setTextColor(swatch?.titleTextColor ?: textColor)
//                    }
//
//                }
//            }
//            val textColor = ContextCompat.getColor(context, R.color.matteRed)
//
//            tvPrice.setTextColor(textColor)
//            tvRs.setTextColor(textColor)
//            tvDiscount.setTextColor(ContextCompat.getColor(context, R.color.green_base))

            //details view


            //setting the favorites icon for the products
//            if (product.favorite) {
//                ivFavourite.setImageResource(R.drawable.ic_favorite_filled)
//                ivFavourite.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.matteRed))
//
//            } else {
//                ivFavourite.setImageResource(R.drawable.ic_favorite_outline)
//                ivFavourite.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green_base))
//            }

            //create a mutable list of variant names for the spinner
//            for (i in 0 until product.variants.size) {
//                val variant = "${product.variants[i].variantName} ${product.variants[i].variantType}"
//                if (product.variantInCart.contains(variant)) {
//                    selectedVariant = i
//                }
//                variantNames.add(variant)
//            }

            //setting the adapter for the spinner with the mutable list of variant names
//            val adapter = ArrayAdapter(
//                spProductVariant.popupContext,
//                R.layout.support_simple_spinner_dropdown_item,
//                variantNames
//            )
//            spProductVariant.adapter = adapter
//            spProductVariant.setSelection(selectedVariant)
//
//            //click listener for the spinner item selection
//            spProductVariant.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(
//                    parent: AdapterView<*>?,
//                    view: View?,
//                    variantposition: Int,
//                    id: Long
//                ) {
//                    selectedVariant = variantposition
//                    //setting the selected variant price and applying the discount for that variant
//                    val variant = product.variants[selectedVariant]
//
//                    variantName =
//                        "${variant.variantName} ${variant.variantType}"
//                    variantPrice = variant.variantPrice.toFloat()
//
//                    //setting the original price without discount
////                    tvDiscount.setTextAnimation("Rs: $variantPrice")
////                    tvDiscount.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
//                    setDiscountedValues(holder, product, variantposition)
//                    setCartItems(product, variantName, selectedVariant, holder)
//                    checkVariantAvailability(holder, variant)
//                }
//
//                override fun onNothingSelected(parent: AdapterView<*>?) {
////                    tvDiscount.setTextAnimation("Rs: $variantPrice")
////                    tvDiscount.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
//            checkVariantAvailability(holder, product.variants[selectedVariant])
//            setDiscountedValues(holder, product, 0)
//                    setCartItems(product, variantName, holder = holder)
//                }
//            }
//
//            //favorites click listener
//            ivFavourite.setOnClickListener {
//                ivFavourite.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
//                product.favorite = !product.favorite
//                onItemClickListener.updateFavorites(id, product, position)
//            }

//            tvAddItem.setOnClickListener {
//                val maxOrderQuantity = product.variants[selectedVariant].inventory
//                if (limited) {
//                    selectedVariant = product.defaultVariant
//                }
//
//                //checking if it is limited item. If yes, then item will not be added
//                with(tvAddItem) {
//                    when(text) {
//                        "View" -> {
//                            onItemClickListener.navigateToProduct(product, ivProductThumbnail, position)
//                        }
//                        "Add" -> {
//                                onItemClickListener.upsertCartItem(
//                                    product,
//                                    position,
//                                    variantName,
//                                    1,
//                                    totalPrice,
//                                    variantPrice,
//                                    selectedVariant,
//                                    maxOrderQuantity
//                                )
//                        }
//                        else -> {
//                            onItemClickListener.deleteCartItemFromShoppingMain(product, variantName, position)
//                        }
//                    }
//                }
//            }

            ivCart.setOnClickListener {
                if (limited) {
                    selectedVariant = product.defaultVariant
                }

                val maxOrderQuantity = product.variants[selectedVariant].inventory

                when {
                    productInCart -> onItemClickListener.deleteCartItemFromShoppingMain(product, variantName, position)
                    product.productType == SUBSCRIPTION -> onItemClickListener.navigateToProduct(product, ivProductThumbnail, position)
                    else -> {
                        if (product.variants[0].status == OUT_OF_STOCK) {
                            Toast.makeText(context, "Product Out of Stock", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val discountedPrice = if (product.variants[0].discountPrice == 0.0) {
                            product.variants[0].variantPrice.toFloat()
                        } else {
                            product.variants[0].discountPrice.toFloat()
                        }
                        onItemClickListener.upsertCartItem(
                            product,
                            position,
                            variantName,
                            1,
                            discountedPrice,
                            product.variants[0].variantPrice.toFloat(),
                            selectedVariant,
                            maxOrderQuantity
                        )
                    }

                }
            }

            ivProductThumbnail.setOnClickListener {
                onItemClickListener.navigateToProduct(product, ivProductThumbnail, position)
            }

            body.setOnClickListener {
                onItemClickListener.navigateToProduct(product, ivProductThumbnail, position)
            }

        }
    }

    private fun setShortVariantName(holder: ShoppingMainAdapter.ShoppingMainViewHolder,product: ProductEntity,  position: Int) {
        val shortName =
            "${product.variants[position].variantName}${getVariantType(product.variants[position].variantType)}"
        holder.binding.tvProductVariant.text = if (product.variants.size > 1) {
            "$shortName (+${product.variants.size - 1} more)"
        } else {
            shortName
        }
    }

    private fun getVariantType(variantType: String): String {
        return when(variantType) {
            "Kilogram" -> " Kg"
            "Gram" -> " g"
            "Liter" -> " l"
            "Unit" -> " Unit"
            else -> " ml"
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
                tvDiscountAmt.setTextAnimation(
                    "${getDiscountPercent(currentVariant.variantPrice.toFloat(), currentVariant.discountPrice.toFloat()).toInt()}% Off",
                    200
                )

//                discountedPrice = currentVariant.discountPrice.toFloat()
//                totalPrice = currentVariant.discountPrice.toFloat()
//                tvPrice.setTextAnimation(totalPrice.toString(), 200)
                tvPrice.setTextAnimation(currentVariant.discountPrice.toString(), 200)
                tvDiscount.setTextAnimation("Rs: ${currentVariant.variantPrice}", 200)
                tvDiscount.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
//                clDiscountLayout.fadInAnimation(200)
            } else {
//                discountedPrice = currentVariant.variantPrice.toFloat()
//                totalPrice = discountedPrice
//                tvPrice.setTextAnimation(totalPrice.toString())
                tvPrice.setTextAnimation(currentVariant.variantPrice.toString())
                tvDiscountAmt.fadOutAnimation(200)
//                clDiscountLayout.fadOutAnimation(200)
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
//                    tvAddItem.isEnabled = true
                }
                Constants.OUT_OF_STOCK -> {
                    ivCountBg.visibility = View.VISIBLE
                    tvItemCount.visibility = View.VISIBLE
                    tvItemCount.setTextAnimation("Out of Stock")
//                    tvAddItem.isEnabled = false

                }
                Constants.NO_LIMIT -> {
                    ivCountBg.visibility = View.GONE
                    tvItemCount.visibility = View.GONE
                    tvItemCount.setTextAnimation("")
//                    tvAddItem.isEnabled = true
                }
            }
        }
    }

//    private fun setCartItems(
//        product: ProductEntity,
//        variantName: String,
//        selectedVariant: Int = 0,
//        holder: ShoppingMainAdapter.ShoppingMainViewHolder
//    ) {
//        when {
//            product.productType == Constants.SUBSCRIPTION ->
//                with(holder.binding.tvAddItem) {
//                    visibility = View.VISIBLE
//                    text = "View"
//                    backgroundTintList =
//                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green_base))
//                    setBackgroundResource(R.drawable.shape_round_rectangle_8)
//            }
//            product.variants[selectedVariant].status == Constants.OUT_OF_STOCK -> {
//                holder.binding.tvAddItem.visibility = View.INVISIBLE
//            }
//            else -> {
//                holder.binding.tvAddItem.apply {
//                    visibility = View.VISIBLE
//                    if (product.variantInCart.contains(variantName)) {
//                        text = "Remove"
//                        backgroundTintList =
//                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.matteRed))
//                        setBackgroundResource(R.drawable.shape_round_rectangle_8)
//                    } else {
//                        text = "Add"
//                        backgroundTintList =
//                            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green_base))
//                        setBackgroundResource(R.drawable.shape_round_rectangle_8)
//                    }
//                }
//            }
//        }
//    }

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
        fun navigateToProduct(product: ProductEntity, thumbnail: ShapeableImageView, position: Int)
        fun upsertCartItem(product: ProductEntity, position: Int , variant: String, count: Int, price: Float, originalPrice: Float, variantIndex: Int, maxOrderQuantity: Int)
        fun deleteCartItemFromShoppingMain(product: ProductEntity, variantName: String, position: Int)
    }
}