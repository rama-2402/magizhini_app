package com.voidapp.magizhiniorganics.magizhiniorganics.adapter


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.diffUtils.CartDiffUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoader

class CartAdapter(
    val context: Context,
    var cartItems: List<CartEntity>,
    val viewModel: ViewModel
): RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //small discount layout in the thumbnail
        val cart: ConstraintLayout = itemView.findViewById(R.id.clCart)
        val productThumbNail: ShapeableImageView = itemView.findViewById(R.id.ivProductThumbnail)
        val discountType: TextView = itemView.findViewById<TextView>(R.id.tvDiscountType)

        //details views
        val productName: TextView = itemView.findViewById<TextView>(R.id.tvProductName)
        val price: TextView = itemView.findViewById<TextView>(R.id.tvDiscount)
        val discountedAmount: TextView = itemView.findViewById<TextView>(R.id.tvPrice)
        val variants: Spinner = itemView.findViewById<Spinner>(R.id.spProductVariant)

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
        GlideLoader().loadUserPicture(holder.productThumbNail.context, cartItem.thumbnailUrl, holder.productThumbNail)
        holder.productThumbNail.clipToOutline = true

        holder.productName.text = cartItem.productName
        holder.orderCount.text = cartItem.quantity.toString()

        val variantArray = arrayListOf<String>()
        var price = 0F

        //we are picking only the variant added to cart by checking if the variant name is contained in
        //the variant array list in tha constructor and setting only that variant name for the adapter
//        for (i in product.variants) {
//            val name = "${i.variantName} ${i.variantType}"
//            if (variant.contains(name)) {
//                variantArray.add(name)
//                price = i.variantPrice.toFloat()
//            }
//        }

        variantArray.add(cartItem.variant)
        price = cartItem.price
        val adapter =
            ArrayAdapter(holder.variants.context, R.layout.support_simple_spinner_dropdown_item, variantArray)
        holder.variants.adapter = adapter

        holder.discountedAmount.text = "Rs. $price"
        holder.price.text = "Rs. ${cartItem.originalPrice}"
        holder.price.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG

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
            holder.add.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
            val count = holder.orderCount.text.toString().toInt()
            if (count < limit) {
                when(viewModel) {
                    is ShoppingMainViewModel -> viewModel.updateCartItem(id, count+1)
                    is ProductViewModel -> viewModel.updateCartItem(id, count+1)
                    is CheckoutViewModel -> viewModel.updateCartItem(id, count+1)
                }
            }
        }
        holder.remove.setOnClickListener {
            holder.remove.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
            val count = holder.orderCount.text.toString().toInt()
            if (count > 1) {
                when(viewModel) {
                    is ShoppingMainViewModel -> viewModel.updateCartItem(id, count-1)
                    is ProductViewModel -> viewModel.updateCartItem(id, count-1)
                    is CheckoutViewModel -> viewModel.updateCartItem(id, count-1)
                }
            }
        }
        holder.delete.setOnClickListener {
            holder.remove.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
            when(viewModel) {
                is ShoppingMainViewModel -> viewModel.deleteCartItem(id, cartItem.productId, cartItem.variant)
                is ProductViewModel -> viewModel.deleteCartItem(id, cartItem.productId, cartItem.variant)
                is CheckoutViewModel -> viewModel.deleteCartItem(id, cartItem.productId, cartItem.variant)
            }
        }

    }

    override fun getItemCount(): Int {
        return cartItems.size
    }


    fun setCartData(newList: List<CartEntity>) {
        val diffUtil = CartDiffUtil(cartItems, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        cartItems = newList
        diffResult.dispatchUpdatesTo(this)
    }
}