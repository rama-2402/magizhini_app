package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


fun compressImageToNewFile(context: Context, uri: Uri): File? {
    return try {

        val file: File =
            try {
                context.contentResolver.openInputStream(uri)?.let { inputStream ->
                    val tempFile = createImageFile(context, "tempCache")

                    val fileOutputStream = FileOutputStream(tempFile)

                    inputStream.copyTo(fileOutputStream)
                    inputStream.close()
                    fileOutputStream.close()

                    tempFile
                }
            } catch (e: Exception) {
                null
            } ?: return null

        // BitmapFactory options to downsize the image
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            o.inSampleSize = 6
            // factor of downsizing the image
            var inputStream = FileInputStream(file)
            //Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o)
            inputStream.close()

            // The new size we want to scale to
            val REQUIRED_SIZE = 75

            val oldExif = ExifInterface(file.path)
            val exifOrientation =
                oldExif.getAttribute(ExifInterface.TAG_ORIENTATION)
            if (exifOrientation != null) {
                val newExif =
                    ExifInterface(file.path)
                newExif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation)
                newExif.saveAttributes()
            }

            // Find the correct scale value. It should be the power of 2.
            var scale = 1
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                o.outHeight / scale / 2 >= REQUIRED_SIZE
            ) {
                scale *= 2
            }
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            inputStream = FileInputStream(file)
            val selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2)
            inputStream.close()

            // here i override the original image file
            file.createNewFile()
            val outputStream = FileOutputStream(file)
            selectedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            file
//        file

        } catch (e: Exception) {
            null
        }
    }


 fun createImageFile(context: Context, fileName: String): File {
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(fileName, ".jpg", dir)
}
