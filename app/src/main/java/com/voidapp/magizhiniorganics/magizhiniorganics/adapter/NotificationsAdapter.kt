package com.voidapp.magizhiniorganics.magizhiniorganics.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserNotificationEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.RvNotificationItemBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Utils.addCharAtIndex
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.loadImg

class NotificationsAdapter(
    private val context: Context,
    var notifications: MutableList<UserNotificationEntity>,
    private val onItemClickListener: NotificationItemClickListener
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(val binding: RvNotificationItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = RvNotificationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.binding.apply {
//            val date = notification.timestamp.toString()
//            val firstChange = date.addCharAtIndex('/', 4)
//            val secondChange = firstChange.addCharAtIndex('/', 7)
            tvDate.text = TimeUtil().getCustomDate(dateLong = notification.timestamp)
            tvTitle.text = notification.title
            if (notification.message != "") {
                tvMessage.text = notification.message
            }
            if (notification.imageUrl != "") {
                ivMessage.loadImg(notification.imageUrl) {}
            } else {
                ivMessage.visibility = View.GONE
            }
            llNotificationBody.setOnClickListener {
                onItemClickListener.clickedNotification(notification, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return notifications.size
    }
//
//    fun deleteNotification(position: Int) {
//        notifications.
//    }

    interface NotificationItemClickListener {
        fun clickedNotification(notification: UserNotificationEntity, position: Int)
    }
}