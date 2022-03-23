package com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.AddressAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityCheckoutBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.AddressDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.LoadStatusDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.AddressDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CHECKOUT_PAGE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.NAVIGATION
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PRODUCTS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.Job
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
    AddressAdapter.OnAddressClickListener,
    AddressDialogClickListener
{
    override val kodein: Kodein by kodein()
    private val factory: CheckoutViewModelFactory by instance()
    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var viewModel: CheckoutViewModel

    private var cartBottomSheet: BottomSheetBehavior<ConstraintLayout> = BottomSheetBehavior()
    private lateinit var checkoutText: TextView
    private lateinit var cartBtn: ImageBadgeView
    private lateinit var filterBtn: ImageView

    private lateinit var addressAdapter: AddressAdapter
    private lateinit var cartAdapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_checkout)
        viewModel = ViewModelProvider(this, factory).get(CheckoutViewModel::class.java)
        binding.viewmodel = viewModel

        title = ""
        setSupportActionBar(binding.tbToolbar)
        binding.ivBackBtn.setOnClickListener {
            onBackPressed()
        }

        viewModel.navigateToPage = intent.getStringExtra(NAVIGATION).toString()
        val cartItems = intent.getParcelableArrayListExtra<CartEntity>("dish") ?: mutableListOf()
        viewModel.isCWMCart = intent.getBooleanExtra("cwm", false)

        binding.apply {
            rvAddress.startAnimation(AnimationUtils.loadAnimation(this@InvoiceActivity, R.anim.slide_in_right_bounce))
            nsvScrollBody.startAnimation(AnimationUtils.loadAnimation(this@InvoiceActivity, R.anim.slide_up))
        }

        Checkout.preload(applicationContext)

        initRecyclerView(cartItems)
        initData(cartItems)
        setCartBottom()
        initLiveData()
        listeners()
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
        orderDetailsMap["deliveryPreference"] = binding.spDeliveryPreference.selectedItem
        orderDetailsMap["deliveryNote"] = binding.etDeliveryNote.text.toString().trim()
        orderDetailsMap["appliedCoupon"] = if (viewModel.couponPrice != null) {
            binding.etCoupon.text.toString().trim()
        } else {
            ""
        }
        viewModel.userProfile?.let { profile ->
            orderDetailsMap["userID"] = profile.id
            orderDetailsMap["name"] = profile.name
            orderDetailsMap["phoneNumber"] = profile.phNumber
            orderDetailsMap["address"] = profile.address[viewModel.checkedAddressPosition]
        }
        return orderDetailsMap
    }

    private fun initRecyclerView(cartItems: MutableList<CartEntity>) {
        val cartRecycler = findViewById<RecyclerView>(R.id.rvCart)

        addressAdapter = AddressAdapter(
            this,
            viewModel.checkedAddressPosition,
            arrayListOf(),
            this
        )
        binding.rvAddress.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvAddress.adapter = addressAdapter

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
                        val content = if(viewModel.isCWMCart) {
                            "Rs: ${viewModel.getCartPrice(viewModel.cwmDish)}"
                        } else {
                            "Rs: ${viewModel.getCartPrice(viewModel.totalCartItems)}"
                        }
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
                    if(viewModel.isCWMCart) {
                        viewModel.clearCart(viewModel.cwmDish)
                    } else {
                        viewModel.clearCart(viewModel.totalCartItems)
                    }
                }
            } else {
                Intent(this, WalletActivity::class.java).also { intent ->
                    intent.putExtra(NAVIGATION, CHECKOUT_PAGE)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                }
            }
        }

        cartBtn.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            if(cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
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
            showListBottomSheet(this, arrayListOf<String>("Online", "Wallet (Rs: ${viewModel.wallet?.amount})", "Cash On Delivery"))
        }
    }

    private fun setBottomSheetIcon(content: String) {
        val icon =  when(content) {
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

    @SuppressLint("NotifyDataSetChanged")
    private fun initLiveData() {
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

        viewModel.uiUpdate.observe(this) { event ->
            when (event) {
                is CheckoutViewModel.UiUpdate.WalletData -> {
                    //can get updated wallet data if needed
                }
                is CheckoutViewModel.UiUpdate.PopulateCartData -> {
                    event.cartItems?.let {
                        cartAdapter.setCartData(it as MutableList<CartEntity>)
                    } ?: showToast(this, "Looks like your cart is Empty")
                }
                is CheckoutViewModel.UiUpdate.UpdateCartData -> {
                    setDataToViews()
                    event.count?.let {
                        cartAdapter.updateItemsCount(event.position, event.count)
                    } ?: cartAdapter.deleteCartItem(event.position)
                }
                is CheckoutViewModel.UiUpdate.StartingTransaction -> {
                    showLoadStatusDialog("", "Processing payment from Wallet...", "transaction")
                }
                is CheckoutViewModel.UiUpdate.PlacingOrder -> {
                    updateLoadStatusDialogText("placingOrder", event.message)
                }
                is CheckoutViewModel.UiUpdate.OrderPlaced -> {
                    updateLoadStatusDialogText("success", event.message)
                    val cartItems = if (viewModel.isCWMCart) {
                        viewModel.cwmDish
                    } else {
                        viewModel.totalCartItems
                    }
                    lifecycleScope.launch {
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
                    addressAdapter.setAddressData(event.addressList)
                    setDataToViews()
                }
                is CheckoutViewModel.UiUpdate.AddressUpdate -> {
                    if (event.isSuccess) {
                        when (event.message) {
                            "update" -> {
                                showToast(this@InvoiceActivity, "Address Updated")
                                addressAdapter.updateAddress(event.position, event.address!!)
                            }
                            "add" -> {
                                showToast(this@InvoiceActivity, "Address added")
                                addressAdapter.addAddress(event.position, event.address!!)
                            }
                            "delete" -> {
                                showToast(this@InvoiceActivity, "Address Updated")
                                addressAdapter.deleteAddress(event.position)
                            }
                        }
                    } else {
                        showErrorSnackBar(event.message, true)
                    }
                }
                is CheckoutViewModel.UiUpdate.CouponApplied -> {
                    this.hideKeyboard()
                    showErrorSnackBar(event.message, false)
                    setDataToViews()
                    applyUiChangesWithCoupon(true)
                }
                is CheckoutViewModel.UiUpdate.CartCleared -> {
                    cartAdapter.emptyCart()
                    setDataToViews()
                    applyUiChangesWithCoupon(false)
                }
                is CheckoutViewModel.UiUpdate.Empty -> return@observe
                else -> Unit
            }
            viewModel.setEmptyStatus()
        }
    }

    private fun showLoadStatusDialog(title: String, body: String, content: String) {
        LoadStatusDialog.newInstance(title, body, content).show(supportFragmentManager,
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
                ivCouponInfo.visible()
                btnApplyCoupon.text = "Remove"
                btnApplyCoupon.setBackgroundColor(
                    ContextCompat.getColor(
                        baseContext,
                        R.color.matteRed
                    )
                )
            } else {
                viewModel.couponPrice = null
                viewModel.currentCoupon = null
                setDataToViews()
                etCoupon.setText("")
                etCoupon.enable()
                ivCouponInfo.remove()
                btnApplyCoupon.text = "Apply"
                btnApplyCoupon.setBackgroundColor(
                    ContextCompat.getColor(
                        baseContext,
                        R.color.green_base
                    )
                )
            }
        }
    }

    fun selectedPaymentMode(paymentMethod: String) {
        lifecycleScope.launch {
            val cartItems = if (viewModel.isCWMCart) {
                viewModel.cwmDish
            } else {
                viewModel.totalCartItems
            }
            when(paymentMethod) {
                "Online" -> {
                    val mrp = (viewModel.couponPrice ?: viewModel.getCartPrice(cartItems)) + viewModel.getDeliveryCharge()
                    viewModel.userProfile?.let {
                        startPayment(
                            this@InvoiceActivity,
                            mailID = it.mailId,
                            mrp  * 100f,
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
                    showSwipeConfirmationDialog(this@InvoiceActivity, "")
                }
                else -> {
                    val orderDetailsMap: HashMap<String, Any> = generateOrderDetailsMap()
                    viewModel.proceedForWalletPayment(
                        orderDetailsMap
                    )
                }
            }
        }
    }

    fun approved() {
        showLoadStatusDialog(
            "",
            "Validating your purchase...",
            "purchaseValidation"
        )
        viewModel.placeCashOnDeliveryOrder(generateOrderDetailsMap())
    }

    private fun setDataToViews() {
        val cartItems = if (viewModel.isCWMCart) {
            viewModel.cwmDish
        } else {
            viewModel.totalCartItems
        }
        var detailsJob: Job? = null
        detailsJob?.cancel()
        detailsJob = lifecycleScope.launch {
            delay(600)
            viewModel.currentCoupon?.let {
                viewModel.verifyCoupon(it.code, cartItems)
            }
            val cartPrice = if (viewModel.isCWMCart) {
                viewModel.getCartPrice(cartItems)
            } else {
                viewModel.getCartPrice(cartItems)
            }
            val cartOriginalPrice = viewModel.getCartOriginalPrice(cartItems)
            with(binding) {
                tvSavingsInCouponAmt.text = "${viewModel.couponPrice ?: 0f}"
                tvItemsOrderedCount.text = viewModel.getCartItemsQuantity(cartItems).toString()
                cartBtn.badgeValue = tvItemsOrderedCount.text.toString().toInt()
                tvMrpAmount.text = cartOriginalPrice.toString()
                tvSavingsInDiscountAmt.text = "${cartOriginalPrice - viewModel.getCartPrice(cartItems)}"
                tvDeliveryChargeAmt.text = viewModel.getDeliveryCharge().toString()
                val totalPrice = cartPrice + binding.tvDeliveryChargeAmt.text.toString().toFloat() - (viewModel.couponPrice ?: 0f)
                tvTotalAmt.setTextAnimation(totalPrice.toString())
                if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                    checkoutText.setTextAnimation("Rs: $cartPrice")
                }
            }
        }
        detailsJob = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listeners() {
        binding.apply {
//            etDeliveryNote.setOnTouchListener { _, _ ->
//                binding.nsvScrollBody.requestDisallowInterceptTouchEvent(true)
//                false
//            }
            KeyboardVisibilityEvent.setEventListener(this@InvoiceActivity
            ) { isOpen ->
                if (isOpen) {
                    cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
                } else {
                    cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            btnApplyCoupon.setOnClickListener {
                val couponCode: String = binding.etCoupon.text.toString().trim()
                if (couponCode.isNullOrEmpty()) {
                    showToast(this@InvoiceActivity, "Enter a coupon code")
                    return@setOnClickListener
                }
                viewModel.couponPrice?.let {
                    applyUiChangesWithCoupon(false)
                } ?: viewModel.verifyCoupon(etCoupon.text.toString().trim(), viewModel.totalCartItems)
            }
            ivCouponInfo.setOnClickListener {
                ivCouponInfo.startAnimation(AnimationUtils.loadAnimation(ivCouponInfo.context, R.anim.bounce))
                viewModel.currentCoupon?.let { coupon ->
                    val content = "Using ${coupon.name} Coupon with coupon code ${coupon.code} you can avail ${coupon.amount} ${coupon.type} discount on your total purchase. \n \n \n The Coupon can be used only for the following criteria: \n \n Minimum Purchase Amount: ${coupon.purchaseLimit} \n " +
                            "Maximum Discount Amount: ${coupon.maxDiscount}\n" +
                            "${coupon.description}"
                    showDescriptionBs(content)
                }
            }
        }

        binding.ivBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.ivCustomerSupport.setOnClickListener {
            Intent(this, ChatActivity::class.java).also {
                it.putExtra(NAVIGATION, CHECKOUT_PAGE)
                startActivity(it)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                finish()
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

    override fun onBackPressed() {
        if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
//            if (viewModel.navigateToPage == PRODUCTS) {
//                finish()
//            } else {
//                Intent(this, ShoppingMainActivity::class.java).also {
//                    it.putExtra(Constants.CATEGORY, viewModel.navigateToPage)
//                    startActivity(it)
//                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
//                    finish()
//                }
//            }
        }
    }

    override fun onDestroy() {
        viewModel.apply {
            userProfile = null
            wallet = null
            currentCoupon = null
        }
        super.onDestroy()
    }

    fun moveToCustomerSupport() {
        Intent(this, ChatActivity::class.java).also {
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
    //from address adapter
    override fun selectedAddress(position: Int) {
        viewModel.checkedAddressPosition = position
        addressAdapter.checkedAddressPosition = position
        addressAdapter.notifyDataSetChanged()
        setDataToViews()
    }

    override fun addAddress(position: Int) {
        AddressDialog().show(supportFragmentManager, "addressDialog")
    }

    override fun deleteAddress(position: Int) {
        viewModel.deleteAddress(position)
        viewModel.checkedAddressPosition = 0
        addressAdapter.checkedAddressPosition = 0
    }

    override fun updateAddress(position: Int) {
        viewModel.addressPosition = position
        viewModel.userProfile?.let {
            val dialog = AddressDialog()
            val bundle = Bundle()
            bundle.putParcelable("address", it.address[position])
            dialog.arguments = bundle
            dialog.show(supportFragmentManager, "addressDialog")
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
            if (isNew) {
                viewModel.addAddress(address)
            } else {
                viewModel.updateAddress(address)
            }
        }
    }
}