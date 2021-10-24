package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import java.io.File
import java.io.FileOutputStream


class InvoiceGenerator {

    val TAG: String = "qqqq"

    @RequiresApi(Build.VERSION_CODES.R)
    fun createPdf(context: Context): Uri? {
        val pdfDocument = PdfDocument()
        val paintText = Paint()
        val pageInfoOne: PdfDocument.PageInfo =
            PdfDocument.PageInfo.Builder(
                1200, 2010, 1
            ).create()
        val pageOne: PdfDocument.Page = pdfDocument.startPage(pageInfoOne)
        val canvas: Canvas = pageOne.canvas

        with(paintText) {
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT_BOLD
            textSize = 70f
            color = ContextCompat.getColor(context, R.color.green_base)
        }

        canvas.drawText("Magizhini Organics", 600f, 200f, paintText)


        pdfDocument.finishPage(pageOne)

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "sample.pdf")
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.AUTHOR, "Magizhini Organics")
            put(MediaStore.Downloads.TITLE, "Invoice")
        }

        val resolver = context.contentResolver

        val collection =
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        var returnUri: Uri? = null
        try {
            resolver.insert(collection, values)?.also { uri ->
                resolver.openOutputStream(uri).use {
                    pdfDocument.writeTo(it)
                    returnUri = uri
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "createPdf: ${e.message}")
            return null
        }
        pdfDocument.close()

        return returnUri
    }


}






























































