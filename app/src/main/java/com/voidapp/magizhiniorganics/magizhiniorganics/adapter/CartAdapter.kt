package com.voidapp.magizhiniorganics.magizhiniorganics.adapter


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ShoppingMainAdapter.ShoppingMainAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.dish.DishViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder.QuickOrderViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.diffUtils.CartDiffUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadInAnimation
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadOutAnimation
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.setTextAnimation

class CartAdapter(
    val context: Context,
    var cartItems: MutableList<CartEntity>,
    val viewModel: ViewModel
): RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //small discount layout in the thumbnail
        val cart: ConstraintLayout = itemView.findViewById(R.id.clCart)
        val productThumbNail: ShapeableImageView = itemView.findViewById(R.id.ivProductThumbnail)

        //details views
        val productName: TextView = itemView.findViewById<TextView>(R.id.tvProductName)
        val price: TextView = itemView.findViewById<TextView>(R.id.tvDiscount)
        val discountedAmount: TextView = itemView.findViewById<TextView>(R.id.tvPrice)
        val variantName: TextView = itemView.findViewById<TextView>(R.id.tvProductVariant)
        val discountBanner: TextView = itemView.findViewById(R.id.tvDiscountAmt)

        //val addItem = itemView.findViewById<Chip>(R.id.cpAddItem)
        val add: ShapeableImageView = itemView.findViewById(R.id.ivAdd)
        val remove: ShapeableImageView = itemView.findViewById(R.id.ivRemove)
        val delete: ShapeableImageView = itemView.findViewById(R.id.ivDelete)
        val orderCount:TextView = itemView.findViewById(R.id.tvOrderCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.rv_cart_items, parent, false)
        return CartViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        //setting up the basic variables
        val cartItem = cartItems[position]
//        val product = cartItems[position].product
        val id = cartItem.id

        //loading the thumbnail
        holder.productThumbNail.loadImg(cartItem.thumbnailUrl)  {}
        holder.productThumbNail.clipToOutline = true

        holder.productName.text = cartItem.productName
        holder.orderCount.text = cartItem.quantity.toString()
        holder.variantName.text = "Quantity: ${getVariantType(cartItem.variant)}"
        setDiscountedValues(holder, cartItem.price, cartItem.originalPrice)
//        val variantArray = arrayListOf<String>()
//        var price = 0F

        //we are picking only the variant added to cart by checking if the variant name is contained in
        //the variant array list in tha constructor and setting only that variant name for the adapter
//        for (i in product.variants) {
//            val name = "${i.variantName} ${i.variantType}"
//            if (variant.contains(name)) {
//                variantArray.add(name)
//                price = i.variantPrice.toFloat()
//            }
//        }

//        variantArray.add(cartItem.variant)
//        price = cartItem.price

//        holder.discountedAmount.text = "Rs. $price"
//        if (cartItem.originalPrice != cartItem.price) {
//            holder.price.visibility = View.VISIBLE
//            holder.price.text = "Rs. ${cartItem.originalPrice}"
//            holder.price.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
//        } else {
//            holder.price.visibility = View.GONE
//        }

        holder.productThumbNail.setOnClickListener {
            when(viewModel) {
//                is ShoppingMainViewModel -> viewModel.moveToProductDetails(cartItem.productId, cartItem.productName)
                //can implement later for checkout and product page
            }
        }

        val limit = if (cartItem.maxOrderQuantity < 11) {
            cartItem.maxOrderQuantity
        } else {
            10
        }

        holder.add.setOnClickListener {
//            holder.add.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
            val count = holder.orderCount.text.toString().toInt()
            if (count < limit) {
                when(viewModel) {
                    is ShoppingMainViewModel -> viewModel.updateCartItem(id, count+1)
                    is ProductViewModel -> viewModel.updateCartItem(id, count+1, position, "add")
                    is CheckoutViewModel -> viewModel.updateCartItem(id, count+1, position)
                    is DishViewModel -> viewModel.updateCartItem(position, count+1)
                    is QuickOrderViewModel -> viewModel.updateCartItem(position, count+1)
                }
            }
        }
        holder.remove.setOnClickListener {
//            holder.remove.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
            val count = holder.orderCount.text.toString().toInt()
            if (count > 1) {
                when(viewModel) {
                    is ShoppingMainViewModel -> viewModel.updateCartItem(id, count-1)
                    is ProductViewModel -> viewModel.updateCartItem(id, count-1, position, "remove")
                    is CheckoutViewModel -> viewModel.updateCartItem(id, count-1, position)
                    is DishViewModel -> viewModel.updateCartItem(position, count-1)
                    is QuickOrderViewModel -> viewModel.updateCartItem(position, count-1)
                }
            }
        }
        holder.delete.setOnClickListener {
//            holder.delete.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
            when(viewModel) {
                is ShoppingMainViewModel -> viewModel.deleteCartItem(id, cartItem.productId, cartItem.variant)
                is ProductViewModel -> viewModel.deleteCartItem(id, cartItem.productId, cartItem.variant, position)
                is CheckoutViewModel -> viewModel.deleteCartItem(id, cartItem.productId, cartItem.variant, position)
                is DishViewModel -> viewModel.deleteItemFromCart(position)
                is QuickOrderViewModel -> viewModel.deleteItemFromCart(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return cartItems.size
    }

    private fun getVariantType(variantName: String): String {
        val variantArray = variantName.split(" ")
        return when(variantArray[1]) {
            "Kilogram" -> "${variantArray[0]} Kg"
            "Gram" -> "${variantArray[0]} g"
            "Liter" -> "${variantArray[0]} l"
            "Unit" -> "${variantArray[0]} Unit"
            else -> "${variantArray[0]} ml"
        }
    }

    private fun setDiscountedValues(
        holder: CartViewHolder,
        discountPrice: Float,
        originalPrice: Float
    ) {
        holder.apply {
            //setting up the product discount info
            if (discountPrice != originalPrice) {
                discountBanner.setTextAnimation(
                    "${getDiscountPercent(originalPrice, discountPrice).toInt()}% Off",
                    200
                )
                discountedAmount.text = "Rs. $discountPrice"
                price.visibility = View.VISIBLE
                price.text = "Rs. $originalPrice"
                price.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                discountBanner.fadOutAnimation()
                discountedAmount.text = "Rs. $originalPrice"
                price.visibility = View.GONE
            }
        }
    }

    private fun getDiscountPercent(price: Float, discountPrice: Float): Float
            = ((price-discountPrice)/price)*100

    fun deleteCartItem(position: Int) {
        val items = cartItems.map { it.copy() } as MutableList
        items.removeAt(position)
        setCartData(items)
        this.notifyDataSetChanged()
    }

    fun updateItemsCount(position: Int, count: Int) {
        val items = cartItems.map { it.copy() }  as MutableList
        items[position].quantity = count
        setCartData(items)
    }

    fun addCartItem(cartEntity: CartEntity) {
        val items = cartItems.map { it.copy() } as MutableList
        items.add(cartEntity.copy())
        setCartData(items)
        this.notifyDataSetChanged()
    }

    fun emptyCart() {
        setCartData(mutableListOf())
    }

    fun setCartData(newList: MutableList<CartEntity>) {
        val diffUtil = CartDiffUtil(cartItems, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        cartItems = newList
        diffResult.dispatchUpdatesTo(this)
    }

}