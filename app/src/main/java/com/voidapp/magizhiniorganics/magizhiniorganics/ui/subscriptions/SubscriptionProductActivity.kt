package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hsalf.smileyrating.SmileyRating
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ReviewAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySubscriptionProductBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddressBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NAVIGATION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class SubscriptionProductActivity :
    BaseActivity(),
    KodeinAware,
    PaymentResultListener,
    ReviewAdapter.ReviewItemClickListener
{

    //TODO CREATE AN INFO ABOUT WHAT SUB IS

    override val kodein: Kodein by kodein()
    private val factory: SubscriptionProductViewModelFactory by instance()
    private lateinit var viewModel: SubscriptionProductViewModel
    private lateinit var binding: ActivitySubscriptionProductBinding

    private var mProduct = ProductEntity()
    private var mProductId: String = ""
    private var mProductName: String = ""
    private var mRating: Int = 5
    private var mStartDate: Long = 0L

    private var newReview: Boolean = false
    private var isPreviewVisible: Boolean = false
    private var reviewImageUri: Uri? = null

    private lateinit var adapter: ReviewAdapter
    private lateinit var variantAdapter: ArrayAdapter<String>
    private lateinit var mAddressBottomSheet: BottomSheetDialog

    //sub class variables
    private var oSubscription = Subscription()
    private val paymentList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_subscription_product)
        viewModel = ViewModelProvider(this, factory).get(SubscriptionProductViewModel::class.java)
        binding.viewmodel = viewModel

        mProductId = intent.getStringExtra(Constants.PRODUCTS).toString()
        mProductName = intent.getStringExtra(Constants.PRODUCT_NAME).toString()
        viewModel.mProductID = mProductId
        viewModel.navigateToPage = intent.getStringExtra(NAVIGATION).toString()

        setSupportActionBar(binding.tbCollapsedToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        binding.tvProductName.text = mProductName
        binding.tvProductName.isSelected = true

        oSubscription.customerID = SharedPref(this).getData(Constants.USER_ID, Constants.STRING, "").toString()
        oSubscription.startDate = System.currentTimeMillis()

        Checkout.preload(applicationContext)

        initRecyclerView()
        initData()
        initLiveData()
    }

    override fun onPaymentSuccess(response: String?) {
        showSuccessDialog("", "Processing payment ...", "wallet")
        startTransaction()
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }

    private fun startTransaction() {
        oSubscription.productID = mProductId
        oSubscription.productName = mProductName
        oSubscription.monthYear = "${TimeUtil().getMonth()}${TimeUtil().getYear()}"
        oSubscription.phoneNumber = viewModel.userProfile.phNumber
        oSubscription.status = Constants.SUB_ACTIVE
        if (mStartDate == 0L) {
            filterDate(System.currentTimeMillis() + (1000 * 60 * 60 * 24))
        }
        viewModel.generateSubscription(oSubscription)
    }

    private fun initRecyclerView() {
        adapter = ReviewAdapter(
            this,
            arrayListOf(),
            this
        )
        binding.rvReviews.layoutManager = LinearLayoutManager(this)
        binding.rvReviews.adapter = adapter
    }

    fun filterDate(date: Long) {
        if (date < System.currentTimeMillis()) {
            showErrorSnackBar("Pick a valid date", true)
        } else {
            binding.tvFromDate.text = TimeUtil().getCustomDate(dateLong = date)
            mStartDate = date
            oSubscription.startDate = date
            setEndDate(binding.spSubscriptionType.selectedItemPosition)
        }
    }

    private fun setEndDate(position: Int) {
        when(position) {
            0 -> oSubscription.endDate = oSubscription.startDate
            1 -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = oSubscription.startDate
                cal.add(Calendar.DATE, 29)  //since we add the start date as well, we add remaining 29 days to get a total of 30 days
                oSubscription.endDate = cal.timeInMillis
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun clickListeners() {
        with(binding) {
            ivCalendar.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                DatePickerLib().startSubscriptionDate(this@SubscriptionProductActivity)
            }

            edtDescription.setOnTouchListener { _, _ ->
                binding.svBody.requestDisallowInterceptTouchEvent(true)
                false
            }

            ivWallet.setOnClickListener {
                Intent(this@SubscriptionProductActivity, WalletActivity::class.java).also {
                    it.putExtra(NAVIGATION, PRODUCTS)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    onPause()
                }
            }

            cpAddReview.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                newReview = if (newReview) {
                    closeReview()
                    false
                } else {
                    addReview()
                    true
                }
            }

            svBody.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                if (scrollY < oldScrollY && binding.fabSubscribe.isGone) {
                    fabSubscribe.startAnimation(AnimationUtils.loadAnimation(fabSubscribe.context, R.anim.fab_open))
                    fabSubscribe.visibility = View.VISIBLE
                } else if (scrollY > oldScrollY && binding.fabSubscribe.isVisible) {
                    fabSubscribe.startAnimation(AnimationUtils.loadAnimation(fabSubscribe.context, R.anim.fab_close))
                    fabSubscribe.visibility = View.GONE
                }
            })

            //scroll change listener to hide the fab when scrolling down
            rvReviews.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, up: Int, down: Int) {
                    super.onScrolled(recyclerView, up, down)
                    if (down > 0 && binding.fabSubscribe.isVisible) {
                        fabSubscribe.startAnimation(AnimationUtils.loadAnimation(fabSubscribe.context, R.anim.fab_close))
                        fabSubscribe.visibility = View.GONE
                    } else if (down < 0 && binding.fabSubscribe.isGone) {
                        fabSubscribe.startAnimation(AnimationUtils.loadAnimation(fabSubscribe.context, R.anim.fab_open))
                        fabSubscribe.visibility = View.VISIBLE
                    }
                }
            })
            spSubscriptionType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long) {
                    generateEstimate(position)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    generateEstimate(0)
                }
            }
            spVariants.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long) {
                    oSubscription.variantName = "${mProduct.variants[position].variantName} ${mProduct.variants[position].variantType}"
                    setDataToDisplay(position)
                    generateEstimate(spSubscriptionType.selectedItemPosition)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    oSubscription.variantName = "${mProduct.variants[0].variantName} ${mProduct.variants[0].variantType}"
                    setDataToDisplay(0)
                    generateEstimate(0)
                }
            }
            fabSubscribe.setOnClickListener {
                if (cpAddReview.text == "cancel") {
                    return@setOnClickListener
                } else {
                     lifecycleScope.launch(Dispatchers.IO) {
                        oSubscription.id = viewModel.generateSubscriptionID("${TimeUtil().getMonth()}${TimeUtil().getYear()}")
                    }
                    showAddressBs(viewModel.userProfile.address[0])
                }
            }
            tbAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                title = if (abs(verticalOffset) -appBarLayout.totalScrollRange == 0) {
                    mProductName
                } else {
                    ""
                }
            })
            ivPreviewImage.setOnClickListener {
                onBackPressed()
            }
            btnAddImage.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                if (PermissionsUtil.hasStoragePermission(this@SubscriptionProductActivity)) {
                    getAction.launch(pickImageIntent)
                } else {
                    showExitSheet(this@SubscriptionProductActivity, "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
                }
            }

            srSmileyRating.setSmileySelectedListener { type ->
                mRating = when (type) {
                    SmileyRating.Type.TERRIBLE -> 1
                    SmileyRating.Type.BAD -> 2
                    SmileyRating.Type.OKAY -> 3
                    SmileyRating.Type.GOOD -> 4
                    SmileyRating.Type.GREAT -> 5
                    else -> 5
                }
            }

            btnSaveReview.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                Review(
                    "",
                    viewModel.userProfile.name,
                    viewModel.userProfile.profilePicUrl,
                    System.currentTimeMillis(),
                    mRating,
                    getReviewContent()
                ).also { review ->
                    showProgressDialog()
                    viewModel.upsertProductReview(
                        review,
                        mProduct.id,
                        reviewImageUri,
                        reviewImageUri?.let { GlideLoader().imageExtension(this@SubscriptionProductActivity,  reviewImageUri)!! } ?: ""
                    )
                }
            }
            ivInfo.setOnClickListener {
                showDescriptionBs(resources.getString(R.string.subscription_info))
            }
        }
    }

    //this will populate the add/edit address bottom sheet
    private fun showAddressBs(address: Address) {
        mAddressBottomSheet = BottomSheetDialog(this, R.style.BottomSheetDialog)
        val view: DialogBottomAddressBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(this),
                R.layout.dialog_bottom_address,
                null,
                false
            )

        if (address.userId.isNotEmpty()) {
            with(view) {
                etProfileName.setText(address.userId)
                etAddressOne.setText(address.addressLineTwo)
                etAddressTwo.setText(address.addressLineTwo)
                spArea.setSelection(address.LocationCodePosition)
            }
        }

        view.btnSaveAddress.text = "Proceed to payment"

        view.btnSaveAddress.setOnClickListener {
            when {
                validateAddress(view.etProfileName.text.toString()) -> view.etProfileName.error =
                    "*required"
                validateAddress(view.etAddressOne.text.toString()) -> view.etAddressOne.error =
                    "*required"
                validateAddress(view.etAddressTwo.text.toString()) -> view.etAddressTwo.error =
                    "*required"
                else -> {
                    val newAddress = Address(
                        view.etProfileName.text.toString().trim(),
                        view.etAddressOne.text.toString().trim(),
                        view.etAddressTwo.text.toString().trim(),
                        view.spArea.selectedItem.toString(),
                        view.spArea.selectedItemPosition,
                        "Chennai"
                    )
                    oSubscription.address = newAddress
                    hideAddressBs()

                    showListBottomSheet(this, paymentList as ArrayList<String>)
                }
            }
        }

        mAddressBottomSheet.setCancelable(true)
        mAddressBottomSheet.setCanceledOnTouchOutside(true)
        mAddressBottomSheet.setContentView(view.root)

        mAddressBottomSheet.show()
    }

    private fun hideAddressBs() = mAddressBottomSheet.dismiss()

    fun selectedPaymentMode(paymentMode: String) {
        oSubscription.paymentMode = paymentMode
        if (paymentMode == "Online") {
            with(viewModel.userProfile) {
                startPayment(
                    this@SubscriptionProductActivity,
                    mailId,
                    oSubscription.estimateAmount * 100,
                    name,
                    id,
                    phNumber
                ).also { status ->
                    if (!status) {
                        Toast.makeText(this@SubscriptionProductActivity,"Error in processing payment. Try Later ", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            showSwipeConfirmationDialog(this, "swipe right to make payment")
        }
    }

    private fun validateAddress(text: String?): Boolean {
        return text.isNullOrBlank()
    }

    private fun generateEstimate(position: Int = 0) {
        when(position) {
            0 -> {
                oSubscription.subType = Constants.SINGLE_PURCHASE
                binding.tvEstimate.text = "Estimate (single order)"
                binding.tvEstimateAmount.text = "Rs: ${mProduct.variants[binding.spVariants.selectedItemPosition].variantPrice}"
                oSubscription.estimateAmount = mProduct.variants[binding.spVariants.selectedItemPosition].variantPrice.toString().toFloat()
                setEndDate(position)
            }
            1 -> {
                oSubscription.subType = Constants.MONTHLY
                binding.tvEstimate.text = "Estimate (30 days cycle)"
                val price = mProduct.variants[binding.spVariants.selectedItemPosition].variantPrice.toFloat() * 30
                binding.tvEstimateAmount.text = "Rs: $price"
                oSubscription.estimateAmount = price
                setEndDate(position)
            }
        }
    }

    private fun getReviewContent(): String {
        return if (binding.edtDescription.text.isNullOrEmpty()) {
            ""
        } else {
            binding.edtDescription.text.toString().trim()
        }
    }

    private fun initData() {
        showShimmer()
        viewModel.getProductByID(mProductId)
        viewModel.getProfileData()
    }

    private fun initLiveData() {
        viewModel.product.observe(this, {
            mProduct = it
            val variantNames = arrayListOf<String>()
            mProduct.variants.forEach { variant ->
                variantNames.add("${variant.variantName} ${variant.variantType}")
            }
            variantAdapter = ArrayAdapter(
                binding.spVariants.popupContext,
                R.layout.support_simple_spinner_dropdown_item,
                variantNames
            )
            binding.spVariants.adapter = variantAdapter
            setDataToDisplay(0)
            clickListeners()
        })

        viewModel.reviews.observe(this, {
            if (it.isEmpty()) {
                hideShimmer()
                binding.llEmptyLayout.visible()
            } else {
                hideShimmer()
                it.sortByDescending { review ->
                    review.timeStamp
                }
                binding.llEmptyLayout.remove()
                adapter.setData(it)
            }
        })

        lifecycleScope.launchWhenStarted {
            viewModel.wallet.collect { result ->
                when(result) {
                    is NetworkResult.Success -> {
                        with(result.data as Wallet) {
                            viewModel.liveWallet = this
                            paymentList.clear()
                            paymentList.add("Online")
                            paymentList.add("Wallet - (${this.amount})")
                        }
                    }
                    is NetworkResult.Failed -> showErrorSnackBar(result.data as String, true)
                    else -> Unit
                }
            }
        }

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

    private fun setDataToDisplay(variantPosition: Int) {
        val nextDate = System.currentTimeMillis() + (1000 * 60 * 60 * 24)
        with(binding) {
            GlideLoader().loadUserPicture(this@SubscriptionProductActivity, mProduct.thumbnailUrl, ivProductThumbnail)
            tvDiscountedPrice.text = "Rs. ${mProduct.variants[variantPosition].variantPrice}"
            tvFromDate.text = TimeUtil().getCustomDate(dateLong = nextDate)
            tvEstimateAmount.text = "Rs: ${mProduct.variants[variantPosition].variantPrice}"
        }
    }

    fun approved(status: Boolean) {
        showSuccessDialog("", "Processing payment from Wallet...", "wallet")
        lifecycleScope.launch {
            if (viewModel.checkWalletForBalance(oSubscription.estimateAmount)) {
                withContext(Dispatchers.IO) {
                    with(oSubscription) {
                        viewModel.makeTransactionFromWallet(
                            estimateAmount,
                            customerID,
                            id,
                            "Remove"
                        )
                    }
                }
            } else {
                delay(1000)
                hideSuccessDialog()
                showErrorSnackBar(
                    "Insufficient balance in Wallet. Pick another payment method",
                    true
                )
            }
        }
    }

    private fun addReview() {
        with(binding) {
            cpAddReview.text = "cancel"
            cpAddReview.chipIcon = ContextCompat.getDrawable(this@SubscriptionProductActivity, R.drawable.ic_close_white_24dp)
            cpAddReview.chipBackgroundColor = ContextCompat.getColorStateList(cpAddReview.context, R.color.matteRed)
            llNewReview.startAnimation(AnimationUtils.loadAnimation(llNewReview.context, R.anim.slide_in_right))
            llEmptyLayout.startAnimation(AnimationUtils.loadAnimation(llEmptyLayout.context, R.anim.slide_out_left))
            rvReviews.startAnimation(AnimationUtils.loadAnimation(rvReviews.context, R.anim.slide_out_left))
            srSmileyRating.setTitle(SmileyRating.Type.TERRIBLE, "Bad")
            srSmileyRating.setTitle(SmileyRating.Type.BAD, "Not Satisfied")
            srSmileyRating.setTitle(SmileyRating.Type.OKAY, "Satisfied")
            srSmileyRating.setTitle(SmileyRating.Type.GOOD, "Great")
            srSmileyRating.setTitle(SmileyRating.Type.GREAT, "Awesome")
//            fabSubscribe.startAnimation(AnimationUtils.loadAnimation(fabSubscribe.context, R.anim.fab_close))
            fabSubscribe.remove()
            lifecycleScope.launch {
                delay(400)
                llNewReview.visible()
                rvReviews.remove()
                llEmptyLayout.remove()
            }
        }
    }

    private fun closeReview() {
        with(binding) {
            cpAddReview.text = "Add Review"
            edtDescription.setText("")
            ivReviewImage.remove()
            cpAddReview.chipIcon = ContextCompat.getDrawable(this@SubscriptionProductActivity, R.drawable.ic_add)
            cpAddReview.chipBackgroundColor = ContextCompat.getColorStateList(cpAddReview.context, R.color.green_base)
            llNewReview.startAnimation(AnimationUtils.loadAnimation(llNewReview.context, R.anim.slide_out_right))
            llEmptyLayout.startAnimation(AnimationUtils.loadAnimation(llEmptyLayout.context, R.anim.slide_in_left))
            rvReviews.startAnimation(AnimationUtils.loadAnimation(rvReviews.context, R.anim.slide_in_left))
            fabSubscribe.startAnimation(AnimationUtils.loadAnimation(fabSubscribe.context, R.anim.fab_open))
            lifecycleScope.launch {
                delay(400)
                llNewReview.remove()
                rvReviews.visible()
                llEmptyLayout.visible()
            }
        }
    }

    //Title bar back button press function
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onSuccessCallback(message: String, data: Any?) {
        when(message) {
            "review" -> {
                hideProgressDialog()
                showToast(this, data as String)
                closeReview()
            }
            "transaction" -> viewModel.updateTransaction(data as TransactionHistory)
            "transactionID" -> lifecycleScope.launch {
                if (oSubscription.status == Constants.CANCELLED) {
                    delay(1500)
                    hideSuccessDialog()
                    showExitSheet(
                        this@SubscriptionProductActivity,
                        "Outstanding Balance for Delivery Cancelled Dates, Delivery Failed Days and Remaining Days is Added to the Wallet Successfully!\n" +
                                " \n" +
                                " Please Click the message to contact Customer Support for any queries or further assistance",
                        "cs"
                    )
                } else {
                    startTransaction()
                }
            }
            "sub" -> lifecycleScope.launch {
                delay(1500)
                hideSuccessDialog()
                showSuccessDialog("", data as String)
                delay(1500)
                hideSuccessDialog()
                showExitSheet(this@SubscriptionProductActivity, "Subscription created Successfully! \n\n You can manager your subscriptions in Subscription History page. To go to Subscription History click PROCEED below. ", "purchaseHistory")
            }
        }

        viewModel.emptyResult()
    }

    private fun onFailedCallback(message: String, data: Any?) {
        when(message) {
            "review" -> {
                hideProgressDialog()
                showToast(this, data as String)
            }
            "transaction" -> {
                hideSuccessDialog()
                showErrorSnackBar(data!! as String, true)
            }
            "transactionID" -> {
                hideSuccessDialog()
                showExitSheet(this, "Server Error! Could not record wallet transaction. \n \n If Money is already debited from Wallet, Please contact customer support and the transaction will be reverted in 24 Hours", "cs")
            }
        }
        viewModel.emptyResult()
    }

    private fun showShimmer() {
        with(binding) {
            flShimmerPlaceholder.visible()
            rvReviews.remove()
        }
    }

    private fun hideShimmer() {
        with(binding) {
            flShimmerPlaceholder.remove()
            rvReviews.visible()
        }
    }

    private fun navigateToPreviousPage() {
        when(viewModel.navigateToPage) {
            Constants.HOME_PAGE -> {
                Intent(this, HomeActivity::class.java).also {
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    finishAffinity()
                }
            }
            Constants.SUB_HISTORY_PAGE -> finish()
            else -> {
                Intent(this, ShoppingMainActivity::class.java).also {
                    it.putExtra(Constants.CATEGORY, Constants.ALL_PRODUCTS)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    finishAffinity()
                }
            }
        }
    }

    override fun previewImage(url: String) {
        isPreviewVisible = true
        GlideLoader().loadUserPicture(this, url, binding.ivPreviewImage)
        binding.ivPreviewImage.visible()
        binding.ivPreviewImage.startAnimation(Animations.scaleBig)
    }

    override fun onBackPressed() {
        when {
            isPreviewVisible -> {
                binding.ivPreviewImage.startAnimation(Animations.scaleSmall)
                binding.ivPreviewImage.remove()
                isPreviewVisible = false
            }
            else -> {
                navigateToPreviousPage()
            }
        }
    }

    fun navigateToOtherPage(content: String) {
        when(content) {
            "purchaseHistory" -> {
                Intent(this, SubscriptionHistoryActivity::class.java).also {
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    startActivity(it)
                    finish()
                }
            }
        }
    }

    fun proceedToRequestPermission() = PermissionsUtil.requestStoragePermissions(this)

    fun proceedToRequestManualPermission() = this.openAppSettingsIntent()

    private val getAction = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        reviewImageUri = result.data?.data
        reviewImageUri?.let { uri ->
            GlideLoader().loadUserPicture(this, uri, binding.ivReviewImage)
            binding.ivReviewImage.visible()
        }
    }

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
                getAction.launch(pickImageIntent)
            } else {
                showToast(this, "Storage Permission Denied")
                showExitSheet(this, "Some or All of the Storage Permission Denied. Please click PROCEED to go to App settings to Allow Permission Manually \n\n PROCEED >> [Settings] >> [Permission] >> Permission Name Containing [Storage or Media or Photos]", "setting")
            }
        }
    }

    override fun onResume() {
        viewModel.getProfileData()
        super.onResume()
    }
}