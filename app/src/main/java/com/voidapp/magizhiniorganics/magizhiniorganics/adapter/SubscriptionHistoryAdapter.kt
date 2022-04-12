package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvSubscriptionHistoryItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CUSTOM_DAYS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SINGLE_DAY_LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil

class SubscriptionHistoryAdapter(
    var subscriptions: MutableList<SubscriptionEntity>,
    private val onItemClickListener: SubscriptionHistoryListener
): RecyclerView.Adapter<SubscriptionHistoryAdapter.SubscriptionHistoryViewHolder>() {

    inner class SubscriptionHistoryViewHolder(val binding: RvSubscriptionHistoryItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubscriptionHistoryViewHolder {
        val view = RvSubscriptionHistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubscriptionHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubscriptionHistoryViewHolder, position: Int) {
        val subscription = subscriptions[position]
        with(holder.binding) {
            tvProductName.isSelected = true
            tvSubStatus.isSelected = true
            tvProductName.text = "${subscription.productName} ${subscription.variantName}"
            tvSubID.text = subscription.id
            tvSubDuration.text =
                "${TimeUtil().getCustomDate(dateLong = subscription.startDate)} - ${TimeUtil().getCustomDate(dateLong = subscription.endDate)}"
            tvDate.text = "Rs: ${subscription.estimateAmount} - Due On: ${TimeUtil().getCustomDate(dateLong = subscription.endDate)}"

            val dateLimit = if (subscription.startDate >= System.currentTimeMillis()) {
                subscription.startDate
            } else {
                System.currentTimeMillis()
            }
            var remainingDays = ((subscription.endDate - dateLimit + SINGLE_DAY_LONG)/SINGLE_DAY_LONG).toInt()
            subscription.cancelledDates.forEach { date ->
               if (date >= dateLimit) {
                   remainingDays -= 1
               }
            }

            tvAddress.text = "${subscription.address.userId}, ${subscription.address.addressLineOne}, ${subscription.address.addressLineTwo}, ${subscription.address.LocationCode}, ${subscription.address.city}"

            ivCalendar.badgeValue = remainingDays

            when (subscription.status) {
                Constants.SUB_ACTIVE ->  {
    //                    ivSubStatus.setImageDrawable(ContextCompat.getDrawable(ivSubStatus.context, R.drawable.ic_delivered))
                    if (
                        System.currentTimeMillis() > TimeUtil().getCustomDateFromDifference(subscription.endDate, -7)
                    ) {
                        btnSend.background = ContextCompat.getDrawable(btnSend.context, R.drawable.bg_order_history_invoice)
                        tvCancel.text = "RENEW"
                    } else {
                        btnSend.background = ContextCompat.getDrawable(btnSend.context, R.drawable.bg_order_history_cancel)
                        tvCancel.text = "CANCEL"
                    }
                    tvSubStatus.text = if (subscription.subType == CUSTOM_DAYS) {
                        "Active - ${subscription.subType} ${subscription.customDates}"
                    } else {
                        "Active - ${subscription.subType}"
                    }
                }
                Constants.SUB_CANCELLED -> {
//                    ivSubStatus.setImageDrawable(ContextCompat.getDrawable(ivSubStatus.context, R.drawable.ic_delivery_cancelled))
                    tvCancel.text = "CANCELLED"
                    btnSend.background = ContextCompat.getDrawable(btnSend.context, R.drawable.bg_order_history_failed)
                    tvSubStatus.text = if (subscription.subType == CUSTOM_DAYS) {
                        "Cancelled - ${subscription.subType} ${subscription.customDates}"
                    } else {
                        "Cancelled - ${subscription.subType}"
                    }
                    ivCalendar.badgeValue = 0
                }
                Constants.EXPIRED -> {
//                    ivSubStatus.setImageDrawable(ContextCompat.getDrawable(ivSubStatus.context, R.drawable.ic_delivery_cancelled))
                    tvCancel.text = "EXPIRED"
                    btnSend.background = ContextCompat.getDrawable(btnSend.context, R.drawable.bg_order_history_failed)
                    tvSubStatus.text = if (subscription.subType == CUSTOM_DAYS) {
                        "Expired - ${subscription.subType} (${subscription.customDates})"
                    } else {
                        "Expired - ${subscription.subType}"
                    }
                    ivCalendar.badgeValue = 0
                }
            }

            btnSend.setOnClickListener {
//                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                if (tvCancel.text == "CANCEL") {
                    onItemClickListener.cancelSub(position)
                } else if (tvCancel.text == "RENEW"){
                    onItemClickListener.renewSub(position)
                }
            }

            lytSub.setOnClickListener {
                if (tvSubID.isFocused) {
                    tvSubID.clearFocus()
                    return@setOnClickListener
                }
                onItemClickListener.showCalendar(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return subscriptions.size
    }

    fun updateSubscription(position: Int, subscription: SubscriptionEntity) {
        var items: MutableList<SubscriptionEntity>? = subscriptions.map { it.copy() } as MutableList
        items?.let {
            it[position] = subscription
            setSubHistoryData(it)
        }
        items = null
    }

    fun setSubHistoryData(newList: List<SubscriptionEntity>) {
        val diffUtil = SubHistoryDiffUtil(subscriptions, newList)
        val diffResult = DiffUtil.calculateDiff(diffUtil)
        subscriptions = newList as ArrayList<SubscriptionEntity>
        diffResult.dispatchUpdatesTo(this)
    }

    class SubHistoryDiffUtil(
        private val oldList: List<SubscriptionEntity>,
        private val newList: List<SubscriptionEntity>
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return when {
                oldList[oldItemPosition].endDate != newList[newItemPosition].endDate -> false
                oldList[oldItemPosition].cancelledDates != newList[newItemPosition].cancelledDates -> false
                oldList[oldItemPosition].status != newList[newItemPosition].status -> false
                else -> true
            }
        }
    }

    interface SubscriptionHistoryListener {
        fun renewSub(position: Int)
        fun showCalendar(position: Int)
        fun cancelSub(position: Int)
    }

}