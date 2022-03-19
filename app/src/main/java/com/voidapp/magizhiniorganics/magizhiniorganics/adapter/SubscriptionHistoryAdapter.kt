package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvSubscriptionHistoryItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil

class SubscriptionHistoryAdapter(
    var subscriptions: MutableList<SubscriptionEntity>,
    private val onItemClickListener: SubscriptionHistoryListener
): RecyclerView.Adapter<SubscriptionHistoryAdapter.SubscriptionHistoryViewHolder>() {

    inner class SubscriptionHistoryViewHolder(val binding: RvSubscriptionHistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {    }

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
            tvTransactionIDText.text = subscription.productName
            tvSubID.text = subscription.id
            tvStartSubDate.text = TimeUtil().getCustomDate(dateLong = subscription.startDate)
            tvDueDate.text = TimeUtil().getCustomDate(dateLong = subscription.endDate)
            tvSubType.text = "Rs: ${subscription.estimateAmount}"
            ivSubStatus.visibility = View.INVISIBLE
            when (subscription.status) {
                Constants.SUB_ACTIVE ->  {
    //                    ivSubStatus.setImageDrawable(ContextCompat.getDrawable(ivSubStatus.context, R.drawable.ic_delivered))
                    if (
                        System.currentTimeMillis() > TimeUtil().getCustomDateFromDifference(subscription.endDate, -7)
                    ) {
                        tvRenew.text = "Renew \n Subscription"
                    } else {
                        tvRenew.text = "Unsubscribe"
                    }
                    tvRenew.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(tvRenew.context, R.color.matteRed))
                    tvRenew.elevation = 4f
                    tvRenew.setTextColor(ContextCompat.getColor(tvRenew.context, R.color.white))

                }
                Constants.SUB_CANCELLED -> {
//                    ivSubStatus.setImageDrawable(ContextCompat.getDrawable(ivSubStatus.context, R.drawable.ic_delivery_cancelled))
                    tvRenew.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(tvRenew.context, R.color.white))
                    tvRenew.elevation = 0f
                    tvRenew.text = "Subscription \n Cancelled"
                    tvRenew.setTextColor(ContextCompat.getColor(tvRenew.context, R.color.matteRed))
                }
            }

            tvRenew.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                if (tvRenew.text == "Unsubscribe") {
                    onItemClickListener.cancelSub(position)
                } else {
                    onItemClickListener.renewSub(position)
                }
            }

            lytSub.setOnClickListener {
                onItemClickListener.showCalendar(position)
            }
        }
    }

    override fun getItemCount(): Int {
        return subscriptions.size
    }

    interface SubscriptionHistoryListener {
        fun renewSub(position: Int)
        fun showCalendar(position: Int)
        fun cancelSub(position: Int)

    }
}