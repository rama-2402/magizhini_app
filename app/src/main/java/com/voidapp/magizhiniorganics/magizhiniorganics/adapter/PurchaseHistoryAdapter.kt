package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryViewModel

class PurchaseHistoryAdapter(
    val context: Context,
    var orders: List<OrderEntity>,
    val viewModel: ViewModel
): RecyclerView.Adapter<PurchaseHistoryAdapter.PurchaseHistoryViewHolder>() {

    inner class PurchaseHistoryViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val orderId: TextView = itemView.findViewById(R.id.tvOrderId)
        val orderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        val purchaseDate: TextView = itemView.findViewById(R.id.tvPurchaseDate)
        val cartPrice: TextView = itemView.findViewById(R.id.tvCartPrice)
        val paymentStatus: TextView = itemView.findViewById(R.id.tvPaymentStatus)
        val showCart: ShapeableImageView = itemView.findViewById(R.id.ivShowCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_order_items, parent, false)
        return PurchaseHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchaseHistoryViewHolder, position: Int) {
        val order = orders[position]

        with(holder) {
            orderId.text = order.orderId
            orderStatus.text = order.orderStatus
            purchaseDate.text = order.purchaseDate
            cartPrice.text = "Rs. ${order.price}"

            when(order.isPaymentDone) {
                true -> {
                    paymentStatus.text = "(paid - ${order.paymentMethod})"
                    holder.paymentStatus.setTextColor(ContextCompat.getColor(context, R.color.matteGreen))
                }
                false -> {
                    paymentStatus.text = "(pending - ${order.paymentMethod})"
                    holder.paymentStatus.setTextColor(ContextCompat.getColor(context, R.color.matteRed))
                }
            }

            showCart.setOnClickListener {
                when(viewModel) {
                    is PurchaseHistoryViewModel -> viewModel.showCartDialog(order.cart)
                }
            }

            itemView.setOnClickListener {
                when(viewModel) {
                    is PurchaseHistoryViewModel -> viewModel.showCartDialog(order.cart)
                }
            }

        }
    }

    override fun getItemCount(): Int {
        return orders.size
    }
}
