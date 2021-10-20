package com.voidapp.magizhiniorganics.magizhiniorganics.adapterimport android.content.Contextimport android.content.res.ColorStateListimport android.graphics.Paintimport android.view.LayoutInflaterimport android.view.Viewimport android.view.ViewGroupimport android.view.animation.AnimationUtilsimport android.widget.*import androidx.core.content.ContextCompatimport androidx.lifecycle.ViewModelimport androidx.recyclerview.widget.RecyclerViewimport com.google.android.material.imageview.ShapeableImageViewimport com.voidapp.magizhiniorganics.magizhiniorganics.Rimport com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntityimport com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.CheckoutViewModelimport com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryViewModelimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constantsimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.GlideLoaderimport com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPrefclass OrderItemsAdapter(    private val context: Context,    var cartItems: List<CartEntity>,    private val viewModel: ViewModel,    var favorites: ArrayList<String> = ArrayList(),    private val content: String = "") : RecyclerView.Adapter<OrderItemsAdapter.OrderItemsViewHolder>() {    //getting the current user id so that favorites can be added to firestore data    val userId: String = SharedPref(context).getData(Constants.USER_ID, Constants.STRING, "").toString()    inner class OrderItemsViewHolder(private val itemView: View) : RecyclerView.ViewHolder(itemView) {        //details views        val productThumbNail: ShapeableImageView = itemView.findViewById(R.id.ivProductThumbnail)        val orderCount: TextView = itemView.findViewById(R.id.tvOrderItemCount)        val productName: TextView = itemView.findViewById<TextView>(R.id.tvProductName)        val price: TextView = itemView.findViewById<TextView>(R.id.tvDiscount)        val discountedAmount: TextView = itemView.findViewById<TextView>(R.id.tvPrice)        val variants: Spinner = itemView.findViewById<Spinner>(R.id.spProductVariant)        val addBtn: TextView = itemView.findViewById(R.id.tvAddItem)        val ivFavorites: ShapeableImageView = itemView.findViewById(R.id.ivFavourite)        val review: ShapeableImageView = itemView.findViewById(R.id.ivReview)    }    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderItemsViewHolder {        val view =            LayoutInflater.from(parent.context).inflate(R.layout.rv_list_item, parent, false)        return OrderItemsViewHolder(view)    }    override fun onBindViewHolder(holder: OrderItemsViewHolder, position: Int) {        //setting up the basic variables        val cartItem = cartItems[position]        val id = cartItem.id        //loading the thumbnail        GlideLoader().loadUserPicture(context, cartItem.thumbnailUrl, holder.productThumbNail)        holder.productThumbNail.clipToOutline = true        holder.addBtn.visibility = View.INVISIBLE        holder.productName.text = cartItem.productName        holder.orderCount.text = "Purchased: ${cartItem.quantity}"        val variantArray = arrayListOf<String>()        var price = 0F        variantArray.add(cartItem.variant)        price = cartItem.price        val adapter =            ArrayAdapter(holder.variants.context, R.layout.support_simple_spinner_dropdown_item, variantArray)        holder.variants.adapter = adapter        holder.discountedAmount.text = "Rs. $price"        holder.price.text = "Rs. ${cartItem.originalPrice}"        holder.price.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG        //setting the favorties icon for the products        if (favorites.contains(cartItem.productId)) {            holder.ivFavorites.setImageResource(R.drawable.ic_favorite_filled)        } else {            holder.ivFavorites.setImageResource(R.drawable.ic_favorite_outline)        }        if (content == "checkout") {            with(holder) {                review.visibility = View.INVISIBLE                ivFavorites.visibility = View.INVISIBLE                addBtn.visibility = View.VISIBLE                addBtn.text = "Remove"                addBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.matteRed))                orderCount.visibility = View.GONE            }        }        //favorites click listener        holder.ivFavorites.setOnClickListener {            holder.ivFavorites.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))            when(viewModel) {                is PurchaseHistoryViewModel -> {                    for (i in favorites.indices) {                        if (cartItem.productId == favorites[i]) {                            holder.ivFavorites.setImageResource(R.drawable.ic_favorite_outline)                            viewModel.updateFavorites(userId, cartItem.productId, false)                        } else {                            holder.ivFavorites.setImageResource(R.drawable.ic_favorite_filled)                            viewModel.updateFavorites(userId, cartItem.productId, true)                        }                    }                }            }        }        holder.productThumbNail.setOnClickListener {            when (viewModel) {                is PurchaseHistoryViewModel -> viewModel.moveToProductDetails(cartItem.productId, cartItem.productName)            }        }        holder.review.setOnClickListener {            holder.review.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))            when (viewModel) {                is PurchaseHistoryViewModel -> viewModel.moveToProductDetails(cartItem.productId, cartItem.productName)            }        }        holder.addBtn.setOnClickListener {            holder.addBtn.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))            when (viewModel) {                is CheckoutViewModel -> {                    viewModel.deleteCartItem(id ,cartItem.productId, cartItem.variant)                    holder.addBtn.text = "Removed"                    holder.addBtn.isEnabled = false                }            }        }    }    override fun getItemCount(): Int {        return cartItems.size    }}