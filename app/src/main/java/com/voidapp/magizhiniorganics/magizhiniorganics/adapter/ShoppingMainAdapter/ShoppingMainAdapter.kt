package com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ShoppingMainAdapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.ProductVariant
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.diffUtils.DiffUtils

open class ShoppingMainAdapter(
    private val context: Context,
    var products: List<ProductEntity>,
    var limited: Boolean = false,
    private val viewModel: ShoppingMainViewModel
) : RecyclerView.Adapter<ShoppingMainAdapter.ShoppingMainViewHolder>() {

    //we are defining this as a global variable coz this has to be accesible by the extension function that calculated the discounted price
    var discountedPrice: Float = 0F

    inner class ShoppingMainViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView) {
        //small discount layout in the thumbnail
        val productThumbNail: ShapeableImageView = itemView.findViewById(R.id.ivProductThumbnail)
        val discountLayout = itemView.findViewById<ConstraintLayout>(R.id.clDiscountLayout)
        val discountAmount = itemView.findViewById<TextView>(R.id.tvDiscountAmt)
        val discountType = itemView.findViewById<TextView>(R.id.tvDiscountType)
        val transparentBg = itemView.findViewById<View>(R.id.ivCountBg)
        val inStock = itemView.findViewById<TextView>(R.id.tvItemCount)

        //details views
        val productName = itemView.findViewById<TextView>(R.id.tvProductName)
        val price = itemView.findViewById<TextView>(R.id.tvDiscount)
        val discountedAmount = itemView.findViewById<TextView>(R.id.tvPrice)
        val variants = itemView.findViewById<Spinner>(R.id.spProductVariant)
        val addItem = itemView.findViewById<TextView>(R.id.tvAddItem)
        val favorites: ShapeableImageView = itemView.findViewById(R.id.ivFavourite)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingMainViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.rv_shopping_items, parent, false)
        return ShoppingMainViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingMainViewHolder, position: Int) {

        val product = products[position]
        val productId = product.id

        //setting the variant details by default
        val variantNames = mutableListOf<String>()
        var variantName = "${product.variants[0].variantName} ${product.variants[0].variantType}"

        var variantInCartPosition: Int = product.defaultVariant
        var maximumOrderQuantity: Int = product.variants[0].inventory
        var variantPrice: Float = product.variants[0].variantPrice.toFloat()
        checkVariantAvailability(holder, product.variants[0])

        //setting the thumbnail
        GlideLoader().loadUserPicture(context, product.thumbnailUrl, holder.productThumbNail)
//        holder.productThumbNail.clipToOutline = true


        //getting the current user id so that favorites can be added to firestore data
        val id: String =
            SharedPref(context).getData(Constants.USER_ID, Constants.STRING, "").toString()

        //details view
        holder.productName.text = product.name

        //if discount is available then we make the discount layout visible and set the discount amount and percentage
        if (product.discountAvailable) {
            holder.discountLayout.visibility = View.VISIBLE
            holder.price.visibility = View.VISIBLE
            setDiscountedValues(holder, product, variantPrice, 0)
        } else {
            holder.discountLayout.visibility = View.INVISIBLE
            holder.price.visibility = View.INVISIBLE
            holder.discountedAmount.text = variantPrice.toString()
        }

        //setting the favorties icon for the products
        if (product.favorite) {
            holder.favorites.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            holder.favorites.setImageResource(R.drawable.ic_favorite_outline)
        }

        //create a mutable list of variant names for the spinner
        for (i in 0 until product.variants.size) {
            val variant = "${product.variants[i].variantName} ${product.variants[i].variantType}"
            variantNames.add(variant)
        }

        //setting the adapter for the spinner with the mutable list of variant names
        val adapter = ArrayAdapter(
            holder.variants.popupContext,
            R.layout.support_simple_spinner_dropdown_item,
            variantNames
        )
        holder.variants.adapter = adapter

        setCartItems(product, variantName, holder)

        //click listener for the spinner item selection
        holder.variants.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                variantposition: Int,
                id: Long
            ) {
                //setting the selected variant price and applying the discount for that variant
                val variant = product.variants[variantposition]

                variantName =
                    "${product.variants[variantposition].variantName} ${product.variants[variantposition].variantType}"
                variantPrice = product.variants[variantposition].variantPrice.toFloat()
                maximumOrderQuantity = variant.inventory
                variantInCartPosition = variantposition

                //setting the original price without discount
                holder.price.text = "Rs: $variantPrice"
                holder.price.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG

                setCartItems(product, variantName, holder)

                if (!product.discountAvailable) {
                    discountedPrice = variantPrice.toString().toFloat()
                }
                setDiscountedValues(holder, product, variantPrice, variantposition)
                checkVariantAvailability(holder, variant)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                variantInCartPosition = product.defaultVariant
                variantPrice = product.variants[0].variantPrice.toFloat()
                maximumOrderQuantity = product.variants[0].inventory
                holder.price.text = "Rs: $variantPrice"
                holder.price.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
                if (!product.discountAvailable) {
                    discountedPrice = variantPrice.toString().toFloat()
                }
                setDiscountedValues(holder, product, variantPrice, 0)
            }
        }

        //favorites click listener
        holder.favorites.setOnClickListener {
            var removedItem: String = ""
            var addedItem: String = ""

            //if the favorites array list that we got contains the clicked product id then it will be removed and notificatio will
            //be sent to the view model with the current userID, position of the product so that itemchange can be notified only to that
            //particular product once the product is added or remove from the favorites arraylist in the profile and then
            // new array is added back to the adapter.

            if (product.favorite) {
                removedItem = product.id
                holder.favorites.setImageResource(R.drawable.ic_favorite_outline)
                product.favorite = false
            } else {
                addedItem = product.id
                holder.favorites.setImageResource(R.drawable.ic_favorite_filled)
                product.favorite = true
            }
            viewModel.upsertProduct(product)

            viewModel.updateFavorites(id, position, addedItem, removedItem)
        }

        holder.addItem.setOnClickListener {
//            //checking if it is limited item. If yes, then item will not be added

            var limitedCheck: Boolean = false
            if (limited) {
                variantInCartPosition = product.defaultVariant
            } else {
                when (product.variants[variantInCartPosition].status) {
                    Constants.LIMITED -> {
                    limitedCheck = true
                    Toast.makeText(
                        context,
                        "Check product availability in Limited Items filter",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                    Constants.OUT_OF_STOCK -> {
                        Toast.makeText(
                            context,
                            "Product Out of Stock",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            with(holder.addItem) {
                if (text == "Add") {
                    if (!limitedCheck) {
                        text = "Remove"
                        backgroundTintList =
                            ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    context,
                                    R.color.matteRed
                                )
                            )
                        setBackgroundResource(R.drawable.shape_round_rectangle_8)
                        Toast.makeText(context, "Added to Cart", Toast.LENGTH_SHORT).show()
//                    product.variantInCart.add(variantName)
//                    viewModel.upsertProduct(product)
                        viewModel.upsertCartItem(
                            productId,
                            product.name,
                            product.thumbnailUrl,
                            variantName,
                            1,
                            holder.discountedAmount.text.toString().toFloat(),
                            variantPrice,
                            maximumOrderQuantity,
                            variantInCartPosition
                        )
                    }
                } else {
                    text = "Add"
                    backgroundTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green_base))
                    setBackgroundResource(R.drawable.shape_round_rectangle_8)
                    Toast.makeText(context, "Removed from Cart", Toast.LENGTH_SHORT).show()
//                    product.variantInCart.remove(variantName)
//                    viewModel.upsertProduct(product)
                    viewModel.deleteCartItemFromShoppingMain(productId, variantName)
                }
            }
        }

        holder.productThumbNail.setOnClickListener {
            viewModel.moveToProductDetails(productId, product.name)
        }
    }

    private fun setDiscountedValues(
        holder: ShoppingMainAdapter.ShoppingMainViewHolder,
        product: ProductEntity,
        variantPrice: Float,
        position: Int
    ) {
        if (product.discountAvailable) {
            when (discountAvailability(product, position)) {
                "product" -> {
                    holder.discountAmount.text = product.discountAmt.toString()
                    //setting up the product discount info
                    if (product.discountType == "Percentage") {
                        holder.discountType.text = "%"
                        discountedPrice = calculateDiscountedAmount(
                            "Percentage",
                            product.discountAmt,
                            product.variants[position].variantPrice
                        ).toFloat()
                        holder.discountedAmount.text =
                            discountedPrice.toString()
                    } else {
                        holder.discountType.text = "Rs"
                        discountedPrice = calculateDiscountedAmount(
                            "rupees",
                            product.discountAmt,
                            product.variants[position].variantPrice
                        ).toFloat()
                        holder.discountedAmount.text =
                            discountedPrice.toString()

                    }
                }
                "variant" -> {
                    holder.discountAmount.text =
                        product.variants[position].discountPercent.toString()
                    //setting up the product discount info
                    if (product.variants[position].discountType == "Percentage") {
                        holder.discountType.text = "%"
                        discountedPrice = calculateDiscountedAmount(
                            "Percentage",
                            product.variants[position].discountPercent,
                            product.variants[position].variantPrice
                        ).toFloat()
                        holder.discountedAmount.text =
                            discountedPrice.toString()
                    } else {
                        holder.discountType.text = "Rs"
                        discountedPrice = calculateDiscountedAmount(
                            "rupees",
                            product.variants[position].discountPercent,
                            product.variants[position].variantPrice
                        ).toFloat()
                        holder.discountedAmount.text = discountedPrice.toString()
                    }
                }
            }
        } else {
            holder.discountedAmount.text = variantPrice.toString()
            discountedPrice = variantPrice
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

    private fun checkVariantAvailability(
        holder: ShoppingMainAdapter.ShoppingMainViewHolder,
        variant: ProductVariant
    ) {
        when (variant.status) {
            Constants.LIMITED -> {
                holder.transparentBg.visibility = View.VISIBLE
                holder.inStock.visibility = View.VISIBLE
                holder.inStock.text = "Available: ${variant.inventory}"
                holder.addItem.isEnabled = true
            }
            Constants.OUT_OF_STOCK -> {
                holder.transparentBg.visibility = View.VISIBLE
                holder.inStock.visibility = View.VISIBLE
                holder.inStock.text = "Out of Stock"
                holder.addItem.isEnabled = false

            }
            Constants.NO_LIMIT -> {
                holder.transparentBg.visibility = View.GONE
                holder.inStock.visibility = View.GONE
                holder.inStock.text = ""
                holder.addItem.isEnabled = true
            }
        }
    }

    private fun setCartItems(
        product: ProductEntity,
        variantName: String,
        holder: ShoppingMainAdapter.ShoppingMainViewHolder
    ) {
        if (product.variantInCart.contains(variantName)) {
            with(holder.addItem) {
                text = "Remove"
                backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.matteRed))
                setBackgroundResource(R.drawable.shape_round_rectangle_8)
            }
        } else {
            with(holder.addItem) {
                text = "Add"
                backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green_base))
                setBackgroundResource(R.drawable.shape_round_rectangle_8)
            }
        }
    }

    private fun discountAvailability(productEntity: ProductEntity, position: Int): String {
        return if (productEntity.variants[position].variantDiscount) {
            "variant"
        } else
            "product"
    }

    override fun getItemCount(): Int {
        return products.size
    }

    fun setData(newList: List<ProductEntity>) {
        val diffUtil = DiffUtils(products, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        products = newList
        diffResult.dispatchUpdatesTo(this)
    }

}