package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation.ConversationActivity

object PermissionsUtil {

    fun hasStoragePermission(context: Context): Boolean {

        val storagePermissions = mutableListOf<String>(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        for (permission in storagePermissions) {
            if (
                ContextCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    fun requestStoragePermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            Constants.STORAGE_PERMISSION_CODE
        )
    }

    fun isGpsEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

}