package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants

class PurchaseHistoryAdapter(
    private val context: Context,
    var orders: MutableList<OrderEntity>,
    private val onItemClickListener: PurchaseHistoryListener
): RecyclerView.Adapter<PurchaseHistoryAdapter.PurchaseHistoryViewHolder>() {

    inner class PurchaseHistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val orderId: TextView = itemView.findViewById(R.id.tvOrderId)
        val orderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        val purchaseDate: TextView = itemView.findViewById(R.id.tvPurchaseDate)
        val cartPrice: TextView = itemView.findViewById(R.id.tvCartPrice)
        val paymentStatus: TextView = itemView.findViewById(R.id.tvPaymentStatus)
        val showCart: ShapeableImageView = itemView.findViewById(R.id.ivShowCart)
        val liveStatus: TextView = itemView.findViewById(R.id.tvStatusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_order_items, parent, false)
        return PurchaseHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchaseHistoryViewHolder, position: Int) {

        val order = orders[position]

        with(holder) {
            orderId.text = order.orderId

            purchaseDate.text = order.purchaseDate
            cartPrice.text = "Rs. ${order.price}"

            when(order.isPaymentDone) {
                true -> {
                    paymentStatus.text = "(paid - ${order.paymentMethod})"
                    holder.paymentStatus.setTextColor(ContextCompat.getColor(context, R.color.matteGreen))
                }
                false -> {
                    paymentStatus.text = "(${order.paymentMethod})"
                    holder.paymentStatus.setTextColor(ContextCompat.getColor(context, R.color.matteGreen))
                }
            }

            when(order.orderStatus) {
                Constants.CANCELLED -> {
                    showCart.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_cancelled_order))
                    showCart.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.matteRed))
                    orderStatus.text = "Cancelled"
                    orderStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
                    orderStatus.elevation = 0f
                    orderStatus.setTextColor(ContextCompat.getColor(context, R.color.matteRed))
                    liveStatus.text = "Order Cancelled"

                }
                Constants.SUCCESS -> {
                    showCart.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check))
                    showCart.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.matteGreen))
                    orderStatus.text = "Invoice"
                    orderStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green_base))
                    orderStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
                    liveStatus.text = "Products Delivered"
                }
                Constants.FAILED -> {
                    showCart.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_cancelled_order))
                    showCart.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.matteRed))
                    orderStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
                    orderStatus.elevation = 0f
                    orderStatus.text = "Delivery \n Failed"
                    orderStatus.setTextColor(ContextCompat.getColor(context, R.color.matteRed))
                    liveStatus.text = "Failed to Deliver"
                }
                Constants.PENDING -> {
                    showCart.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.carbon_delivery))
                    orderStatus.text = "Cancel"
                    liveStatus.text = "Getting ready to be packed"
                }
                else -> {
                    showCart.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.carbon_delivery))
                    orderStatus.text = "Cancel"
                    liveStatus.text = order.orderStatus
                }
            }

            showCart.setOnClickListener {
                showCart.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
                cancelOrder(position, order.orderStatus)
            }

            orderStatus.setOnClickListener {
                orderStatus.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
                cancelOrder(position, order.orderStatus)
            }

            itemView.setOnClickListener {
                onItemClickListener.showCart(order.cart)
            }

        }
    }

     override fun getItemCount(): Int {
        return orders.size
    }

    private fun cancelOrder(position: Int, status: String) {
        when (status) {
            Constants.SUCCESS -> {
                onItemClickListener.generateInvoice(position)
            }
            else -> {
                onItemClickListener.cancelOrder(position)
            }
        }
    }

    interface PurchaseHistoryListener {
        fun showCart(cart: List<CartEntity>)
        fun cancelOrder(position: Int)
        fun generateInvoice(position: Int)
    }
}
