package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvSubscriptionHistoryItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Animations
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil

class SubscriptionHistoryAdapter(
    val activity: SubscriptionHistoryActivity,
    val viewModel: SubscriptionHistoryViewModel,
    var subscriptions: List<SubscriptionEntity>
): RecyclerView.Adapter<SubscriptionHistoryAdapter.SubscriptionHistoryViewHolder>() {

    inner class SubscriptionHistoryViewHolder(val binding: RvSubscriptionHistoryItemBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubscriptionHistoryViewHolder {
        val view = RvSubscriptionHistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubscriptionHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubscriptionHistoryViewHolder, position: Int) {
        val subscription = subscriptions[position]
        var renewal = false
        with(holder.binding) {
            tvTransactionIDText.text = subscription.productName
            tvSubID.text = subscription.id
            tvStartSubDate.text = TimeUtil().getCustomDate(dateLong = subscription.startDate)
            tvDueDate.text = TimeUtil().getCustomDate(dateLong = subscription.endDate)
            tvSubType.text = subscription.subType
            when (subscription.status) {
                Constants.SUB_ACTIVE ->  {
                    ivSubStatus.setImageDrawable(ContextCompat.getDrawable(ivSubStatus.context, R.drawable.ic_delivered))
                    tvSubStatus.visibility = View.INVISIBLE
                    tvRenew.visibility = View.VISIBLE
                        renewal = if (
                            subscription.subType != Constants.SINGLE_PURCHASE &&
                            System.currentTimeMillis() > TimeUtil().getCustomDateFromDifference(subscription.endDate, -7)
                        ) {
                            tvRenew.text = "Renew \n Subscription"
                            true
                        } else {
                            tvRenew.text = "Unsubscribe"
                            false
                        }
                }
                Constants.SUB_CANCELLED -> {
                    ivSubStatus.setImageDrawable(ContextCompat.getDrawable(ivSubStatus.context, R.drawable.ic_delivery_cancelled))
                    tvSubStatus.apply {
                        visibility = View.VISIBLE
                        text = Constants.SUB_CANCELLED
                    }
                    tvRenew.visibility = View.INVISIBLE
                }
            }

            tvRenew.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                viewModel.renewSelectedSubscription(subscription, renewal)
            }

            lytSub.setOnClickListener {
                activity.showProgressDialog()
                activity.showCalendarDialog(subscription)
            }
        }
    }

    override fun getItemCount(): Int {
        return subscriptions.size
    }
}