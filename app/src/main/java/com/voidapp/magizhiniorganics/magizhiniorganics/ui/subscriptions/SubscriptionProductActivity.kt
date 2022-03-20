package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.transition.doOnEnd
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayoutMediator
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ReviewAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.SubProductViewPager
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySubscriptionProductBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.PreviewActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.AddressDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.CustomSubDaysDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.LoadStatusDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.AddressDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NAVIGATION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SINGLE_DAY_LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUB_ACTIVE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WALLET
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import kotlin.math.abs

class SubscriptionProductActivity :
    BaseActivity(),
    KodeinAware,
    PaymentResultListener,
    ReviewAdapter.ReviewItemClickListener,
    AddressDialogClickListener
{

    override val kodein: Kodein by kodein()
    private val factory: SubscriptionProductViewModelFactory by instance()
    private lateinit var viewModel: SubscriptionProductViewModel
    private lateinit var binding: ActivitySubscriptionProductBinding

    private var isPreviewVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_subscription_product)
        viewModel = ViewModelProvider(this, factory).get(SubscriptionProductViewModel::class.java)

        viewModel.navigateToPage = intent.getStringExtra(NAVIGATION).toString()

        setSupportActionBar(binding.tbCollapsedToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""
        binding.tvProductName.text = intent.getStringExtra(Constants.PRODUCT_NAME).toString()
        binding.tvProductName.isSelected = true

        Checkout.preload(applicationContext)

        initData(intent.getStringExtra(PRODUCTS).toString())
        initLiveData()
        initViewPager()

        binding.apply {
            clProductDetails.startAnimation(AnimationUtils.loadAnimation(this@SubscriptionProductActivity, R.anim.slide_in_right_bounce))
            llViewPager.startAnimation(AnimationUtils.loadAnimation(this@SubscriptionProductActivity, R.anim.slide_up))
        }
    }

    override fun onPaymentSuccess(response: String?) {
        showSuccessDialog("", "Processing payment ...", "wallet")
        generateSubscriptionMap(response!!)
    }

    private fun generateSubscriptionMap(response: String?) {
        val subscriptionsMap = hashMapOf<String, Any>()
        subscriptionsMap["subType"] = binding.spSubscriptionType.selectedItem.toString()
        subscriptionsMap["monthYear"] = "${TimeUtil().getMonth()}${TimeUtil().getYear()}"
        subscriptionsMap["variantPosition"] = binding.spVariants.selectedItemPosition
        subscriptionsMap["subTypePosition"] = binding.spSubscriptionType.selectedItemPosition
        subscriptionsMap["status"] = SUB_ACTIVE
        subscriptionsMap["paymentMode"] = response?.let {
            "Online"
        } ?: WALLET
        viewModel.generateSubscription(subscriptionsMap, response)
    }


    override fun onPaymentError(p0: Int, p1: String?) {
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }

    private fun initViewPager() {
        val adapter = SubProductViewPager(supportFragmentManager, lifecycle)
        binding.vpFragmentContent.adapter = adapter
        TabLayoutMediator(binding.tlTabLayout, binding.vpFragmentContent) { tab, position ->
            when(position) {
                0 -> tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_about_us)
                1 -> tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_reviews)
                2 -> tab.icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_write_review)
            }
        }.attach()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListeners() {
        with(binding) {
            ivCalendar.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
//                showCalendarDialog()
                val selectedDateMap = HashMap<String, Long>()
                selectedDateMap["date"] = viewModel.subStartDate
                selectedDateMap["month"] = viewModel.subStartDate
                selectedDateMap["year"] = viewModel.subStartDate
                DatePickerLib.showCalendar(this@SubscriptionProductActivity, this@SubscriptionProductActivity, System.currentTimeMillis() + SINGLE_DAY_LONG, null, selectedDateMap)
            }
            ivShowCustomDaysDialog.setOnClickListener {
                CustomSubDaysDialog(
                    this@SubscriptionProductActivity,
                    viewModel.customSubDays,
                    this@SubscriptionProductActivity
                ).show()
            }
            ivWallet.setOnClickListener {
                Intent(this@SubscriptionProductActivity, WalletActivity::class.java).also {
                    it.putExtra(NAVIGATION, PRODUCTS)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
            svBody.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                if (scrollY < oldScrollY && binding.fabSubscribe.isGone) {
                    fabSubscribe.startAnimation(AnimationUtils.loadAnimation(fabSubscribe.context, R.anim.fab_open))
                    fabSubscribe.visible()
                } else if (scrollY > oldScrollY && binding.fabSubscribe.isVisible) {
                    fabSubscribe.startAnimation(AnimationUtils.loadAnimation(fabSubscribe.context, R.anim.fab_close))
                    fabSubscribe.remove()
                }
            })
            spSubscriptionType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long) {
                    if (position == 2) {
                        CustomSubDaysDialog(
                            this@SubscriptionProductActivity,
                            viewModel.customSubDays,
                            this@SubscriptionProductActivity
                        ).show()
                        ivShowCustomDaysDialog.visible()
                    } else {
                        ivShowCustomDaysDialog.remove()
                        generateEstimate(position, spVariants.selectedItemPosition)
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    generateEstimate(0, spVariants.selectedItemPosition)
                }
            }
            spVariants.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long) {
                    setDataToDisplay(position)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    setDataToDisplay(0)
                }
            }
            fabSubscribe.setOnClickListener {
                viewModel.address?.let {
                    openAddressDialog(it)
                } ?:let {
                    viewModel.userProfile?.let {
                        openAddressDialog(it.address[0])
                    } ?:  showErrorSnackBar("User Profile Details not available", true)
                }
            }
            tbAppBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                title = if (abs(verticalOffset) -appBarLayout.totalScrollRange == 0) {
                    viewModel.product?.name
                } else {
                    ""
                }
            })
            ivInfo.setOnClickListener {
                showDescriptionBs(resources.getString(R.string.subscription_info))
            }
        }
    }

    private fun openAddressDialog(address: Address) {
        val dialog = AddressDialog()
        val bundle = Bundle()
        bundle.putParcelable("address", address)
        dialog.arguments = bundle
        dialog.show(supportFragmentManager, "addressDialog")
    }

    fun selectedCalendarDate(date: Long) {
        viewModel.subStartDate = date
        setDataToDisplay(binding.spVariants.selectedItemPosition)
    }
