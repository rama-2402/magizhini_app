package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import java.io.IOException

class GlideLoader {

    fun showImageChooser(activity: Activity) {
        // An intent for launching the image selection of phone storage.
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        // Launches the image selection of phone storage using the constant code.
        activity.startActivityForResult(galleryIntent, Constants.PICK_IMAGE_REQUEST_CODE)
    }


    fun imageExtension(activity: Activity, uri: Uri?): String? {
        return MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(activity.contentResolver.getType(uri!!))
    }

    /**
     * A function to load image from URI for the user profile picture.
     */
    fun loadUserPicture(context: Context, image: Any, imageView: ImageView) {
        try {
            // Load the user image in the ImageView.
            Glide
                .with(context)
                .load(image) // URI of the image
                .centerCrop() // Scale type of the image.
                .placeholder(R.drawable.ic_app_placeholder) // A default place holder if image is failed to load.
                .into(imageView) // the view in which the image will be loaded.
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

        /**
     * A function to load image from URI for the user profile picture.
     */
    fun loadUserPictureWithoutCrop(context: Context, image: Any, imageView: ImageView) {
        try {
            // Load the user image in the ImageView.
            Glide
                .with(context)
                .load(image) // URI of the image
                .placeholder(R.drawable.ic_app_placeholder) // A default place holder if image is failed to load.
                .into(imageView) // the view in which the image will be loaded.
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}