package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.AmmaSpecialOrder
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvFoodStatusBinding

class FoodStatusAdapter (
    var orders: List<AmmaSpecialOrder>,
    var orderStatusMap: HashMap<String, String>,
    var selectedPosition: Int? = null,
    var onItemClickListener: FoodStatusOnClickListener
): RecyclerView.Adapter<FoodStatusAdapter.FoodStatusViewHolder>() {

    inner class FoodStatusViewHolder(val binding: RvFoodStatusBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodStatusViewHolder {
        val view = RvFoodStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FoodStatusViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodStatusViewHolder, position: Int) {
        val order = orders[position]
        holder.binding.apply {
            tvOrderId.text = order.id
            tvOrderType.text = when(order.orderType) {
                "month" -> "Monthly Subscription"
                "single" -> "Single Purchase"
                else -> "Custom Subscription"
            }
            tvOrderFor.text = if (order.orderCount == 1) {
                "Serving for 1 Person only"
            } else {
                "Serving for ${order.orderCount} Persons"
            }
            tvAddress.text = "${order.userName}, ${order.addressOne}, ${order.addressTwo}, ${order.city}, ${order.code}"

            tvOrderStatus.isSelected = true
            tvOrderStatus.text = when(orderStatusMap[order.id]) {
                "preparing" -> "Manual Quality check for all ingredients are complete and sent for cooking"
                "cooking" -> "Your Food is being cooked right now"
                "ready" -> "Your Food is packed and out for delivery"
                "success" -> "Your order has been delivered successfully"
                "cancel" -> {
                    btnSend.visibility = View.GONE
                    "Your order has been cancelled. Please reach out to customer support for further details"
                }
                "fail" -> {
                    btnSend.visibility = View.GONE
                    "Failed to deliver your order. Please reach out to customer support for further details"
                }
                "na" -> "Food status not available yet!"
                else -> ""
            }

            clBody.setBackgroundColor(ContextCompat.getColor(clBody.context, R.color.white))
            selectedPosition?.let {
                if (position == it) {
                    clBody.setBackgroundColor(ContextCompat.getColor(clBody.context, R.color.green_light))
                }
            }

            clBody.setOnClickListener {
                onItemClickListener.selectedOrder(order, position)
            }

            btnSend.setOnClickListener {
                onItemClickListener.cancelDelivery(order)
            }
        }
    }

    override fun getItemCount(): Int {
        return orders.size
    }
}


interface FoodStatusOnClickListener {
    fun selectedOrder(order: AmmaSpecialOrder, position: Int)
    fun cancelDelivery(order: AmmaSpecialOrder)
}