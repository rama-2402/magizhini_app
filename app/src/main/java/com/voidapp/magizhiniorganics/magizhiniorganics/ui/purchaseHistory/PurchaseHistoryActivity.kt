package com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.gson.Gson
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.OrderItemsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.PurchaseHistoryAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistory
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityPurchaseHistoryBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateTotalOrderItemService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.PermissionsUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.dialogs.ItemsBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.ceil

class PurchaseHistoryActivity :
    BaseActivity(),
    KodeinAware,
    PurchaseHistoryAdapter.PurchaseHistoryListener
{
    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityPurchaseHistoryBinding
    private val factory: PurchaseHistoryViewModelFactory by instance()
    private lateinit var viewModel: PurchaseHistoryViewModel

    private lateinit var ordersAdapter: PurchaseHistoryAdapter
    private lateinit var orderItemsAdapter: OrderItemsAdapter
    private var mOrderHistory: MutableList<OrderEntity> = mutableListOf()

    private var mFilterMonth = "January"
    private var mFilterYear = "2021"
    private var mCancelOrder = OrderEntity()
    private var mCancelOrderPosition = 0
    private var mInvoiceOrder = OrderEntity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_purchase_history)
        viewModel = ViewModelProvider(this, factory).get(PurchaseHistoryViewModel::class.java)
        binding.viewmodel = viewModel

        title = ""
        setSupportActionBar(binding.tbToolbar)
        mFilterMonth = TimeUtil().getMonth()
        mFilterYear = TimeUtil().getYear()
        binding.fabMonthFilter.text = " $mFilterMonth"

        showShimmer()

        initRecyclerView()
        initLiveData()
        initListeners()
    }

    private fun initListeners() {
        binding.ivBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.tvYearFilter.setOnClickListener {
            val years = resources.getStringArray(R.array.years).toList() as ArrayList<String>
            showListBottomSheet(this, years, data = "years")
        }

        //scroll change listener to hide the fab when scrolling down
        binding.rvPurchaseHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, up: Int, down: Int) {
                super.onScrolled(recyclerView, up, down)
                if (down > 0 && binding.fabMonthFilter.isVisible) {
                    binding.fabMonthFilter.startAnimation(
                        AnimationUtils.loadAnimation(
                            binding.fabMonthFilter.context,
                            R.anim.slide_out_right
                        )
                    )
                    binding.fabMonthFilter.hide()
                } else if (down < 0 && binding.fabMonthFilter.isGone) {
                    binding.fabMonthFilter.show()
                    binding.fabMonthFilter.startAnimation(
                        AnimationUtils.loadAnimation(
                            this@PurchaseHistoryActivity,
                            R.anim.slide_in_right
                        )
                    )
                }
            }
        })

        binding.fabMonthFilter.setOnClickListener {
            val months = resources.getStringArray(R.array.months_name).toList() as ArrayList<String>
            showListBottomSheet(this, months, data = "months")
        }
    }

    fun setYearFilter(year: String) {
        mFilterYear = year
        binding.tvYearFilter.text = year
        fetchData()
    }

    fun setMonthFilter(month: String) {
        mFilterMonth = month
        binding.fabMonthFilter.text = " $month"
        fetchData()
    }

    private fun fetchData() {
        showShimmer()
        viewModel.getAllPurchaseHistory("${mFilterMonth}${mFilterYear}")
    }

    private fun initLiveData() {
        viewModel.getProfileData("${mFilterMonth}${mFilterYear}")

        viewModel.moveToProductReview.observe(this, {
            moveToProductDetails(viewModel.productId, viewModel.productName)
        })

        lifecycleScope.launchWhenStarted {
            viewModel.status.collect { result ->
                when(result) {
                    is NetworkResult.Success -> onSuccessCallback(result.message, result.data)
                    is NetworkResult.Failed -> onFailedCallback(result.message, result.data)
                    is NetworkResult.Loading -> {
                        if (result.message == "") {
                            showProgressDialog()
                        } else {
                            showSuccessDialog("", result.message, result.data)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun startWorkerThread(order: OrderEntity) {
        val stringConvertedOrder = order.toStringConverter(order)
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateTotalOrderItemService>()
                .setInputData(
                    workDataOf(
                        "order" to stringConvertedOrder,
                        Constants.STATUS to false
                    )
                )
                .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun OrderEntity.toStringConverter(order: OrderEntity): String {
        return Gson().toJson(order)
    }

    fun cancellationConfirmed() {
        showProgressDialog()
        viewModel.confirmCancellation(mCancelOrder)
    }

    fun moveToCustomerSupport() {
        Intent(this, ChatActivity::class.java).also {
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private fun moveToProductDetails(productId: String, productName: String) {
        hideCartBottomSheet()
        Intent(this, ProductActivity::class.java).also {
            it.putExtra(Constants.PRODUCTS, productId)
            it.putExtra(Constants.PRODUCT_NAME, productName)
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun showCartBottomSheet() =
        ItemsBottomSheet(this, orderItemsAdapter).show()

    private fun hideCartBottomSheet() =
        ItemsBottomSheet(this, orderItemsAdapter).dismiss()

    private fun initRecyclerView() {
        ordersAdapter = PurchaseHistoryAdapter(
            this,
            mutableListOf(),
            this
        )
        binding.rvPurchaseHistory.layoutManager = LinearLayoutManager(this)
        binding.rvPurchaseHistory.adapter = ordersAdapter

        orderItemsAdapter = OrderItemsAdapter(
            this,
            listOf(),
            viewModel,
            arrayListOf(),
            ""
        )
    }

    private fun showShimmer() {
        with(binding) {
            flShimmerPlaceholder.visible()
            rvPurchaseHistory.remove()
        }
    }

    private fun hideShimmer() {
        with(binding) {
            flShimmerPlaceholder.remove()
            rvPurchaseHistory.visible()
        }
    }

    override fun onBackPressed() {
        Intent(this, HomeActivity::class.java).also {
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
            finishAffinity()
        }
    }

    private fun createPDF(order: OrderEntity) {
        val items = mutableListOf<CartEntity>()
        items.addAll(order.cart)
        singlePageDoc(order, items, 0)
    }

    fun proceedToRequestPermission() = PermissionsUtil.requestStoragePermissions(this)

    fun proceedToRequestManualPermission() = this.openAppSettingsIntent()

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.STORAGE_PERMISSION_CODE) {
            if(
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                showToast(this, "Storage Permission Granted")
                createPDF(mInvoiceOrder)
            } else {
                showToast(this, "Storage Permission Denied")
                showExitSheet(this, "Some or All of the Storage Permission Denied. Please click PROCEED to go to App settings to Allow Permission Manually \n\n PROCEED >> [Settings] >> [Permission] >> Permission Name Containing [Storage or Media or Photos]", "setting")
            }
        }
    }

    private fun onSuccessCallback(message: String, data: Any?) {
        when(message) {
            "orders" -> {
                lifecycleScope.launch {
                    if (data == null) {
                        hideShimmer()
                        binding.llEmptyLayout.visible()
                    } else {
                        binding.llEmptyLayout.remove()
                        data as MutableList<OrderEntity>
                        mOrderHistory.clear()
                        mOrderHistory.sortByDescending {
                            it.purchaseDate
                        }
                        mOrderHistory.addAll(data)
                        ordersAdapter.orders = mOrderHistory
                        ordersAdapter.notifyDataSetChanged()
                        delay(1000)
                        hideShimmer()
                    }
                }
            }
            "cancel" -> {
                startWorkerThread(mCancelOrder)
                    mOrderHistory[mCancelOrderPosition].orderStatus = Constants.CANCELLED
                    ordersAdapter.orders[mCancelOrderPosition] = mOrderHistory[mCancelOrderPosition]
                    ordersAdapter.notifyDataSetChanged()
                    if (mOrderHistory[mCancelOrderPosition].paymentMethod == WALLET) {
                        viewModel.makeTransactionFromWallet(
                            mOrderHistory[mCancelOrderPosition].price,
                            mOrderHistory[mCancelOrderPosition].customerId,
                            "Refund",
                            "Add"
                        )
                    } else {
                        hideProgressDialog()
                        showToast(this, "Order Cancelled", Constants.SHORT)
                        showExitSheet(this, "Delivery Cancelled for Order ID: ${mCancelOrder.orderId}. Based on your mode of payment, the purchase amount of Rs: ${mCancelOrder.price} will be refunded in 4 to 5 Business days. \n \n For further queries please click this message to contact Customer Support", "cs")
                    }
            }
            "transaction" -> {
                data as TransactionHistory
                viewModel.updateTransaction(data)
            }
            "transactionID" -> {
                hideProgressDialog()
                showToast(this, "Order Cancelled", Constants.SHORT)
                showExitSheet(this, "Delivery Cancelled for Order ID: ${mCancelOrder.orderId}. Based on your mode of payment, the purchase amount of Rs: ${mCancelOrder.price} will be refunded in 4 to 5 Business days. \n \n For further queries please click this message to contact Customer Support", "cs")
            }
        }
        viewModel.setEmptyStatus()
    }

    private suspend fun onFailedCallback(message: String, data: Any?) {
        when(message) {
            "cancel" -> {
                hideProgressDialog()
                showErrorSnackBar("Server Error! Order cancellation failed. Try later", true)
            }
            "transaction" -> {
                delay(1000)
                hideProgressDialog()
                showErrorSnackBar(data!! as String, true)
            }
            "transactionID" -> {
                delay(1000)
                hideProgressDialog()
                showExitSheet(this, "Server Error! Could not record wallet transaction. \n \n If Money is already debited from Wallet, Please contact customer support and the transaction will be reverted in 24 Hours", "cs")
            }
        }
        viewModel.setEmptyStatus()
    }

    override fun showCart(cart: List<CartEntity>) {
        orderItemsAdapter.cartItems = cart
        showCartBottomSheet()
    }

    override fun cancelOrder(position: Int) {
        mCancelOrderPosition = position
        mCancelOrder = mOrderHistory[position]
        showExitSheet(this, "Confirm Cancellation")
    }

    override fun generateInvoice(position: Int) {
        mInvoiceOrder = mOrderHistory[position]
//        createPDF(mInvoiceOrder)
        if (PermissionsUtil.hasStoragePermission(this)) {
            createPDF(mInvoiceOrder)
        } else {
            showExitSheet(this, "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
        }
    }

    private fun singlePageDoc(
        order: OrderEntity,
        prodNames: MutableList<CartEntity>,
        serial: Int
    ) {
        Log.e("TAG", "createPDF: pg 1")

        val phoneNumber =
            SharedPref(this).getData(Constants.PHONE_NUMBER, Constants.STRING, "")

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
        val pdfDocument = PdfDocument()
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
            color = ContextCompat.getColor(this@PurchaseHistoryActivity, R.color.black)
        }

        with(paintNormalText) {
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
            textSize = 60f
            color = ContextCompat.getColor(this@PurchaseHistoryActivity, R.color.black)
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

            Log.e("TAG", "createPDF: a")

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
                    startIndex += 29    //incrementing the starting index
                    endIndex =
                        if (startIndex + endIndex >= prodName.length) {  //incrementing the endIndex
                            prodName.length
                        } else {
                            startIndex + 29
                        }
                }
                canvas.drawText(
                    "${serialNo + 1}.",
                    80f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                canvas.drawText(
                    product.price.toString(),
                    1310f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                canvas.drawText(
                    product.quantity.toString(),
                    1800f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
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
                canvas.drawText(
                    "${serialNo + 1}.",
                    80f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                canvas.drawText(prodName, 310f, (startHeight + (difference * i)), paintNormalText)
                lastEndingHeight = startHeight + (difference * i)
                canvas.drawText(
                    product.price.toString(),
                    1310f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                canvas.drawText(
                    product.quantity.toString(),
                    1800f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
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
                Log.e("TAG", "createPDF: move 2")
                canvas.drawBitmap(scaledBitmap, 40f, 50f, paintBmp)
                paintNormalText.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    "*2% of your total bill amount will be donated to Food For Homeless",
                    80f,
                    pgHeight - 80f,
                    paintNormalText
                )
                pdfDocument.finishPage(pageOne)
                otherPages(prodNames, pdfDocument, 2, serialNo + 1)
                break
            } else if (prodNames.isEmpty()) {
                Log.e("TAG", "createPDF: save")
                canvas.drawBitmap(scaledBitmap, 40f, 50f, paintBmp)
                paintNormalText.textAlign = Paint.Align.LEFT
                canvas.drawText(
                    "*2% of your total bill amount will be donated to Food For Homeless",
                    80f,
                    pgHeight - 80f,
                    paintNormalText
                )

                pdfDocument.finishPage(pageOne)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, "Invoice.pdf")
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            put(MediaStore.Downloads.AUTHOR, "Magizhini Organics")
                        }
                        put(MediaStore.Downloads.TITLE, "Invoice")
                    }

                    val resolver = this.contentResolver
                    var pdfUri: Uri? = null

                    val collection =
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                    try {
                        resolver.insert(collection, values)?.also { uri ->
                            resolver.openOutputStream(uri).use {
                                pdfDocument.writeTo(it)
                                pdfUri = uri
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TAG", "createPdf: ${e.message}")
                    }
                    pdfDocument.close()
                    openPdfInvoice(pdfUri!!)
                } else {
                    val file: File = File(
                        getExternalFilesDir("/"), "/Invoice.pdf"
                    )

                    try {
                        val fileOutput = FileOutputStream(file)
                        pdfDocument.writeTo(fileOutput)
                    } catch (e: Exception) {
                        Log.e("TAG", "otherPages: ${e.message}")
                    }
                    pdfDocument.close()
                    openFolder()
                }
            }
        }
    }

    private fun otherPages(
        prodNames: MutableList<CartEntity>,
        pdfDocument: PdfDocument,
        pgNum: Int,
        serial: Int
    ) {
        Log.e("TAG", "createPDF: triggered")

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
            color = ContextCompat.getColor(this@PurchaseHistoryActivity, R.color.black)
        }

        with(paintNormalText) {
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
            textSize = 60f
            color = ContextCompat.getColor(this@PurchaseHistoryActivity, R.color.black)
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
                    startIndex += 29    //incrementing the starting index
                    endIndex =
                        if (startIndex + endIndex >= prodName.length) {  //incrementing the endIndex
                            prodName.length
                        } else {
                            startIndex + 29
                        }
                }
                canvas.drawText(
                    "$serialNo.",
                    80f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                canvas.drawText(
                    product.price.toString(),
                    1310f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                canvas.drawText(
                    product.quantity.toString(),
                    1800f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
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
                canvas.drawText(
                    "$serialNo.",
                    80f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                canvas.drawText(prodName, 310f, (startHeight + (difference * i)), paintNormalText)
                lastEndingHeight = startHeight + (difference * i)
                canvas.drawText(
                    product.price.toString(),
                    1310f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
                canvas.drawText(
                    product.quantity.toString(),
                    1800f,
                    (startHeight + (difference * i)),
                    paintNormalText
                )
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, "Invoice.pdf")
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            put(MediaStore.Downloads.AUTHOR, "Magizhini Organics")
                        }
                        put(MediaStore.Downloads.TITLE, "Invoice")
                    }

                    val resolver = this.contentResolver
                    var pdfUri: Uri? = null

                    val collection =
                        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                    try {
                        resolver.insert(collection, values)?.also { uri ->
                            resolver.openOutputStream(uri).use {
                                pdfDocument.writeTo(it)
                                pdfUri = uri
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TAG", "createPdf: ${e.message}")
                    }
                    pdfDocument.close()
                    openPdfInvoice(pdfUri!!)
                } else {

                    val file: File = File(
                        getExternalFilesDir("/"), "/Invoice.pdf"
                    )

                    try {
                        val fileOutput = FileOutputStream(file)
                        pdfDocument.writeTo(fileOutput)
                    } catch (e: Exception) {
                        Log.e("TAG", "otherPages: ${e.message}")
                    }
                    pdfDocument.close()
                    openFolder()
                }
            }
        }

    }

    private fun openFolder() {
        val uri = Uri.parse(getExternalFilesDir("/").toString())
        val target = Intent(Intent.ACTION_VIEW).also {
            it.setDataAndType(uri, "resource/folder")
            it.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        val intent = Intent.createChooser(target, "Open folder in")
        try {
            startActivity(intent)
        } catch (e: IOException) {
            Log.e("TAG", "openFolder: ${e.message}", )
        }
    }

    private fun openPdfInvoice(uri: Uri) {
        val target = Intent(Intent.ACTION_VIEW).also {
            it.setDataAndType(uri, "application/pdf")
            it.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        val intent = Intent.createChooser(target, "Open PDF in")
        try {
            startActivity(intent)
        } catch (e: IOException) {
            Log.e("TAG", "openFolder: ${e.message}", )
        }
    }
}