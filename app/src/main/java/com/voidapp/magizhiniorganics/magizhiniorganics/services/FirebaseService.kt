package com.voidapp.magizhiniorganics.magizhiniorganics.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.SplashActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BOOLEAN
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CUSTOMER_SUPPORT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ONLINE_STATUS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import kotlin.random.Random


private const val CHANNEL_ID = "my_channel"

class FirebaseService : FirebaseMessagingService() {

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val isOnline = SharedPref(this).getData(ONLINE_STATUS, BOOLEAN, false).toString().toBoolean()
        val intent = when(message.data["activity"]) {
            CUSTOMER_SUPPORT -> Intent(this, ChatActivity::class.java)
            else -> Intent(this, SplashActivity::class.java)
        }
        when {
            isOnline && message.data["activity"] == CUSTOMER_SUPPORT -> Unit
            else -> createNotification(intent, message)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createNotification(intent: Intent, message: RemoteMessage) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationID = Random.nextInt()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        val icon: Bitmap = Glide
            .with(this)
            .asBitmap()
            .load(R.drawable.ic_app_shadow)
            .submit()
            .get()

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val notification = if (message.data["image"] == "") {

            val style = NotificationCompat.BigTextStyle()
            style.bigText(message.data["message"])

            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(message.data["title"])
                .setContentText(message.data["message"])
                .setStyle(style)
                .setLargeIcon(icon)
                .setSmallIcon(R.drawable.ic_app_shadow)
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        } else {
            val image: Bitmap = Glide
                .with(this)
                .asBitmap()
                .load(message.data["image"])
                .submit()
                .get()

            val style = NotificationCompat.BigPictureStyle()
            style.bigPicture(image).build()

            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(message.data["title"])
                .setContentText(message.data["message"])
                .setStyle(style)
                .setLargeIcon(icon)
                .setSmallIcon(R.drawable.ic_app_shadow)
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        }

        notificationManager.notify(notificationID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channelName = "channelName"
        val channel = NotificationChannel(CHANNEL_ID, channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "My channel description"
            enableLights(true)
            lightColor = R.color.green_base
        }
        notificationManager.createNotificationChannel(channel)
    }
}