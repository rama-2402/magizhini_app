package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
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
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil


class InvoiceGenerator(private val activity: Activity) {

    private val context = activity.baseContext
    private val resources = activity.resources

    @RequiresApi(Build.VERSION_CODES.Q)
    fun createPDF(orderEntity: OrderEntity) {
        val pdfDocument = PdfDocument()
        singlePageDoc(orderEntity, orderEntity.cart as MutableList<CartEntity>, pdfDocument, 0)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun singlePageDoc(
        order: OrderEntity,
        prodNames: MutableList<CartEntity>,
        pdfDocument: PdfDocument,
        serial: Int
    ) {

        val phoneNumber =
            SharedPref(context).getData(Constants.PHONE_NUMBER, Constants.STRING, "")

        val pgWidth = 2480
        val pgHeight = 3508

        val normaLineSpace = 100f
        val titleLineSpace = 300f

        val topStartText = 300f
        val rightAlignTextIndent = 2400f

        var serialNo: Int = serial

        val paintTitleText = Paint()
        val paintNormalText = Paint()

        val paintBmp = Paint()
        val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_app_shadow)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 1000, 800, false)
        val pageInfoOne: PdfDocument.PageInfo =
            PdfDocument.PageInfo.Builder(
                pgWidth, pgHeight, 1
            ).create()
        val pageOne: PdfDocument.Page = pdfDocument.startPage(pageInfoOne)
        val canvas: Canvas = pageOne.canvas

        with(paintTitleText) {
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT_BOLD
            textSize = 100f
            color = ContextCompat.getColor(context, R.color.black)
        }

        with(paintNormalText) {
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
            textSize = 60f
            color = ContextCompat.getColor(context, R.color.black)
        }

        canvas.drawText(order.address.userId, 2400f, topStartText, paintTitleText)   //ls - 300
        canvas.drawText(
            order.address.addressLineOne,
            2400f,
            topStartText + normaLineSpace,
            paintNormalText
        ) //400
        canvas.drawText(
            order.address.addressLineTwo,
            2400f,
            topStartText + (2 * normaLineSpace),
            paintNormalText
        )  //500
        canvas.drawText(
            order.address.LocationCode,
            2400f,
            topStartText + (3 * normaLineSpace),
            paintNormalText
        ) //600
        canvas.drawText(
            "Ph: $phoneNumber",
            2400f,
            topStartText + (4 * normaLineSpace),
            paintNormalText
        ) //700

        paintTitleText.textAlign = Paint.Align.CENTER
        canvas.drawText("INVOICE", (pgWidth / 2).toFloat(), 1000f, paintTitleText)

        paintTitleText.textAlign = Paint.Align.LEFT
        paintNormalText.textAlign = Paint.Align.LEFT
        canvas.drawText("MAGIZHINI ORGANICS", 40f, 1200f, paintTitleText)
        canvas.drawText("GST NO: - ", 40f, 1300f, paintNormalText)
        canvas.drawText("Mail: magizhiniorganics2018@gmail.com", 40f, 1400f, paintNormalText)
        canvas.drawText("Mobile: 72998 27393", 40f, 1500f, paintNormalText)

        paintNormalText.textAlign = Paint.Align.RIGHT
        canvas.drawText("Order ID: ${order.orderId}", 2400f, 1200f, paintNormalText)
        canvas.drawText("Date: ${order.purchaseDate}", 2400f, 1300f, paintNormalText)
        canvas.drawText("Payment: ${order.paymentMethod}", 2400f, 1400f, paintNormalText)
        canvas.drawText("Amount(Incl GST): Rs ${order.price}", 2400f, 1500f, paintNormalText)

        paintNormalText.style = Paint.Style.STROKE
        paintNormalText.strokeWidth = 2f
        canvas.drawRect(20f, 1600f, (pgWidth - 40).toFloat(), 860f, paintNormalText)
        canvas.drawRect(20f, 1650f, (pgWidth - 40).toFloat(), 1800f, paintNormalText)

        with(paintNormalText) {
            textAlign = Paint.Align.LEFT
            style = Paint.Style.FILL
        }
        canvas.drawText("Sl.No.", 80f, 1750f, paintNormalText) //40f
        canvas.drawText("Product Name", 600f, 1750f, paintNormalText)   //300f
        canvas.drawText("Price", 1350f, 1750f, paintNormalText) //1300f
        canvas.drawText("Qty", 1800f, 1750f, paintNormalText)   //1650f
        canvas.drawText("Total", 2160f, 1750f, paintNormalText) //2100f

        canvas.drawLine(280f, 1670f, 280f, 1775f, paintNormalText)
        canvas.drawLine(1280f, 1670f, 1280f, 1775f, paintNormalText)
        canvas.drawLine(1700f, 1670f, 1700f, 1775f, paintNormalText)
        canvas.drawLine(2000f, 1670f, 2000f, 1775f, paintNormalText)

