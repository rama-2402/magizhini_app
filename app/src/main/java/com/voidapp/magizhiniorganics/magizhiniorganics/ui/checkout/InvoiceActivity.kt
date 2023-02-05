package com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityCheckoutBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateTotalOrderItemService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.*
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ADDRESS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CHECKOUT_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NAVIGATION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STATUS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import ru.nikartm.support.ImageBadgeView

class InvoiceActivity :
    BaseActivity(),
    KodeinAware,
    PaymentResultListener,
    AddressDialogClickListener,
    CustomAlertClickListener {
    override val kodein: Kodein by kodein()
    private val factory: CheckoutViewModelFactory by instance()
    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var viewModel: CheckoutViewModel

    private var cartBottomSheet: BottomSheetBehavior<ConstraintLayout> = BottomSheetBehavior()
    private lateinit var checkoutText: TextView
    private lateinit var cartBtn: ImageBadgeView
    private lateinit var filterBtn: ImageView

    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_checkout)
        viewModel = ViewModelProvider(this, factory).get(CheckoutViewModel::class.java)

        title = ""
        setSupportActionBar(binding.tbToolbar)

        showProgressDialog(true)

        val cartItems = intent.getParcelableArrayListExtra<CartEntity>("dish") ?: mutableListOf()
        viewModel.isCWMCart = intent.getBooleanExtra("cwm", false)

        Checkout.preload(applicationContext)

        initRecyclerView(cartItems)
        initData(cartItems)
        setCartBottom()
        initLiveData()
        listeners()

        lifecycleScope.launch {
            delay(500)
            binding.apply {
                clAddress.startAnimation(
                    AnimationUtils.loadAnimation(
                        this@InvoiceActivity,
                        R.anim.slide_in_right_bounce
                    )
                )
                nsvScrollBody.startAnimation(
                    AnimationUtils.loadAnimation(
                        this@InvoiceActivity,
                        R.anim.slide_up
                    )
                )
                clAddress.visible()
                nsvScrollBody.visible()
                tvFreeDelivery.isSelected = true
                tvFreeDeliveryNotif.isSelected = true
            }
        }
    }

    private fun initData(cartItems: MutableList<CartEntity>) {
        viewModel.getUserProfileData()  //getting profile to update address
        viewModel.getAllCartItem(cartItems) //updating the cart items based on cart from db or cwm
    }

    override fun onPaymentSuccess(response: String?) {
        val orderDetailsMap: HashMap<String, Any> = generateOrderDetailsMap()
        orderDetailsMap["transactionID"] = response!!
        viewModel.placeOrderWithOnlinePayment(orderDetailsMap)
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }

    private fun generateOrderDetailsMap(): HashMap<String, Any> {
        val orderDetailsMap = hashMapOf<String, Any>()
        orderDetailsMap["mrp"] = binding.tvTotalAmt.text.toString().toFloat()
        orderDetailsMap["deliveryPreference"] = binding.spDeliveryPreference.selectedItem
        orderDetailsMap["deliveryNote"] = binding.etDeliveryNote.text.toString().trim()
        orderDetailsMap["appliedCoupon"] = ""
//            if (viewModel.couponPrice != null) {
//            binding.etCoupon.text.toString().trim()
//        } else {
//            ""
//        }
        viewModel.userProfile?.let { profile ->
            orderDetailsMap["userID"] = profile.id
            orderDetailsMap["name"] = profile.name
            orderDetailsMap["phoneNumber"] = profile.phNumber
            orderDetailsMap["address"] = profile.address[0]
        }
        return orderDetailsMap
    }

    private fun initRecyclerView(cartItems: MutableList<CartEntity>) {
        val cartRecycler = findViewById<RecyclerView>(R.id.rvCart)

        cartAdapter = CartAdapter(
            this,
            cartItems,
            viewModel
        )

        cartRecycler.layoutManager = LinearLayoutManager(this)
        cartRecycler.adapter = cartAdapter
    }

    private fun setCartBottom() {
        val bottomSheet = findViewById<ConstraintLayout>(R.id.clBottomCart)
        val checkoutBtn = findViewById<LinearLayout>(R.id.rlCheckOutBtn)
        checkoutText = findViewById(R.id.tvCheckOut)
        cartBtn = findViewById(R.id.ivCart)
        filterBtn = findViewById(R.id.ivFilter)

        setBottomSheetIcon("wallet")

        cartBottomSheet = BottomSheetBehavior.from(bottomSheet)

        cartBottomSheet.isHideable = false
        cartBottomSheet.isDraggable = true

        cartBottomSheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
//                        val content = if (viewModel.isCWMCart) {
//                            "Rs: ${viewModel.getCartPrice(viewModel.cwmDish)}"
//                        } else {
//                            "Rs: ${viewModel.getCartPrice(viewModel.totalCartItems)}"
//                        }
                        val content = "Rs: ${binding.tvTotalAmt.text}"
                        checkoutText.setTextAnimation(content)
                        setBottomSheetIcon("delete")
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        checkoutText.setTextAnimation("PLACE ORDER")
                        setBottomSheetIcon("wallet")
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })

        filterBtn.setOnClickListener {
            if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                lifecycleScope.launch {
                    if (viewModel.isCWMCart) {
                        viewModel.clearCart(viewModel.cwmDish)
                    } else {
                        viewModel.clearCart(viewModel.totalCartItems)
                    }
                }
            } else {
                if (NetworkHelper.isOnline(this)) {
                    updatePreferenceData()
                    Intent(this, WalletActivity::class.java).also { intent ->
                        intent.putExtra(NAVIGATION, CHECKOUT_PAGE)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                } else {
                    showErrorSnackBar("Please check network connection", true)
                }
            }
        }

        cartBtn.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                cartBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        checkoutBtn.setOnClickListener {
            if (viewModel.isCWMCart) {
                if (viewModel.cwmDish.isNullOrEmpty()) {
                    showErrorSnackBar("Add some Items to Cart to proceed", true)
                    return@setOnClickListener
                }
            } else {
                if (viewModel.totalCartItems.isNullOrEmpty()) {
                    showErrorSnackBar("Add some Items to Cart to proceed", true)
                    return@setOnClickListener
                }
            }
            viewModel.userProfile?.let {
                showListBottomSheet(
                    this,
                    arrayListOf<String>(
                        "Online",
                        "Wallet (Rs: ${viewModel.wallet?.amount})",
                        "Cash On Delivery",
                        "Place Order in Whatsapp"
                    )
                )
            } ?: let {
                viewModel.tempAddress?.let {
                    CustomAlertDialog(
                        this,
                        "User not Signed In",
                        "To place order you should be Signed In with Google Account. If you do not wish to login you can still place order using Whatsapp. Select PLACE ORDER WITH WHATSAPP Option to place your order through whatsapp.",
                        "Sign In",
                        "order",
                        this
                    ).show()
                } ?: showToast(this, "Enter address to place order")
            }
        }
    }

    private fun setBottomSheetIcon(content: String) {
        val icon = when (content) {
            "coupon" -> R.drawable.ic_coupon
            "delete" -> R.drawable.ic_delete
            "wallet" -> R.drawable.ic_wallet
            else -> R.drawable.ic_filter
        }
        filterBtn.fadOutAnimation(300)
        filterBtn.setImageDrawable(ContextCompat.getDrawable(this, icon))
        filterBtn.fadInAnimation(300)
        filterBtn.imageTintList =
            if (content == "delete") {
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.matteRed))
            } else {
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_base))
            }
    }

    private fun initLiveData() {
//        viewModel.deliveryNotAvailableDialog.observe(this) {
//            CustomAlertDialog(this).show()
//        }
        viewModel.uiEvent.observe(this) { event ->
            when (event) {
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
            when (event) {
                is CheckoutViewModel.UiUpdate.WalletData -> {
                    //can get updated wallet data if needed
                }
                is CheckoutViewModel.UiUpdate.PopulateCartData -> {
                    event.cartItems?.let {
                        cartAdapter.setCartData(it as MutableList<CartEntity>)
                    } ?: showToast(this, "Looks like your cart is Empty")
                    setDataToViews()
                }
                is CheckoutViewModel.UiUpdate.UpdateCartData -> {
//                    setDataToViews()
                    event.count?.let {
                        cartAdapter.updateItemsCount(event.position, event.count)
                    } ?: cartAdapter.deleteCartItem(event.position)
                    setDataToViews()
                }
                is CheckoutViewModel.UiUpdate.StartingTransaction -> {
                    showLoadStatusDialog("", "Processing payment from Wallet...", "transaction")
                }
                is CheckoutViewModel.UiUpdate.PlacingOrder -> {
                    updateLoadStatusDialogText("placingOrder", event.message)
                }
                is CheckoutViewModel.UiUpdate.OrderPlaced -> {
                    viewModel.updateReferralStatus()
                    updateLoadStatusDialogText("success", event.message)
                    val cartItems = if (viewModel.isCWMCart) {
                        viewModel.cwmDish
                    } else {
                        viewModel.totalCartItems
                    }
                    lifecycleScope.launch {
                        startTotalOrderWorker(cartItems)
                        viewModel.clearCart(cartItems)
                        delay(1800)
                        dismissLoadStatusDialog()
                        if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                            cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }
                }
                is CheckoutViewModel.UiUpdate.WalletTransactionFailed -> {
                    lifecycleScope.launch {
                        delay(1800)
                        dismissLoadStatusDialog()
                        showExitSheet(
                            this@InvoiceActivity,
                            "Server Error! Transaction Failed. If you have already paid or money is deducted from wallet, Please contact customer support we will verify the transaction and refund the amount.  \n \n Click CUSTOMER SUPPORT to open customer support",
                            "cs"
                        )
                    }
                }
                is CheckoutViewModel.UiUpdate.OrderPlacementFailed -> {
                    dismissLoadStatusDialog()
                    showExitSheet(
                        this,
                        "Server Error! Failed to place order. If you have already paid or money is deducted from wallet, Please contact customer support we will verify the transaction and refund the amount. \n \n Click CUSTOMER SUPPORT to open customer support",
                        "cs"
                    )
                }
                is CheckoutViewModel.UiUpdate.ValidatingPurchase -> {
                    showLoadStatusDialog(
                        "",
                        "Validating Transaction...",
                        "purchaseValidation"
                    )
                }
                is CheckoutViewModel.UiUpdate.PopulateAddressData -> {
                    updateAddressInView(event.addressList[0])
//                    updateDeliveryCharge()
                }
                is CheckoutViewModel.UiUpdate.NoProfileFound -> {
                    SharedPref(this).getData(ADDRESS, STRING, "").let { address ->
                        if (address != "") {
                            viewModel.tempAddress = Utils.toAddressDataClass(address as String)
                        }
                        updateAddressInView(viewModel.tempAddress)
//                        setDataToViews()
//                        updateDeliveryCharge()
                    }
                }
                is CheckoutViewModel.UiUpdate.AddressUpdate -> {
                    if (event.isSuccess) {
                        showToast(this@InvoiceActivity, "Address Updated")
                        viewModel.userProfile?.let {
                            updateAddressInView(event.address!!)
                        } ?: updateAddressInView(viewModel.tempAddress)

//                        setDataToViews()
//                        updateDeliveryCharge()
                    } else {
                        showErrorSnackBar(event.message, true)
                    }
                    setDataToViews()
                }
                is CheckoutViewModel.UiUpdate.CouponApplied -> {
                    this.hideKeyboard()
                    if (event.message == "") {
                        showErrorSnackBar(event.message, false)
                        applyUiChangesWithCoupon(false)
                    } else {
                        applyUiChangesWithCoupon(true)
                    }
                }
                is CheckoutViewModel.UiUpdate.CartCleared -> {
                    cartAdapter.emptyCart()
                    setDataToViews()
//                    applyUiChangesWithCoupon(false)
                }
                is CheckoutViewModel.UiUpdate.HowToVideo -> {
                    hideProgressDialog()
                    if (event.url == "") {
                        showToast(
                            this,
                            "Demo Video will be available soon. Sorry for the inconvenience."
                        )
                    } else {
                        openInBrowser(event.url)
                    }
                }
                is CheckoutViewModel.UiUpdate.PlaceOrderWithWhatsapp -> {
                    event.msg?.let { showToast(this, it) }
                    placeOrderWithWhatsapp()
                }
                is CheckoutViewModel.UiUpdate.Empty -> return@observe
                else -> Unit
            }
            viewModel.setEmptyStatus()
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

    private fun updateAddressInView(addressData: Address?) {
        binding.apply {
            addressData?.let { address ->
                tvUserName.setTextAnimation(if (address.userId.isNullOrEmpty()) "" else address.userId)
                tvAddressOne.setTextAnimation(if (address.addressLineOne.isNullOrEmpty()) "" else address.addressLineOne)
                tvAddressTwo.setTextAnimation(if (address.addressLineTwo.isNullOrEmpty()) "" else address.addressLineTwo)
                tvAddressCity.setTextAnimation("${if (address.city.isNullOrEmpty()) "" else address.city} - ${if (address.LocationCode.isNullOrEmpty()) "" else address.LocationCode}")
            } ?: let {
                tvUserName.setTextAnimation("You have not Logged In")
                tvAddressOne.setTextAnimation("Tap to update Address Details to place order")
                tvAddressTwo.setTextAnimation("")
                tvAddressCity.setTextAnimation("")
                tvDeliveryChargeAmt.setTextAnimation("0.00")
            }
        }
    }

    private fun startTotalOrderWorker(cartItems: MutableList<CartEntity>) {
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateTotalOrderItemService>()
                .setInputData(
                    workDataOf(
                        "cart" to cartToStringConverter(cartItems),
                        STATUS to true
                    )
                )
                .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun cartToStringConverter(value: MutableList<CartEntity>): String {
        return Gson().toJson(value)
    }

    private fun showLoadStatusDialog(title: String, body: String, content: String) {
        LoadStatusDialog.newInstance(title, body, content).show(
            supportFragmentManager,
            Constants.LOAD_DIALOG
        )
    }

    private fun dismissLoadStatusDialog() {
        (supportFragmentManager.findFragmentByTag(Constants.LOAD_DIALOG) as? DialogFragment)?.dismiss()
    }

    private fun updateLoadStatusDialogText(filter: String, content: String) {
        LoadStatusDialog.statusContent = content
        LoadStatusDialog.statusText.value = filter
    }

    private fun applyUiChangesWithCoupon(isCouponApplied: Boolean) {
        binding.apply {
            if (isCouponApplied) {
                etCoupon.disable()
                ivCouponInfo.fadInAnimation()
                btnApplyCoupon.remove()
                ivCouponInfo.visible()
            } else {
                viewModel.couponPrice = null
                viewModel.currentCoupon = null
                etCoupon.setText("")
                etCoupon.enable()
                btnApplyCoupon.visible()
                ivCouponInfo.fadOutAnimation()
                ivCouponInfo.remove()
                btnApplyCoupon.text = "Apply"
                btnApplyCoupon.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(btnApplyCoupon.context, R.color.matteRed)
                )
//                btnApplyCoupon.setBackgroundColor(
//                    ContextCompat.getColor(
//                        baseContext,
//                        R.color.green_base
//                    )
//                )
            }
        }
        setDataToViews()
    }

    fun selectedPaymentMode(paymentMethod: String) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        lifecycleScope.launch {
            val cartItems = if (viewModel.isCWMCart) {
                viewModel.cwmDish
            } else {
                viewModel.totalCartItems
            }
            when (paymentMethod) {
                "Online" -> {
                    val mrp = binding.tvTotalAmt.text.toString().toFloat()
                    viewModel.userProfile?.let {
                        startPayment(
                            this@InvoiceActivity,
                            mailID = it.mailId,
                            mrp * 100f,
                            name = it.name,
                            userID = it.id,
                            phoneNumber = it.phNumber
                        ).also { status ->
                            if (!status) {
                                showToast(this@InvoiceActivity, "Error in processing payment")
                            }
                        }
                    }
                }
                "Cash On Delivery" -> {
                    showSwipeConfirmationDialog(
                        this@InvoiceActivity,
                        "Swipe Right to place order",
                        "cod"
                    )
                }
                "Place Order in Whatsapp" -> placeOrderWithWhatsapp()
                else -> {
                    showSwipeConfirmationDialog(
                        this@InvoiceActivity,
                        "Swipe Right to make payment",
                        "wallet"
                    )
                }
            }
        }
    }

    fun approved(paymentMethod: String) {
        if (paymentMethod == "cod") {
            showLoadStatusDialog(
                "",
                "Validating your purchase...",
                "purchaseValidation"
            )
            viewModel.placeCashOnDeliveryOrder(
                generateOrderDetailsMap()
            )
        } else {
            viewModel.proceedForWalletPayment(
                generateOrderDetailsMap()
            )
        }
    }

    private suspend fun updateDeliveryCharge() {
        viewModel.userProfile?.let {
            binding.tvDeliveryChargeAmt.text = viewModel.getDeliveryCharge().toString()
        } ?: let {
            viewModel.tempAddress?.let {
                binding.tvDeliveryChargeAmt.text = viewModel.getDeliveryCharge().toString()
            } ?: let {
                binding.tvDeliveryChargeAmt.setTextAnimation("0.00")
            }
        }
        binding.tvTotalAmt.setTextAnimation(
            "${
                binding.tvTotalAmt.text.toString()
                    .toFloat() + binding.tvDeliveryChargeAmt.text.toString().toFloat()
            }"
        )
        hideProgressDialog()
    }

    private fun setDataToViews() {
        val cartItems = if (viewModel.isCWMCart) {
            viewModel.cwmDish
        } else {
            viewModel.totalCartItems
        }
//        var detailsJob: Job? = null
//        detailsJob?.cancel()
//        detailsJob = lifecycleScope.launch {
//            delay(600)
//            viewModel.currentCoupon?.let {
//                viewModel.verifyCoupon(it.code, cartItems).invokeOnCompletion {
//                    populateInvoiceValues(cartItems)
//                }
//            } ?: populateInvoiceValues(cartItems)
//        }
//        detailsJob = null

        populateInvoiceValues(cartItems)
    }

    private fun populateInvoiceValues(cartItems: MutableList<CartEntity>) = lifecycleScope.launch {
//        val cartOriginalPrice = viewModel.getCartOriginalPrice(cartItems)
//        val cartPrice = viewModel.getCartPrice(cartItems)
        val priceList = viewModel.getCartPrice(cartItems)
        val freeDeliveryLimit: Float =
            viewModel.freeDeliveryLimit ?: viewModel.getFreeDeliveryLimit()
        if (freeDeliveryLimit >= 2000f) {
            binding.tvFreeDeliveryNotif.remove()
            binding.tvFreeDelivery.remove()
        }
        with(binding) {
            var totalPrice = priceList[0]
            if (totalPrice >= freeDeliveryLimit) {
                tvDeliveryChargeAmt.text = "0.00"
                hideProgressDialog()
//                viewModel.getDeliveryCharge()
//                tvTotalAmt.setTextAnimation(totalPrice.toString())
            } else {
                updateDeliveryCharge()
            }
            viewModel.currentCoupon?.let {
                viewModel.couponPrice = viewModel.couponDiscount(it, totalPrice.toDouble())
                if (totalPrice < it.purchaseLimit) {
                    showToast(this@InvoiceActivity, "Coupon removed. Purchase value is less that minimum requirements.")
                    applyUiChangesWithCoupon(false)
                    return@launch
                }
            }
            tvItemsOrderedCount.text = viewModel.getCartItemsQuantity(cartItems).toString()
            cartBtn.badgeValue = tvItemsOrderedCount.text.toString().toInt()
            tvMrpAmount.text = priceList[1].toString()
            tvSavingsInDiscountAmt.text = "${priceList[1] - priceList[0]}"
            tvSavingsInCouponAmt.text = "${viewModel.couponPrice ?: 0.0}"
            tvGstAmount.text = "${viewModel.gstAmount}"
            totalPrice += (viewModel.gstAmount + binding.tvDeliveryChargeAmt.text.toString()
                .toDouble() - tvSavingsInCouponAmt.text.toString().toDouble()).toFloat()
            binding.tvTotalAmt.setTextAnimation(
                "${
                    Utils.roundPrice(totalPrice)
                }"
            )

            if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
//                checkoutText.setTextAnimation("Rs: $cartPrice")
                checkoutText.setTextAnimation("Rs: ${priceList[0]}")
            }
            tvFreeDelivery.text =
                "Discounted Price of Items in cart above Rs: $freeDeliveryLimit is eligible Free Delivery"
            tvFreeDeliveryNotif.text = if (freeDeliveryLimit - priceList[0] < 0) {
                "No delivery charge for your purchase!!"
            } else {
                "Add items worth ${freeDeliveryLimit - priceList[0]} and above to get free delivery!!"
            }
        }
    }

    private fun listeners() {
        binding.apply {
//            etDeliveryNote.setOnTouchListener { _, _ ->
//                binding.nsvScrollBody.requestDisallowInterceptTouchEvent(true)
//                false
//            }
            KeyboardVisibilityEvent.setEventListener(
                this@InvoiceActivity
            ) { isOpen ->
                if (isOpen) {
                    cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
                } else {
                    cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            clAddress.setOnClickListener {
                updateAddress()
            }

            btnApplyCoupon.setOnClickListener {
                if (btnApplyCoupon.text.toString() == "Apply") {
                    val couponCode: String = binding.etCoupon.text.toString().trim()
                    if (couponCode.isNullOrEmpty()) {
                        showToast(this@InvoiceActivity, "Enter a coupon code")
                        return@setOnClickListener
                    }
                    viewModel.currentCoupon?.let {
                        applyUiChangesWithCoupon(true)
                    } ?: viewModel.verifyCoupon(
                        etCoupon.text.toString().trim(),
                        viewModel.totalCartItems
                    )
                } else {
                    applyUiChangesWithCoupon(false)
                }
            }

            ivCouponInfo.setOnClickListener {
                ivCouponInfo.startAnimation(
                    AnimationUtils.loadAnimation(
                        ivCouponInfo.context,
                        R.anim.bounce
                    )
                )
                applyUiChangesWithCoupon(false)
//                viewModel.currentCoupon?.let { coupon ->
//                    val content =
//                        "This Coupon can be used only for the following criteria: \n \n Minimum Purchase Amount: ${coupon.purchaseLimit} \n " +
//                                "Maximum Discount Amount: ${coupon.maxDiscount}\n" +
//                                "\n \n \n ${coupon.description}"
//                    showDescriptionBs(content)
//                }
            }

            ivBackBtn.setOnClickListener {
                onBackPressed()
            }

            ivCustomerSupport.setOnClickListener {
                if (!NetworkHelper.isOnline(this@InvoiceActivity)) {
                    showErrorSnackBar("Please check your Internet Connection", true)
                    return@setOnClickListener
                }
                updatePreferenceData()
                Intent(this@InvoiceActivity, ChatActivity::class.java).also {
                    it.putExtra(NAVIGATION, CHECKOUT_PAGE)
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
        }


//        binding.ivHowTo.setOnClickListener {
//            showProgressDialog(true)
//            viewModel.getHowToVideo("CheckOut")
//        }
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

//    private fun ootItemsDialog(outOfStockItems: List<CartEntity>) {
//        orderItemsAdapter = OrderItemsAdapter(
//            this,
//            outOfStockItems,
//            viewModel,
//            arrayListOf(),
//            "checkout"
//        )
//        hideSuccessDialog()
//        ItemsBottomSheet(this, orderItemsAdapter).show()
//    }

    private fun updatePreferenceData() {
        if (viewModel.clearedProductIDs.isNotEmpty()) {
            val productIDString =
                SharedPref(this).getData(PRODUCTS, Constants.STRING, "").toString()
            val productIDs: MutableList<String> = if (productIDString != "") {
                productIDString.split(":").map { it } as MutableList<String>
            } else {
                mutableListOf<String>()
            }
            viewModel.clearedProductIDs.forEach {
                if (!productIDs.contains(it)) {
                    productIDs.add(it)
                }
            }
//            productIDs.addAll(viewModel.clearedProductIDs.distinct())
            viewModel.clearedProductIDs.clear()
            SharedPref(this).putData(PRODUCTS, Constants.STRING, productIDs.joinToString(":"))
                .toString()
        }
    }

    override fun onBackPressed() {
        when {
            cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED ->
                cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
//            binding.etCoupon.isFocused -> binding.etCoupon.clearFocus()
            binding.etDeliveryNote.isFocused -> binding.etDeliveryNote.clearFocus()
            else -> {
                updatePreferenceData()
                super.onBackPressed()
            }
        }
    }

    override fun onDestroy() {
        viewModel.apply {
            userProfile = null
            wallet = null
//            currentCoupon = null
        }
        super.onDestroy()
    }

    fun updateAddress() {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        val address = viewModel.userProfile?.let {
            it.address[0]
        } ?: let {
            viewModel.tempAddress
        }
        val dialog = AddressDialog()
        val bundle = Bundle()

        bundle.putParcelable("address", address)
        dialog.arguments = bundle
        dialog.show(supportFragmentManager, "addressDialog")
    }

    //from address dialog
    override fun savedAddress(addressMap: HashMap<String, Any>) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        Address(
            userId = addressMap["userId"].toString(),
            addressLineOne = addressMap["addressLineOne"].toString(),
            addressLineTwo = addressMap["addressLineTwo"].toString(),
            LocationCode = addressMap["LocationCode"].toString(),
            LocationCodePosition = addressMap["LocationCodePosition"].toString().toInt(),
            city = addressMap["city"].toString()
        ).also { address ->
            viewModel.deliveryAvailable = true
            viewModel.userProfile?.let {
                viewModel.updateAddress(address)
            } ?: let {
                viewModel.tempAddress = address
                SharedPref(this).putData(ADDRESS, STRING, Utils.toStringForSharedPref(address))
                setDataToViews()
                updateAddressInView(address)
            }
        }
    }

    //function to remove focus of edit text when clicked outside
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun goToSignIn() {
        Intent(this, SignInActivity::class.java).also {
            it.putExtra("goto", "order")
            startActivity(it)
            finish()
        }
    }

    override fun placeOrderWithWhatsapp() {
        lifecycleScope.launch {
            val cartItems = if (viewModel.isCWMCart) {
                viewModel.cwmDish
            } else {
                viewModel.totalCartItems
            }

            val message: String = viewModel.userProfile?.let {
                "Order ID: ${System.currentTimeMillis()} \n Name: ${it.name} \n Phone Number: ${it.phNumber} \n Address: \n ${it.address[0].addressLineOne} \n ${it.address[0].addressLineTwo} \n ${it.address[0].city} - ${it.address[0].LocationCode} \n Price: ${binding.tvTotalAmt.text} (Incl GST + Delivery Charge) \n Delivery Charge: ${binding.tvDeliveryChargeAmt.text} \n Transaction ID: COD \n Purchase Date: ${TimeUtil().getCurrentDate()} \n PaymentDone: False \n Payment Method: COD \n Delivery Preference: ${binding.spDeliveryPreference.selectedItem} \n Delivery Note: ${binding.etDeliveryNote.text} \n Cart Items: ${
                    Utils.createOrderForWhatsapp(
                        cartItems as ArrayList<CartEntity>
                    )
                }"
            } ?: let {
                "Order ID: ${System.currentTimeMillis()} \n Customer ID: Not Signed In \n Price: ${binding.tvTotalAmt.text} (Incl GST + Delivery Charge) \n Delivery Charge: ${binding.tvDeliveryChargeAmt.text} \n Transaction ID: COD \n Purchase Date: ${TimeUtil().getCurrentDate()} \n PaymentDone: False \n Payment Method: COD \n Delivery Preference: ${binding.spDeliveryPreference.selectedItem} \n Delivery Note: ${binding.etDeliveryNote.text} \n Address: ${viewModel.tempAddress!!.userId}, ${viewModel.tempAddress!!.addressLineOne}, ${viewModel.tempAddress!!.addressLineTwo}, ${viewModel.tempAddress!!.city}, ${viewModel.tempAddress!!.LocationCode} \n Cart Items: ${
                    Utils.createOrderForWhatsapp(
                        cartItems as ArrayList<CartEntity>
                    )
                }"
            }

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://api.whatsapp.com/send?phone=+917299827393&text=$message"
                    )
                )
            )
        }
    }

    override fun onClick() {
        Unit
    }
}