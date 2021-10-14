package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvSubscriptionHistoryItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvTransactionItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import org.imaginativeworld.whynotimagecarousel.listener.CarouselOnScrollListener

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
        with(holder.binding) {
            tvTransactionIDText.text = subscription.productName
            tvSubID.text = subscription.id
            tvStartSubDate.text = Time().getCustomDate(dateLong = subscription.startDate)
            tvDueDate.text = Time().getCustomDate(dateLong = subscription.endDate)
            tvSubType.text = subscription.subType
            when (subscription.status) {
                Constants.SUB_ACTIVE ->  {
                    ivSubStatus.setImageDrawable(ContextCompat.getDrawable(ivSubStatus.context, R.drawable.ic_delivered))
                    tvSubStatus.text = Constants.SUB_ACTIVE
                }
                Constants.SUB_CANCELLED -> {
                    ivSubStatus.setImageDrawable(ContextCompat.getDrawable(ivSubStatus.context, R.drawable.ic_delivery_cancelled))
                    tvSubStatus.text = Constants.SUB_CANCELLED
                }
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