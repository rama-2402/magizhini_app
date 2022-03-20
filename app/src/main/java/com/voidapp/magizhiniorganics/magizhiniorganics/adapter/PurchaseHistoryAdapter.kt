package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvOrderItemsBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants

class PurchaseHistoryAdapter(
    private val context: Context,
    var orders: MutableList<OrderEntity>,
    private val onItemClickListener: PurchaseHistoryListener
): RecyclerView.Adapter<PurchaseHistoryAdapter.PurchaseHistoryViewHolder>() {

    inner class PurchaseHistoryViewHolder(val binding: RvOrderItemsBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseHistoryViewHolder {
        val view = RvOrderItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PurchaseHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchaseHistoryViewHolder, position: Int) {

        val order = orders[position]

        with(holder.binding) {
            tvOrderId.text = order.orderId
            tvOrderStatus.isActivated = true
            tvDate.text = "${order.purchaseDate} - ${order.paymentMethod} - Rs: ${order.price}"
            var itemCount = 0
            order.cart.forEach {
                itemCount += it.quantity
            }
            tvDeliveryPreference.text = order.deliveryPreference
            tvDeliveryNote.text = order.deliveryNote
            ivCart.badgeValue = itemCount
            tvAddress.text = "${order.address.userId}, ${order.address.addressLineOne}, ${order.address.addressLineTwo}, ${order.address.LocationCode}, ${order.address.city}"

            when(order.orderStatus) {
                Constants.CANCELLED -> {
                    btnSend.background = ContextCompat.getDrawable(context, R.drawable.bg_order_history_failed)
                    tvCancel.text = "CANCELLED"
                    tvOrderStatus.text = "Order Cancelled"
                }
                Constants.SUCCESS -> {
                    btnSend.background = ContextCompat.getDrawable(context, R.drawable.bg_order_history_invoice)
                    tvCancel.text = "INVOICE"
                    tvOrderStatus.text = "Products Delivered"
                }
                Constants.FAILED -> {
                    btnSend.background = ContextCompat.getDrawable(context, R.drawable.bg_order_history_failed)
                    tvCancel.text = "FAILED"
                    tvOrderStatus.text = "Failed to Deliver"
                }
                Constants.PENDING -> {
                    btnSend.background = ContextCompat.getDrawable(context, R.drawable.bg_order_history_cancel)
                    tvCancel.text = "CANCEL"
                    tvOrderStatus.text = "Getting ready to be packed"
                }
                else -> {
                    btnSend.background = ContextCompat.getDrawable(context, R.drawable.bg_order_history_cancel)
                    tvCancel.text = "CANCEL"
                    tvOrderStatus.text = order.orderStatus
                }
            }

            ivCart.setOnClickListener {
                if (tvOrderId.isFocused) {
                    tvOrderId.clearFocus()
                }
                ivCart.startAnimation(AnimationUtils.loadAnimation(context, R.anim.bounce))
                onItemClickListener.showCart(order.cart)
            }

            btnSend.setOnClickListener {
                if (tvOrderId.isFocused) {
                    tvOrderId.clearFocus()
                }
                cancelOrder(position, order.orderStatus, order.price)
            }

            itemView.setOnClickListener {
                if (tvOrderId.isFocused) {
                    tvOrderId.clearFocus()
                    return@setOnClickListener
                }
                onItemClickListener.showCart(order.cart)
            }

        }
    }

     override fun getItemCount(): Int {
        return orders.size
    }

    private fun cancelOrder(position: Int, status: String, price: Float) {
        when (status) {
            Constants.SUCCESS -> onItemClickListener.generateInvoice(position)
            Constants.CANCELLED -> onItemClickListener.openExitSheet(
                "This Order has been cancelled. If you have already paid. Don't Worry. your order amount Rs: $price will be refunded in 3 to 5 Business Days. If not done already please click Contact Support to contact our Customer Support Team"
            )
            Constants.FAILED -> onItemClickListener.openExitSheet(
                "The Order Delivery is Failed. If you have already paid. Don't Worry. your order amount Rs: $price will be refunded in 3 to 5 Business Days. If not done already please click Contact Support to contact our Customer Support Team"
            )
            else -> onItemClickListener.cancelOrder(position)

        }
    }

    interface PurchaseHistoryListener {
        fun showCart(cart: List<CartEntity>)
        fun cancelOrder(position: Int)
        fun generateInvoice(position: Int)
        fun openExitSheet(message: String)
    }
}