//
//    private fun showCalendarDialog() {
//        val calendar = Calendar.getInstance()
//        val dialog = DatePickerDialog(this, { _, year, month, day_of_month ->
//            calendar[Calendar.YEAR] = year
//            calendar[Calendar.MONTH] = month
//            calendar[Calendar.DAY_OF_MONTH] = day_of_month
//            viewModel.subStartDate = calendar.timeInMillis
//            setDataToDisplay(binding.spVariants.selectedItemPosition)
//        }, calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DAY_OF_MONTH])
//        //this part is to preselect the already selected startsub date
//        dialog.updateDate(
//            TimeUtil().getYear(dateLong = viewModel.subStartDate).toInt(),
//            TimeUtil().getMonthNumber(dateLong = viewModel.subStartDate),
//            TimeUtil().getDateNumber(dateLong = viewModel.subStartDate).toInt()
//        )
//        dialog.datePicker.minDate = calendar.timeInMillis + SINGLE_DAY_LONG
//        dialog.show()
//    }

    fun selectedPaymentMode(paymentMode: String) = lifecycleScope.launch {
        val estimate = viewModel.getEstimateAmount(
            binding.spSubscriptionType.selectedItemPosition,
            binding.spVariants.selectedItemPosition
        )
        if (estimate == 0.0) {
            showErrorSnackBar("Server Error! Try again later", true)
            return@launch
        }
        if (paymentMode == "Online") {
            viewModel.userProfile?.let {
                startPayment(
                    this@SubscriptionProductActivity,
                    it.mailId,
                    estimate.toFloat() * 100,
                    it.name,
                    it.id,
                    it.phNumber
                ).also { status ->
                    if (!status) {
                        Toast.makeText(this@SubscriptionProductActivity,"Error in processing payment. Try Later ", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            viewModel.wallet?.let {
                if (it.amount < estimate) {
                    showErrorSnackBar("Insufficient Balance in Wallet.", true)
                    return@launch
                }
                showSwipeConfirmationDialog(this@SubscriptionProductActivity, "swipe right to make payment")
            } ?: showErrorSnackBar("Server Error! Failed to fetch Wallet", true)
        }
    }

    private fun generateEstimate(
        selectedSubsType: Int,
        selectedVariantPosition: Int
    ) {
        lifecycleScope.launch {
            viewModel.getCancelDates(viewModel.subStartDate, selectedSubsType)
            binding.apply {
                when(selectedSubsType) {
                    0 -> {
                        tvEstimate.setTextAnimation("(30 days cycle)")
                        tvEstimateAmount.setTextAnimation("Rs: ${viewModel.product!!.variants[selectedVariantPosition].variantPrice * 30}")
                        viewModel.customSubDays.clear()
                    }
                    1 -> {
                        tvEstimate.setTextAnimation("(15/30 days cycle)")
                        tvEstimateAmount.setTextAnimation("Rs: ${viewModel.product!!.variants[selectedVariantPosition].variantPrice * 15}")
                        viewModel.customSubDays.clear()
                    }
                    2 -> {
                        val deliverableDates = 30 - viewModel.subCancelledDates.size
                        tvEstimate.setTextAnimation("($deliverableDates/30 days cycle)")
                        tvEstimateAmount.setTextAnimation("Rs: ${viewModel.product!!.variants[selectedVariantPosition].variantPrice * deliverableDates}")
                    }
                }
            }
        }
    }

    private fun initData(productID: String) {
        viewModel.getProductByID(productID)
    }

    private fun initLiveData() {
        viewModel.uiUpdate.observe(this) { event ->
            when(event) {
                is SubscriptionProductViewModel.UiUpdate.PopulateProduct -> {
                    event.product?.let {
                        populateProductDetails(it)
                        initListeners()
                    } ?: let {
                        showToast(this, "Product Not Available")
                        onBackPressed()
                    }
                }
                is SubscriptionProductViewModel.UiUpdate.CheckStoragePermission -> {
                    if (PermissionsUtil.hasStoragePermission(this)) {
                        showToast(this, "Storage Permission Granted")
                        viewModel.previewImage("granted")
                    } else {
                        showExitSheet(this, "The App Needs Storage Permission to access Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
                    }
                }
                is SubscriptionProductViewModel.UiUpdate.OpenPreviewImage -> {
                    isPreviewVisible = true
                    val url = event.imageUrl ?: event.imageUri
                    with(binding) {
//                        GlideLoader().loadUserPictureWithoutCrop(this@SubscriptionProductActivity, url!!, ivPreviewImage)
                        ivPreviewImage.visible()
                        ivPreviewImage.startAnimation(AnimationUtils.loadAnimation(this@SubscriptionProductActivity, R.anim.scale_big))
                    }
                }
                is SubscriptionProductViewModel.UiUpdate.CreateStatusDialog -> {
                    LoadStatusDialog.newInstance("", "Creating Subscription...", "placingOrder").show(supportFragmentManager,
                        Constants.LOAD_DIALOG
                    )
                }
                is SubscriptionProductViewModel.UiUpdate.ValidatingTransaction -> {
                    updateLoadStatusDialog(event.message!!, event.data!!)
                }
                is SubscriptionProductViewModel.UiUpdate.PlacingSubscription -> {
                    updateLoadStatusDialog(event.message!!, event.data!!)
                }
                is SubscriptionProductViewModel.UiUpdate.PlacedSubscription -> {
                    updateLoadStatusDialog(event.message!!, event.data!!)
                }
                is SubscriptionProductViewModel.UiUpdate.DismissStatusDialog -> {
                    (supportFragmentManager.findFragmentByTag(Constants.LOAD_DIALOG) as? DialogFragment)?.dismiss()
                    if (event.status) {
                        showExitSheet(this@SubscriptionProductActivity, "Subscription created Successfully! \n\n You can manager your subscriptions in Subscription History page. To go to Subscription History click PROCEED below. ", "purchaseHistory")
                    } else {
                        showExitSheet(this, "Server Error! Something went wrong while creating your subscription. \n \n If Money is already debited, Please contact customer support and the transaction will be reverted in 24 Hours", "cs")
                    }
                }
                is SubscriptionProductViewModel.UiUpdate.Empty -> return@observe
                else -> Unit
            }
            viewModel.setEmptyStatus()
        }

        viewModel.uiEvent.observe(this) { event ->
            when(event) {
                is UIEvent.Toast -> showToast(this, event.message, event.duration)
                is UIEvent.SnackBar -> showErrorSnackBar(event.message, event.isError)
                is UIEvent.ProgressBar -> {
                    if (event.visibility) {
                        showProgressDialog()
                    } else {
                        hideProgressDialog()
                    }
                }
                is UIEvent.EmptyUIEvent -> return@observe
                else -> Unit
            }
            viewModel.setEmptyUiEvent()
        }
    }

    private fun updateLoadStatusDialog(message: String, data: String) {
        LoadStatusDialog.statusContent = message
        LoadStatusDialog.statusText.value = data
    }

    private fun populateProductDetails(product: ProductEntity) {
        binding.apply {
            val variantNames = mutableListOf<String>()
            product.variants.forEach { variant ->
                variantNames.add("${variant.variantName} ${variant.variantType}")
            }

            spVariants.adapter = ArrayAdapter(
                spVariants.popupContext,
                R.layout.support_simple_spinner_dropdown_item,
                variantNames
            )
        }
    }

    private fun setDataToDisplay(variantPosition: Int) {
        with(binding) {
            viewModel.product?.let { product ->
                window.sharedElementEnterTransition = android.transition.TransitionSet()
                    .addTransition(android.transition.ChangeImageTransform())
                    .addTransition(android.transition.ChangeBounds())
                    .apply {
                        doOnEnd { binding.ivProductThumbnail.loadImg(product.thumbnailUrl) {} }
                    }
                ivProductThumbnail.loadImg(product.thumbnailUrl) {
                    supportStartPostponedEnterTransition()
                }
                tvDiscountedPrice.setTextAnimation("Rs. ${product.variants[variantPosition].variantPrice}")
                tvFromDate.setTextAnimation(TimeUtil().getCustomDate(dateLong = viewModel.subStartDate))
                generateEstimate(spSubscriptionType.selectedItemPosition, variantPosition)
            }
        }
    }

    fun approved(status: Boolean) {
        generateSubscriptionMap(null)
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
                    it.putExtra(Constants.CATEGORY, viewModel.navigateToPage)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                    finishAffinity()
                }
            }
        }
    }

    override fun previewImage(url: String, thumbnail: ShapeableImageView) {
        Intent(this, PreviewActivity::class.java).also { intent ->
            intent.putExtra("url", url)
            intent.putExtra("contentType", "image")
            val options: ActivityOptionsCompat =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this, thumbnail, ViewCompat.getTransitionName(thumbnail)!!)
            startActivity(intent, options.toBundle())
        }
    }
//
//    override fun previewImage(url: String, ) {
//        isPreviewVisible = true
//        GlideLoader().loadUserPicture(this, url, binding.ivPreviewImage)
//        binding.ivPreviewImage.visible()
//        binding.ivPreviewImage.startAnimation(scaleBig)
//    }

    override fun onBackPressed() {
        when {
            isPreviewVisible -> {
                binding.ivPreviewImage.startAnimation(scaleSmall)
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
                viewModel.previewImage("granted")
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

    fun selectedCustomSubDates(dates: ArrayList<String>) {
        if (dates.isNullOrEmpty()) {
            binding.spSubscriptionType.setSelection(0)
        } else {
            viewModel.customSubDays.clear()
            viewModel.customSubDays.addAll(dates)
            generateEstimate(2, binding.spVariants.selectedItemPosition)
        }
    }

    //from address dialog
    override fun savedAddress(addressMap: HashMap<String, Any>, isNew: Boolean) {
        Address(
            userId = addressMap["userId"].toString(),
            addressLineOne = addressMap["addressLineOne"].toString(),
            addressLineTwo = addressMap["addressLineTwo"].toString(),
            LocationCode = addressMap["LocationCode"].toString(),
            LocationCodePosition = addressMap["LocationCodePosition"].toString().toInt(),
            city = addressMap["city"].toString()
        ).also { address ->
            viewModel.address = address
            viewModel.wallet?.let {
                showListBottomSheet(this, arrayListOf("Online", "Wallet - Rs: ${it.amount}"))
            } ?:let {
                showToast(this, "Wallet Data not available")
                selectedPaymentMode("Online")
            }
        }
    }

    override fun onDestroy() {
        viewModel.apply {
            product = null
            wallet = null
            userProfile = null
            address = null
        }
        super.onDestroy()
    }
}