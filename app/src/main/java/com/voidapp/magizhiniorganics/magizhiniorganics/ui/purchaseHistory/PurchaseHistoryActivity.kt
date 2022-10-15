package com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.google.gson.Gson
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.OrderItemsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.PurchaseHistoryAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityPurchaseHistoryBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateTotalOrderItemService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.CalendarFilterDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.ItemsBottomSheet
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.CalendarFilerDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.product.ProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CANCELLED
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.PermissionsUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.roundToInt

class PurchaseHistoryActivity :
    BaseActivity(),
    KodeinAware,
    PurchaseHistoryAdapter.PurchaseHistoryListener,
    CalendarFilerDialogClickListener
{
    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityPurchaseHistoryBinding
    private val factory: PurchaseHistoryViewModelFactory by instance()
    private lateinit var viewModel: PurchaseHistoryViewModel

    private lateinit var ordersAdapter: PurchaseHistoryAdapter
    private lateinit var orderItemsAdapter: OrderItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_purchase_history)
        viewModel = ViewModelProvider(this, factory).get(PurchaseHistoryViewModel::class.java)
        binding.viewmodel = viewModel

        title = ""
        setSupportActionBar(binding.tbToolbar)

        showShimmer()

        initData()
        initRecyclerView()
        initLiveData()
        initListeners()
    }

    private fun initData() {
        viewModel.getProfileData()
    }

    private fun initListeners() {
        binding.ivBackBtn.setOnClickListener {
            onBackPressed()
        }
        binding.ivFilter.setOnClickListener {
            showCalendarFilterDialog(viewModel.filterMonth, viewModel.filterYear)
        }
    }

    private fun fetchData() {
        showShimmer()
        viewModel.getAllPurchaseHistory()
    }

    private fun initLiveData() {
        viewModel.uiEvent.observe(this) { event ->
            when(event) {
                is UIEvent.Toast -> showToast(this, event.message, event.duration)
                is UIEvent.SnackBar -> showErrorSnackBar(event.message, event.isError)
                is UIEvent.ProgressBar -> {
                    if (event.visibility) {
                        showProgressDialog(true)
                    } else {
                        hideProgressDialog()
                    }
                }
                is UIEvent.EmptyUIEvent -> return@observe
                else -> Unit
            }
            viewModel.setEmptyUiEvent()
        }
        viewModel.uiUpdate.observe(this) { event ->
            when(event) {
                is PurchaseHistoryViewModel.UiUpdate.PopulatePurchaseHistory -> {
                    binding.tvToolbarTitle.text = "${viewModel.filterMonth} ${viewModel.filterYear}"
                    /*
                    * If purchase history is null and message is null -> no history available
                    * if purchase history null and message non null -> some error occurred
                    * if purchase history non null -> data available
                    * */
                    event.purchaseHistory?.let {
                        populatePurchaseHistory(it as MutableList<OrderEntity>)
                    } ?:let {
                        event.message?.let { message ->
                            hideShimmer()
                            showErrorSnackBar(message, true)
                        } ?: emptyPurchaseHistoryUI()
                    }
                }
                is PurchaseHistoryViewModel.UiUpdate.OrderCancelStatus -> {
                    if (event.status) {
                        viewModel.order?.let { order ->
                            order.orderStatus = CANCELLED
                            ordersAdapter.updateOrder(viewModel.orderPosition!!, order)
                            viewModel.purchaseHistory[viewModel.orderPosition!!] = order
                            if (order.paymentMethod != "COD") {
                                viewModel.makeTransactionFromWallet(
                                    order.price,
                                    order.customerId,
                                    "Refund",
                                    "Add"
                                )
                            } else {
                                hideProgressDialog()
                                startTotalOrderWorker(order.cart as MutableList<CartEntity>)
                                showToast(this, "Order Cancelled", Constants.SHORT)
                            }
                        }
                    } else {
                        hideProgressDialog()
                        showErrorSnackBar(event.message!!, true)
                    }
                }
                is PurchaseHistoryViewModel.UiUpdate.RefundStatus -> {
                    hideProgressDialog()
                    viewModel.order?.let { order ->
                        if (event.status) {
                            showExitSheet(this, "Order cancelled successfully. Your Total Order Amount Rs:${order.price} for the Order ID: ${order.orderId} has been Refunded to your Wallet.", "close")
                            startTotalOrderWorker(order.cart as MutableList<CartEntity>)
                        } else {
                            showExitSheet(this, event.message!!, "cs")
                        }
                    }
                }
                is PurchaseHistoryViewModel.UiUpdate.Empty -> return@observe
                is PurchaseHistoryViewModel.UiUpdate.HowToVideo -> {
                    hideProgressDialog()
                    if (event.url == "") {
                        showToast(this, "demo video will be available soon. sorry for the inconvenience.")
                    } else {
                        openInBrowser(event.url)
                    }
                }
                else -> Unit
            }
            viewModel.setEmptyStatus()
        }

        viewModel.moveToProductReview.observe(this) {
            moveToProductDetails(viewModel.productId, viewModel.productName)
        }
    }

    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse(url)
            startActivity(Intent.createChooser(intent, "Open link with"))
        } catch (e: Exception) {
            println("The current phone does not have a browser installed")
        }
    }

    private fun startTotalOrderWorker(cartItems: MutableList<CartEntity>) {
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateTotalOrderItemService>()
                .setInputData(
                    workDataOf(
                        "cart" to cartToStringConverter(cartItems),
                        Constants.STATUS to false
                    )
                )
                .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun cartToStringConverter(value: MutableList<CartEntity>): String {
        return Gson().toJson(value)
    }

    private fun populatePurchaseHistory(orders: MutableList<OrderEntity>) {
        binding.llEmptyLayout.remove()
        ordersAdapter.setPurchaseHistoryData(orders)
        hideShimmer()
    }

    private fun emptyPurchaseHistoryUI() {
        hideShimmer()
        binding.apply {
            llEmptyLayout.visible()
        }
    }

//    private fun startWorkerThread(order: OrderEntity) {
//        val stringConvertedOrder = order.toStringConverter(order)
//        val workRequest: WorkRequest =
//            OneTimeWorkRequestBuilder<UpdateTotalOrderItemService>()
//                .setInputData(
//                    workDataOf(
//                        "order" to stringConvertedOrder,
//                        Constants.STATUS to false
//                    )
//                )
//                .build()
//
//        WorkManager.getInstance(this).enqueue(workRequest)
//    }
//
//    private fun OrderEntity.toStringConverter(order: OrderEntity): String {
//        return Gson().toJson(order)
//    }

    fun cancellationConfirmed() {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        showProgressDialog(false)
        viewModel.confirmCancellation()
    }

    fun moveToCustomerSupport() {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        Intent(this, ChatActivity::class.java).also {
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun moveToProductDetails(productId: String, productName: String) {
        hideCartBottomSheet()
        Intent(this, ProductActivity::class.java).also {
            it.putExtra(Constants.PRODUCTS, productId)
            it.putExtra(Constants.PRODUCT_NAME, productName)
            startActivity(it)
        }
    }

    private fun showCartBottomSheet() =
        ItemsBottomSheet(this, orderItemsAdapter, null).show()

    private fun hideCartBottomSheet() =
        ItemsBottomSheet(this, orderItemsAdapter, null).dismiss()

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
            "purchaseHistory"
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
                showProgressDialog(false)
                viewModel.order?.let { createPDF(it) }
            } else {
                showToast(this, "Storage Permission Denied")
                showExitSheet(this, "Some or All of the Storage Permission Denied. Please click PROCEED to go to App settings to Allow Permission Manually \n\n PROCEED >> [Settings] >> [Permission] >> Permission Name Containing [Storage or Media or Photos]", "setting")
            }
        }
    }

    override fun showCart(cart: List<CartEntity>) {
        orderItemsAdapter.cartItems = cart
        showCartBottomSheet()
    }

    override fun cancelOrder(position: Int) {
        viewModel.orderPosition = position
        viewModel.order = viewModel.purchaseHistory[position]
        showExitSheet(this, "The following order with order ID:${viewModel.order?.orderId} will be cancelled. If you have already paid for the purchase Don't worry. Your order amount Rs: ${viewModel.order?.price} will be refunded back to your Magizhini Wallet within 30 minutes. Click PROCEED to confirm cancellation.")
    }

    override fun openExitSheet(message: String) {
        showExitSheet(this, message, "cs")
    }

    override fun selectedFilter(month: String, year: String) {
        viewModel.filterMonth = month
        viewModel.filterYear = year
        dismissCalendarFilterDialog()
        fetchData()
    }

    override fun cancelDialog() {
        dismissCalendarFilterDialog()
    }

    private fun showCalendarFilterDialog(month: String, year: String) {
        CalendarFilterDialog.newInstance(month, year.toInt()).show(supportFragmentManager,
            "calendar"
        )
    }

    private fun dismissCalendarFilterDialog() {
        (supportFragmentManager.findFragmentByTag("calendar") as? DialogFragment)?.dismiss()
    }

    override fun generateInvoice(position: Int) {
        viewModel.orderPosition = position
        viewModel.order = viewModel.purchaseHistory[position]
        if (PermissionsUtil.hasStoragePermission(this)) {
            showProgressDialog(false)
            createPDF(viewModel.order!!)
        } else {
            showExitSheet(this, "The App Needs Storage Permission to save PDF invoice. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
        }
    }

    override fun onDestroy() {
        viewModel.let {
            it.purchaseHistory.clear()
            it.orderPosition = null
            it.profile = null
            it.order = null
        }
        super.onDestroy()
    }

    private fun singlePageDoc(
        order: OrderEntity,
        prodNames: MutableList<CartEntity>,
        serial: Int
    ) {

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

        val namesX = 2400f

        var reminder = ""
        val lineOne = if (order.address.addressLineOne.length > 50) {
            reminder = order.address.addressLineOne.substring(50, order.address.addressLineOne.length)
            "${order.address.addressLineOne.substring(0, 50)} -"
        } else {
            order.address.addressLineOne
        }
        val lineTwo = if (reminder != "") {
            "- $reminder, ${order.address.addressLineTwo}"
        } else {
            order.address.addressLineTwo
        }

        canvas.drawText(order.address.userId, namesX, topStartText, paintTitleText)   //ls - 300
        canvas.drawText(
            lineOne,
            namesX,
            topStartText + normaLineSpace,
            paintNormalText
        ) //400
        canvas.drawText(
            lineTwo,
            namesX,
            topStartText + (2 * normaLineSpace),
            paintNormalText
        )  //500
        canvas.drawText(
            order.address.LocationCode,
            namesX,
            topStartText + (3 * normaLineSpace),
            paintNormalText
        ) //600
        canvas.drawText(
            "Ph: $phoneNumber",
            namesX,
            topStartText + (4 * normaLineSpace),
            paintNormalText
        ) //700

        paintTitleText.textAlign = Paint.Align.CENTER
        canvas.drawText("INVOICE", (pgWidth / 2).toFloat(), 1000f, paintTitleText)

        paintTitleText.textAlign = Paint.Align.LEFT
        paintNormalText.textAlign = Paint.Align.LEFT

        val companyY = 1200f

        canvas.drawText("MAGIZHINI ORGANICS", 40f, companyY, paintTitleText)
        canvas.drawText("No:26/28, Thirupaacheeswarar Street,", 40f, companyY+100, paintNormalText)
        canvas.drawText("Ayanavaram, Chennai - 23.", 40f, companyY+200, paintNormalText)
        canvas.drawText("GST NO: - 33CDKPG6363B1ZU", 40f, companyY+300, paintNormalText)
        canvas.drawText("Mail: magizhiniorganics2018@gmail.com", 40f, companyY+400, paintNormalText)
        canvas.drawText("Mobile: 72998 27393", 40f, companyY+500, paintNormalText)

        val ogPrice = (((order.price * 100)/105) * 100.0).roundToInt() / 100.0

        paintNormalText.textAlign = Paint.Align.RIGHT
        canvas.drawText("Order ID: ${order.orderId}", namesX, companyY, paintNormalText)
        canvas.drawText("Date: ${order.purchaseDate}", namesX, companyY+100, paintNormalText)
        canvas.drawText("Payment: ${order.paymentMethod}", namesX, companyY+200, paintNormalText)
        canvas.drawText("Bill Price: Rs ${ogPrice}", namesX, companyY+300, paintNormalText)
        canvas.drawText("GST 5%: Rs ${((order.price - ogPrice) * 100.0).roundToInt() / 100.0}", namesX, companyY+400, paintNormalText)
        canvas.drawText("Total Bill: Rs ${(order.price * 100.0).roundToInt() / 100.0}", namesX, companyY+500, paintNormalText)

        paintNormalText.style = Paint.Style.STROKE
        paintNormalText.strokeWidth = 2f
        canvas.drawRect(20f, 1800f, (pgWidth - 40).toFloat(), 860f, paintNormalText)
        canvas.drawRect(20f, 1850f, (pgWidth - 40).toFloat(), 2000f, paintNormalText)

        with(paintNormalText) {
            textAlign = Paint.Align.LEFT
            style = Paint.Style.FILL
        }
        canvas.drawText("Sl.No.", 80f, 1950f, paintNormalText) //40f
        canvas.drawText("Product Name", 600f, 1950f, paintNormalText)   //300f
        canvas.drawText("Price", 1350f, 1950f, paintNormalText) //1300f
        canvas.drawText("Qty", 1800f, 1950f, paintNormalText)   //1650f
        canvas.drawText("Total", 2160f, 1950f, paintNormalText) //2100f

        canvas.drawLine(280f, 1870f, 280f, 1975f, paintNormalText)
        canvas.drawLine(1280f, 1870f, 1280f, 1975f, paintNormalText)
        canvas.drawLine(1700f, 1870f, 1700f, 1975f, paintNormalText)
        canvas.drawLine(2000f, 1870f, 2000f, 1975f, paintNormalText)

        var startHeight = 1950f   //margin from top of the page i.e starting position of the line
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
                        hideProgressDialog()
                        showErrorSnackBar("Failed to generate Invoice. Try later", true)
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
                        hideProgressDialog()
                        showErrorSnackBar("Failed to generate Invoice. Try later", true)
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
                        hideProgressDialog()
                        showErrorSnackBar("Failed to generate Invoice. Try later", true)
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
                        hideProgressDialog()
                        showErrorSnackBar("Failed to generate Invoice. Try later", true)
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
        hideProgressDialog()
        try {
            startActivity(intent)
        } catch (e: IOException) {
            showErrorSnackBar("Failed to generate Invoice. Try later", true)
        }
    }

    private fun openPdfInvoice(uri: Uri) {
        val target = Intent(Intent.ACTION_VIEW).also {
            it.setDataAndType(uri, "application/pdf")
            it.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        val intent = Intent.createChooser(target, "Open PDF in")
        hideProgressDialog()
        try {
            startActivity(intent)
        } catch (e: IOException) {
            showErrorSnackBar("Failed to generate Invoice. Try later", true)
        }
    }
}