        var startHeight = 1750f   //margin from top of the page i.e starting position of the line
        val difference = 200f   //line spacing between each distinct value of lines
        var lineNumbers = 0f    //number of word wrapped lines of the content from the same value
        var lastEndingHeight = 0f

        for (index in prodNames.indices) {    //prodnames

            val product = prodNames[0]

            val prodName = "${product.productName} (${product.variant})"  //prodnames

            val i = index + 1   //i value is to calculate the line spacing difference

            startHeight += (lineNumbers * 100f)     //this check is to include line spacing of previous word wrapped content if it exists

            if (prodName.length >= 30) {    //check to perform word wrap
                paintNormalText.textAlign = Paint.Align.LEFT

                lineNumbers =
                    ceil((prodName.length / 30).toFloat())  //get the number of lines to wrap

                var startIndex = 0  //starting character index for the line
                var endIndex = 29   //ending character index for the line
                for (ln in 0..lineNumbers.toInt()) {
                    lastEndingHeight =
                        if (ln == 0) {  //to make sure there is no extra spacing for the first line
                            canvas.drawText(
                                prodName.substring(startIndex, endIndex),
                                310f,
                                (startHeight + (difference * i)),
                                paintNormalText
                            )
                            startHeight + (difference * i)
                        } else {    //extra spacing is given for the word wrapped lines using (ln * 100f)
                            canvas.drawText(
                                prodName.substring(startIndex, endIndex),
                                310f,
                                (startHeight + (difference * i) + (ln * 100f)),
                                paintNormalText
                            )
                            startHeight + (difference * i) + (ln * 100f)
                        }
                    startIndex += 30    //incrementing the starting index
                    endIndex =
                        if (startIndex + endIndex >= prodName.length) {  //incrementing the endIndex
                            prodName.length
                        } else {
                            startIndex + 29
                        }
                }
                canvas.drawText("${serialNo+1}.", 80f, (startHeight + (difference * i)), paintNormalText)
                canvas.drawText(product.price.toString(), 1310f, (startHeight + (difference * i)), paintNormalText)
                canvas.drawText(product.quantity.toString(), 1800f, (startHeight + (difference * i)), paintNormalText)
                paintNormalText.textAlign = Paint.Align.RIGHT
                canvas.drawText(
                    (product.price * product.quantity).toString(),
                    (pgWidth - 80).toFloat(),
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                serialNo += 1
            } else {    //if no word wrap is needed then just normal text
                paintNormalText.textAlign = Paint.Align.LEFT
                canvas.drawText("${serialNo+1}.", 80f, (startHeight + (difference * i)), paintNormalText)
                canvas.drawText(prodName, 310f, (startHeight + (difference * i)), paintNormalText)
                lastEndingHeight = startHeight + (difference * i)
                canvas.drawText(product.price.toString(), 1310f, (startHeight + (difference * i)), paintNormalText)
                canvas.drawText(product.quantity.toString(), 1800f, (startHeight + (difference * i)), paintNormalText)
                paintNormalText.textAlign = Paint.Align.RIGHT
                canvas.drawText(
                    (product.price * product.quantity).toString(),
                    (pgWidth - 80).toFloat(),
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                serialNo += 1
                lineNumbers = 0f    //since no word wrap we have no extra lines to linenumber is 0f
            }
            prodNames.removeAt(0)
            if (lastEndingHeight >= pgHeight - 400f && prodNames.isNotEmpty()) {
                canvas.drawBitmap(scaledBitmap, 40f, 50f, paintBmp)
                paintNormalText.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    "*2% of your total bill amount will be donated to Food For Homeless",
                    80f,
                    pgHeight - 80f,
                    paintNormalText
                )
                pdfDocument.finishPage(pageOne)
                otherPages(prodNames, pdfDocument, 2, serialNo+1)
                break
            } else if (prodNames.isEmpty()) {
                canvas.drawBitmap(scaledBitmap, 40f, 50f, paintBmp)
                paintNormalText.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    "*2% of your total bill amount will be donated to Food For Homeless",
                    80f,
                    pgHeight - 80f,
                    paintNormalText
                )

                pdfDocument.finishPage(pageOne)
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "sample.pdf")
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        put(MediaStore.Downloads.AUTHOR, "Magizhini Organics")
                    }
                    put(MediaStore.Downloads.TITLE, "Invoice")
                }

                val resolver = activity.contentResolver

                val collection =
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                try {
                    resolver.insert(collection, values)?.also { uri ->
                        resolver.openOutputStream(uri).use {
                            pdfDocument.writeTo(it)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TAG", "createPdf: ${e.message}")
                }
                pdfDocument.close()

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun otherPages(
        prodNames: MutableList<CartEntity>,
        pdfDocument: PdfDocument,
        pgNum: Int,
        serial: Int
    ) {

        val pgWidth = 2480
        val pgHeight = 3508

        var serialNo = serial

        val paintTitleText = Paint()
        val paintNormalText = Paint()

        val pageInfoOne: PdfDocument.PageInfo =
            PdfDocument.PageInfo.Builder(
                pgWidth, pgHeight, pgNum
            ).create()
        val pageOne: PdfDocument.Page = pdfDocument.startPage(pageInfoOne)
        val canvas: Canvas = pageOne.canvas

        with(paintTitleText) {
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT_BOLD
            textSize = 100f
            color = ContextCompat.getColor(context, R.color.black)
        }

        with(paintNormalText) {
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
            textSize = 60f
            color = ContextCompat.getColor(context, R.color.black)
        }

        var startHeight = 50f   //margin from top of the page i.e starting position of the line
        val difference = 200f   //line spacing between each distinct value of lines
        var lineNumbers = 0f    //number of word wrapped lines of the content from the same value
        var lastEndingHeight = 0f

        for (index in prodNames.indices) {    //prodnames

            val product = prodNames[0]
            val prodName = "${product.productName} (${product.variant})" //prodnames

            val i = index + 1   //i value is to calculate the line spacing difference

            startHeight += (lineNumbers * 100f)     //this check is to include line spacing of previous word wrapped content if it exists

            if (prodName.length >= 30) {    //check to perform word wrap
                paintNormalText.textAlign = Paint.Align.LEFT

                lineNumbers =
                    ceil((prodName.length / 30).toFloat())  //get the number of lines to wrap

                var startIndex = 0  //starting character index for the line
                var endIndex = 29   //ending character index for the line
                for (ln in 0..lineNumbers.toInt()) {
                    lastEndingHeight =
                        if (ln == 0) {  //to make sure there is no extra spacing for the first line
                            canvas.drawText(
                                prodName.substring(startIndex, endIndex),
                                310f,
                                (startHeight + (difference * i)),
                                paintNormalText
                            )
                            startHeight + (difference * i)
                        } else {    //extra spacing is given for the word wrapped lines using (ln * 100f)
                            canvas.drawText(
                                prodName.substring(startIndex, endIndex),
                                310f,
                                (startHeight + (difference * i) + (ln * 100f)),
                                paintNormalText
                            )
                            startHeight + (difference * i) + (ln * 100f)
                        }
                    startIndex += 30    //incrementing the starting index
                    endIndex =
                        if (startIndex + endIndex >= prodName.length) {  //incrementing the endIndex
                            prodName.length
                        } else {
                            startIndex + 29
                        }
                }
                canvas.drawText("$serialNo.", 80f, (startHeight + (difference * i)), paintNormalText)
                canvas.drawText(product.price.toString(), 1310f, (startHeight + (difference * i)), paintNormalText)
                canvas.drawText(product.quantity.toString(), 1800f, (startHeight + (difference * i)), paintNormalText)
                paintNormalText.textAlign = Paint.Align.RIGHT
                canvas.drawText(
                    (product.price * product.quantity).toString(),
                    (pgWidth - 80).toFloat(),
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                serialNo += 1
            } else {    //if no word wrap is needed then just normal text
                paintNormalText.textAlign = Paint.Align.LEFT
                canvas.drawText("$serialNo.", 80f, (startHeight + (difference * i)), paintNormalText)
                canvas.drawText(prodName, 310f, (startHeight + (difference * i)), paintNormalText)
                lastEndingHeight = startHeight + (difference * i)
                canvas.drawText(product.price.toString(), 1310f, (startHeight + (difference * i)), paintNormalText)
                canvas.drawText(product.quantity.toString(), 1800f, (startHeight + (difference * i)), paintNormalText)
                paintNormalText.textAlign = Paint.Align.RIGHT
                canvas.drawText(
                    (product.price * product.quantity).toString(),
                    (pgWidth - 80).toFloat(),
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                serialNo += 1
                lineNumbers = 0f    //since no word wrap we have no extra lines to linenumber is 0f
            }
            prodNames.removeAt(0)
            if (lastEndingHeight >= pgHeight - 400f && prodNames.isNotEmpty()) {
                paintNormalText.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    "*2% of your total bill amount will be donated to Food For Homeless",
                    80f,
                    pgHeight - 80f,
                    paintNormalText
                )
                pdfDocument.finishPage(pageOne)
                otherPages(prodNames, pdfDocument, pgNum + 1, serialNo)
                break
            } else if (prodNames.isEmpty()) {
                paintNormalText.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    "*2% of your total bill amount will be donated to Food For Homeless",
                    80f,
                    pgHeight - 80f,
                    paintNormalText
                )

                pdfDocument.finishPage(pageOne)
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "sample.pdf")
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        put(MediaStore.Downloads.AUTHOR, "Magizhini Organics")
                    }
                    put(MediaStore.Downloads.TITLE, "Invoice")
                }

                val resolver = activity.contentResolver

                val collection =
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                try {
                    resolver.insert(collection, values)?.also { uri ->
                        resolver.openOutputStream(uri).use {
                            pdfDocument.writeTo(it)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TAG", "createPdf: ${e.message}")
                }
                pdfDocument.close()

            }
        }

    }
}






























































