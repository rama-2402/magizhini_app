package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.chatConversation.ConversationActivity

class PermissionsUtil {

   fun checkStoragePermission(activity: Activity) {
       val context: Context = activity.baseContext

       if (ContextCompat.checkSelfPermission(
               context,
               Manifest.permission.READ_EXTERNAL_STORAGE
           )
           == PackageManager.PERMISSION_GRANTED
       ) {
           GlideLoader().showImageChooser(activity)
       } else {
           /*Requests permissions to be granted to this application. These permissions
            must be requested in your manifest, they should not be granted to your app,
            and they should have protection level*/
           ActivityCompat.requestPermissions(
               activity,
               arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
               Constants.READ_STORAGE_PERMISSION_CODE
           )
       }
   }

    fun isGpsEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

}