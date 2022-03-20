package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.imageview.ShapeableImageView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import java.io.IOException
import javax.sql.DataSource


/**
 * A function to load image from URI for the user profile picture.
 */
@SuppressLint("CheckResult")
fun ShapeableImageView.loadImg(url: Any, loadOnlyFromCache: Boolean = false, onLoadingFinished: () -> Unit) {
    try {
        this.scaleType = ImageView.ScaleType.CENTER_CROP
        val listener = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                onLoadingFinished()
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                dataSource: com.bumptech.glide.load.DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                onLoadingFinished()
                return false
            }
        }

        val requestOptions = RequestOptions.placeholderOf(R.drawable.carousel_default_placeholder)
            .dontTransform()
            .onlyRetrieveFromCache(loadOnlyFromCache)

        // Load the user image in the ImageView.
        Glide
            .with(this.context)
            .load(url) // URI of the image
            .centerCrop() // Scale type of the image.
            .placeholder(R.drawable.carousel_default_placeholder) // A default place holder if image is failed to load.
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(requestOptions)
            .listener(listener)
            .into(this) // the view in which the image will be loaded.

    } catch (e: IOException) {
        e.printStackTrace()
    }
}

@SuppressLint("CheckResult")
fun ImageView.loadImg(url: Any, loadOnlyFromCache: Boolean = false, onLoadingFinished: () -> Unit) {
    try {
        this.scaleType = ImageView.ScaleType.CENTER_CROP
        val listener = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                onLoadingFinished()
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                dataSource: com.bumptech.glide.load.DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                onLoadingFinished()
                return false
            }
        }

        val requestOptions = RequestOptions.placeholderOf(R.drawable.carousel_default_placeholder)
            .dontTransform()
            .onlyRetrieveFromCache(loadOnlyFromCache)

        // Load the user image in the ImageView.
        Glide
            .with(this.context)
            .load(url) // URI of the image
//            .centerCrop() // Scale type of the image.
            .placeholder(R.drawable.carousel_default_placeholder) // A default place holder if image is failed to load.
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(requestOptions)
            .listener(listener)
            .into(this) // the view in which the image will be loaded.

    } catch (e: IOException) {
        e.printStackTrace()
    }
}

/**
 * A function to load image from URI for the user profile picture.
 */
@SuppressLint("CheckResult")
fun ShapeableImageView.loadOriginal(url: Any) {
    try {
        // Load the user image in the ImageView.
        Glide
            .with(this.context)
            .load(url) // URI of the image
            .placeholder(R.drawable.carousel_default_placeholder) // A default place holder if image is failed to load.
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(this) // the view in which the image will be loaded.
            .apply {
                RequestOptions().dontTransform()
            }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

@SuppressLint("CheckResult")
fun ImageView.loadOriginal(url: Any, loadOnlyFromCache: Boolean = true, onLoadingFinished: () -> Unit) {
    try {
        val listener = object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                onLoadingFinished()
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: com.bumptech.glide.request.target.Target<Drawable>?,
                dataSource: com.bumptech.glide.load.DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                onLoadingFinished()
                return false
            }
        }

        val requestOptions = RequestOptions.placeholderOf(R.drawable.carousel_default_placeholder)
            .dontTransform()
            .onlyRetrieveFromCache(loadOnlyFromCache)

        // Load the user image in the ImageView.
        Glide
            .with(this.context)
            .load(url) // URI of the image
//            .centerCrop() // Scale type of the image.
            .placeholder(R.drawable.carousel_default_placeholder) // A default place holder if image is failed to load.
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(requestOptions)
            .listener(listener)
            .into(this) // the view in which the image will be loaded.

    } catch (e: IOException) {
        e.printStackTrace()
    }
}

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