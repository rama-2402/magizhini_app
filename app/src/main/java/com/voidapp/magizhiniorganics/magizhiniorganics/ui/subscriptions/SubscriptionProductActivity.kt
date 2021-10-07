package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import com.hsalf.smileyrating.SmileyRating
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ReviewAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.ProductViewPager
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.WalletEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Review
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Subscription
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySubscriptionProductBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddressBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

class SubscriptionProductActivity : BaseActivity(), KodeinAware {

    override val kodein: Kodein by kodein()
    private val factory: SubscriptionProductViewModelFactory by instance()
    private lateinit var viewModel: SubscriptionProductViewModel
    private lateinit var binding: ActivitySubscriptionProductBinding

    private var mProduct = ProductEntity()
    private var mProductId: String = ""
    private var mProductName: String = ""
    private var mRating: Int = 5
    private var mProfile: UserProfileEntity = UserProfileEntity()
    private var mWallet: WalletEntity = WalletEntity()

    private var newReview: Boolean = false

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
        binding.tvProductName.text = mProductName
        binding.tvProductName.isSelected = true

        oSubscription.customerID = SharedPref(this).getData(Constants.USER_ID, Constants.STRING, "").toString()

        initRecyclerView()
        initData()
        initLiveData()
        clickListeners()
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
            binding.tvFromDate.text = Time().getCustomDate(dateLong = date)
            oSubscription.startDate = date
        }
    }

    private fun clickListeners() {
        with(binding) {
            ivCalendar.setOnClickListener {
                DatePickerLib().startSubscriptionDate(this@SubscriptionProductActivity)
            }
            etDescription.setOnTouchListener { _, _ ->
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
                    binding.fabSubscribe.visibility = View.VISIBLE
                } else if (scrollY > oldScrollY && binding.fabSubscribe.isVisible) {
                    binding.fabSubscribe.gone()
                }
            })

            //scroll change listener to hide the fab when scrolling down
            rvReviews.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, up: Int, down: Int) {
                    super.onScrolled(recyclerView, up, down)
                    if (down > 0 && binding.fabSubscribe.isVisible) {
                        binding.fabSubscribe.hide()
                    } else if (down < 0 && binding.fabSubscribe.isGone) {
                        binding.fabSubscribe.show()
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
                mProduct.reviews.add(review)
                adapter.reviews = mProduct.reviews
                adapter.notifyDataSetChanged()
                viewModel.upsertProductReview(mProductId, review, mProduct)
                newReview = false
                closeReview()
                Toast.makeText(this@SubscriptionProductActivity, "Thanks for the review :)", Toast.LENGTH_SHORT).show()
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

                }
            }
            binding.fabSubscribe.setOnClickListener {
                showAddressBs(mProfile.address[0])
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

    private fun hideAddressBs() = mAddressBottomSheet.hide()

    fun setPaymentFilter(paymentMode: String) {
        if (paymentMode == "UPI") {

        } else {
            walletTransaction()
        }
    }

    private fun walletTransaction() {
        if (mWallet.amount < oSubscription.estimateAmount) {
            showErrorSnackBar("Insufficient wallet balance! Please recharge to continue", true)
        } else {
            showSwipeConfirmationDialog(this)
        }
    }

    private fun validateAddress(text: String?): Boolean {
        return text.isNullOrBlank()
    }

    private fun generateEstimate(position: Int) {
        when(position) {
            0 -> {
                binding.tvEstimateAmount.text = "Rs: ${mProduct.variants[0].variantPrice} / purchase"
                oSubscription.estimateAmount = mProduct.variants[0].variantPrice.toFloat()
                oSubscription.paymentMode = Constants.SINGLE_PURCHASE
            }
            1 -> {
                val price = mProduct.variants[0].variantPrice.toFloat() * 5
                binding.tvEstimateAmount.text = "Rs: $price (5 days)"
                oSubscription.estimateAmount = price
                oSubscription.paymentMode = Constants.WEEKDAYS
            }
            2 -> {
                val price = mProduct.variants[0].variantPrice.toFloat() * 2
                binding.tvEstimateAmount.text = "Rs: $price (2 days)"
                oSubscription.estimateAmount = price
                oSubscription.paymentMode = Constants.WEEKENDS
            }
            3 -> {
                val price = mProduct.variants[0].variantPrice.toFloat() * 30
                binding.tvEstimateAmount.text = "Rs: $price (30 days)"
                oSubscription.estimateAmount = price
                oSubscription.paymentMode = Constants.MONTHLY
            }
        }
    }

    private fun addReview() {
        with(binding) {
            cpAddReview.text = "cancel"
            cpAddReview.chipIcon = ContextCompat.getDrawable(this@SubscriptionProductActivity, R.drawable.ic_close_white_24dp)
            llNewReview.show()
            fabSubscribe.gone()
            rvReviews.gone()
        }
    }

    private fun closeReview() {
        with(binding) {
            cpAddReview.text = "Add Review"
            cpAddReview.chipIcon = ContextCompat.getDrawable(this@SubscriptionProductActivity, R.drawable.ic_add)
            llNewReview.gone()
            fabSubscribe.show()
            rvReviews.show()
        }
    }

    private fun getReviewContent(): String {
        return if (binding.etDescription.text.isNullOrEmpty()) {
            ""
        } else {
            binding.etDescription.text.toString().trim()
        }
    }

    private fun initLiveData() {
        viewModel.product.observe(this, {
            mProduct = it
            setDataToDisplay()
        })
        viewModel.profile.observe(this, {
            mProfile = it
        })
        viewModel.wallet.observe(this, {
            mWallet = it
            paymentList.clear()
            paymentList.add("UPI")
            paymentList.add("Wallet - (${mWallet.amount})")
        })
        viewModel.failed.observe(this, {
            showErrorSnackBar(it, true)
        })
        viewModel.subStatus.observe(this, {
            hideSuccessDialog()
            lifecycleScope.launch {
                delay(1500)
                showSuccessDialog("","Creating Subscription... ","order")
                delay(1500)
                hideSuccessDialog()
                showSuccessDialog("", "Subscription Created Successfully!", "complete")
                    delay(2000)
                    hideSuccessDialog()
                    Intent(this@SubscriptionProductActivity, SubscriptionHistoryActivity::class.java).also {
                        startActivity(it)
                        finish()
                    }
                }
        })
    }

    fun approved(status: Boolean) {
        if (status) {
            showSuccessDialog("","Validation Purchase...", "limited")
            oSubscription.productID = mProductId
            oSubscription.monthYear = "${Time().getMonth()}${Time().getYear()}"
            viewModel.generateSubscription(oSubscription)
        } else {
            Toast.makeText(this, "Transaction cancelled", Toast.LENGTH_SHORT).show()
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
            tvFromDate.text = Time().getCustomDate(dateLong = nextDate)
        }
    }

    private fun initData() {
        viewModel.getProductByID(mProductId)
        viewModel.getProfileData()
        viewModel.getWallet()
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
        super.onBackPressed()
        Intent(this, ShoppingMainActivity::class.java).also {
            it.putExtra(Constants.CATEGORY, Constants.ALL_PRODUCTS)
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }
    }
}