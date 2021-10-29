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
import android.widget.Toast
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
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Subscription
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySubscriptionProductBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddressBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

class SubscriptionProductActivity : BaseActivity(), KodeinAware, PaymentResultListener {

    override val kodein: Kodein by kodein()
    private val factory: SubscriptionProductViewModelFactory by instance()
    private lateinit var viewModel: SubscriptionProductViewModel
    private lateinit var binding: ActivitySubscriptionProductBinding

    private var mProduct = ProductEntity()
    private var mProductId: String = ""
    private var mProductName: String = ""
    private var mRating: Int = 5
    private var mStartDate: Long = 0L

    private var mProfile: UserProfileEntity = UserProfileEntity()
    private var mWallet: Wallet = Wallet()

    private var newReview: Boolean = false
    private var isPreviewVisible: Boolean = false
    private var reviewImageUri: Uri? = null
    private var reviewImageExtension: String = ""

    private lateinit var adapter: ReviewAdapter
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
        clickListeners()

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
            viewModel
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
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
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

            binding.svBody.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
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
                val reviewContent: String = getReviewContent()
                val review = Review(
                    mProfile.name,
                    mProfile.profilePicUrl,
                    System.currentTimeMillis(),
                    mRating,
                    reviewContent
                )
                lifecycleScope.launch {
                    viewModel.upsertProductReview(review, mProduct, reviewImageUri, reviewImageExtension)
                }
            }

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

            fabSubscribe.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    oSubscription.id = viewModel.generateSubscriptionID("${TimeUtil().getMonth()}${TimeUtil().getYear()}")
                }
                showAddressBs(mProfile.address[0])
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
                PermissionsUtil().checkStoragePermission(this@SubscriptionProductActivity)
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

    fun setPaymentFilter(paymentMode: String) {
        oSubscription.paymentMode = paymentMode
        if (paymentMode == "Online") {
            with(mProfile) {
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
            walletTransaction()
        }
    }

    private fun walletTransaction() {
        if (mWallet.amount < oSubscription.estimateAmount) {
            showErrorSnackBar("Insufficient wallet balance! Please recharge to continue", true)
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
                oSubscription.estimateAmount = mProduct.variants[0].variantPrice.toFloat()
                binding.tvEstimate.text = "Estimate (single order)"
                binding.tvEstimateAmount.text = "Rs: ${mProduct.variants[0].variantPrice}"
                setEndDate(position)
            }
            1 -> {
                oSubscription.subType = Constants.MONTHLY
                binding.tvEstimate.text = "Estimate (30 days cycle)"
                val price = mProduct.variants[0].variantPrice.toFloat() * 30
                binding.tvEstimateAmount.text = "Rs: $price"
                oSubscription.estimateAmount = price
                setEndDate(position)
            }
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

    private fun addReview() {
        with(binding) {
            cpAddReview.text = "cancel"
            cpAddReview.chipIcon = ContextCompat.getDrawable(this@SubscriptionProductActivity, R.drawable.ic_close_white_24dp)
            llNewReview.startAnimation(AnimationUtils.loadAnimation(llNewReview.context, R.anim.slide_in_right))
            rvReviews.startAnimation(AnimationUtils.loadAnimation(rvReviews.context, R.anim.slide_out_left))
            fabSubscribe.startAnimation(AnimationUtils.loadAnimation(fabSubscribe.context, R.anim.fab_close))
            lifecycleScope.launch {
                delay(400)
                llNewReview.show()
                fabSubscribe.gone()
                rvReviews.gone()
            }
        }
    }

    private fun closeReview() {
        with(binding) {
            cpAddReview.text = "Add Review"
            edtDescription.setText("")
            ivReviewImage.gone()
            cpAddReview.chipIcon = ContextCompat.getDrawable(this@SubscriptionProductActivity, R.drawable.ic_add)
            llNewReview.startAnimation(AnimationUtils.loadAnimation(llNewReview.context, R.anim.slide_out_right))
            rvReviews.startAnimation(AnimationUtils.loadAnimation(rvReviews.context, R.anim.slide_in_left))
            fabSubscribe.startAnimation(AnimationUtils.loadAnimation(fabSubscribe.context, R.anim.fab_open))
            lifecycleScope.launch {
                delay(400)
                llNewReview.gone()
                rvReviews.show()
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

    private fun initLiveData() {
        viewModel.product.observe(this, {
            mProduct = it
            setDataToDisplay()
        })
        viewModel.profile.observe(this, {
            mProfile = it
            viewModel.getWallet(mProfile.id)
        })
        viewModel.wallet.observe(this, {
            mWallet = it
            paymentList.clear()
            paymentList.add("Online")
            paymentList.add("Wallet - (${mWallet.amount})")
        })
        viewModel.failed.observe(this, {
            showErrorSnackBar(it, true)
        })
        viewModel.subStatus.observe(this, {
            if (it == "complete") {
                lifecycleScope.launch {
                    delay(1500)
                    hideSuccessDialog()
                    showSuccessDialog("","Creating Subscription... ","order")
                    delay(1500)
                    hideSuccessDialog()
                    showSuccessDialog("", "Subscription Created Successfully!", "complete")
                    delay(2000)
                    hideSuccessDialog()
                    Intent(this@SubscriptionProductActivity, SubscriptionHistoryActivity::class.java).also { intent ->
                        startActivity(intent)
                        finish()
                    }
                }
            }
        })
        viewModel.reviewImage.observe(this, {
            isPreviewVisible = true
            with(binding) {
                GlideLoader().loadUserPictureWithoutCrop(this@SubscriptionProductActivity, it, ivPreviewImage)
                ivPreviewImage.show()
                ivPreviewImage.startAnimation(Animations.scaleBig)
            }
        })
        viewModel.serverError.observe(this, {
            hideProgressDialog()
            showErrorSnackBar("Server Error! Please try again later", true)
        })
        viewModel.uploadingReviewStatus.observe(this, {
            if (it == -5) {
                mProduct = viewModel.mUpdatedProductReview
                adapter.reviews = mProduct.reviews
                adapter.notifyDataSetChanged()
                lifecycleScope.launch {
                    delay(1000)
                    hideProgressDialog()
                    newReview = false
                    closeReview()
                    Toast.makeText(this@SubscriptionProductActivity, "Thanks for the review :)", Toast.LENGTH_SHORT).show()
                }
            } else {
                showProgressDialog()
            }
        })
    }

    fun approved(status: Boolean) = lifecycleScope.launch (Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            showSuccessDialog("", "Processing payment from Wallet...", "wallet")
        }
        if (status) {
            if (viewModel.checkWalletForBalance(oSubscription.estimateAmount, mProfile.id)) {
                val id = viewModel.makeTransactionFromWallet(oSubscription.estimateAmount, mProfile.id, oSubscription.id)
                validatingTransactionBeforeOrder(id)
            } else {
                withContext(Dispatchers.Main) {
                    delay(1000)
                    hideSuccessDialog()
                    showErrorSnackBar(
                        "Insufficient balance in Wallet. Pick another payment method",
                        true
                    )
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SubscriptionProductActivity, "Transaction cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun validatingTransactionBeforeOrder(id: String) = withContext(Dispatchers.Main) {
        if ( id == "failed") {
            hideSuccessDialog()
            showErrorSnackBar("Server Error! If money is debited please contact customer support", false)
            return@withContext
        } else {
            startTransaction()
        }
    }


    private fun setDataToDisplay() {
        val nextDate = System.currentTimeMillis() + (1000 * 60 * 60 * 24)
        with(binding) {
            GlideLoader().loadUserPicture(this@SubscriptionProductActivity, mProduct.thumbnailUrl, ivProductThumbnail)
            adapter.reviews = mProduct.reviews
            adapter.notifyDataSetChanged()
            tvVariantName.text = "${mProduct.variants[0].variantName} ${mProduct.variants[0].variantType}"
            tvDiscountedPrice.text = "Rs. ${mProduct.variants[0].variantPrice}"
            tvFromDate.text = TimeUtil().getCustomDate(dateLong = nextDate)
        }
    }

    private fun initData() {
        viewModel.getProductByID(mProductId)
        viewModel.getProfileData()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            //If permission is granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GlideLoader().showImageChooser(this)
            } else {
                //Displaying another toast if permission is not granted
                showErrorSnackBar("Storage Permission Denied!", true)
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.PICK_IMAGE_REQUEST_CODE) {
                if (data != null) {
                    try {
                        // The uri of selected image from phone storage.
                        reviewImageUri = data.data!!

                        binding.ivReviewImage.show()
                        GlideLoader().loadUserPicture(
                            binding.ivReviewImage.context,
                            reviewImageUri!!,
                            binding.ivReviewImage
                        )

                        reviewImageExtension = GlideLoader().imageExtension(this,  reviewImageUri)!!

                    } catch (e: IOException) {
                        e.printStackTrace()
                        showErrorSnackBar("Image selection failed!", true)
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // A log is printed when user close or cancel the image selection.
            Log.e("Request Cancelled", "Image selection cancelled")
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

    override fun onBackPressed() {
        when {
            isPreviewVisible -> {
                binding.ivPreviewImage.startAnimation(Animations.scaleSmall)
                binding.ivPreviewImage.visibility = View.GONE
                isPreviewVisible = false
            }
            else -> {
                Intent(this, ShoppingMainActivity::class.java).also {
                    it.putExtra(Constants.CATEGORY, Constants.ALL_PRODUCTS)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()

                }
            }
        }
    }
}