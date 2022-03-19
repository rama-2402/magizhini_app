package com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.AddressAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.QuickOrderListAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.OrderEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Cart
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.QuickOrder
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityQuickOrderBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.AddressDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.LoadStatusDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.AddressDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LOAD_DIALOG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import ru.nikartm.support.ImageBadgeView

class QuickOrderActivity :
    BaseActivity(),
    KodeinAware,
    PaymentResultListener,
    AddressAdapter.OnAddressClickListener,
    QuickOrderListAdapter.QuickOrderClickListener,
    AddressDialogClickListener
{
    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityQuickOrderBinding

    private lateinit var viewModel: QuickOrderViewModel
    private val factory: QuickOrderViewModelFactory by instance()

    private var cartBottomSheet: BottomSheetBehavior<ConstraintLayout> = BottomSheetBehavior()
    private lateinit var cartBtn: ImageBadgeView
    private lateinit var checkoutText: TextView
    private lateinit var filterBtn: ImageView

    private lateinit var cartAdapter: CartAdapter
    private lateinit var addressAdapter: AddressAdapter
    private lateinit var quickOrderListAdapter: QuickOrderListAdapter

    private var isPreviewOpened: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quick_order)
        viewModel = ViewModelProvider(this, factory).get(QuickOrderViewModel::class.java)

        Checkout.preload(applicationContext)

        initRecyclerView()
        initData()

        initObservers()
        initListeners()
    }

    private fun initData() {
        lifecycleScope.launchWhenCreated {
            viewModel.userProfile = UserProfileEntity()
            viewModel.addressContainer = Address()
        }
        viewModel.getAddress()
    }

    private fun initListeners() {
        binding.apply {
            /*
            * This is a keyboard listener that changes visibility of buttons when typing for coupon or description
            * */
            KeyboardVisibilityEvent.setEventListener(this@QuickOrderActivity
            ) { isOpen ->
                if (!isOpen) {
                    viewModel.quickOrder?:let {
                        binding.btnGetEstimate.visible()
                        binding.btnPlaceOrder.visible()
                    }
                } else {
                    binding.btnPlaceOrder.remove()
                    binding.btnGetEstimate.remove()
                }
            }
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            btnApplyCoupon.setOnClickListener {
                if (etCoupon.text.isNullOrEmpty()) {
                    showToast(this@QuickOrderActivity, "Enter a coupon code")
                    return@setOnClickListener
                }
                /*
                * checking if cart is empty so that we can determine if estimate is received
                * since estimate data is posted with cart items filled
                * */
                if (viewModel.quickOrder?.cart?.isEmpty() ?: true) {
                    etCoupon.setText("")
                    if (KeyboardVisibilityEvent.isKeyboardVisible(this@QuickOrderActivity)) {
                        this@QuickOrderActivity.hideKeyboard()
                    }
                    showErrorSnackBar("Coupon can be applied only after receiving Estimate Data", true)
                } else {
                    /*
                    * if couponAppliedPrice is not null that mean coupon is already applied. So it has to be removed
                    * */
                    viewModel.couponAppliedPrice?.let {
                        applyUiChangesWithCoupon(false)
                    } ?: viewModel.verifyCoupon(etCoupon.text.toString().trim())
                }
            }
            ivHelp.setOnClickListener {
                showDescriptionBs(getString(R.string.quick_order_description))
            }
            ivNotification.setOnClickListener {
                viewModel.quickOrder?.let {
                    if (it.note.isNotEmpty()) {
                        showDescriptionBs(it.note)
                        binding.ivNotification.badgeValue = 0
                    } else {
                        showToast(this@QuickOrderActivity, "No New Notification")
                    }
                }
            }
            nsvScrollBody.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                viewModel.quickOrder?.let {
                    when {
                        scrollY < oldScrollY -> cartBottomSheet.state =
                            BottomSheetBehavior.STATE_COLLAPSED
                        scrollY > oldScrollY -> cartBottomSheet.state =
                            BottomSheetBehavior.STATE_HIDDEN
                    }
                }
            })
            btnGetEstimate.setOnClickListener {
                /*
                * If there is no quick order and the uri is empty then nothing is selected
                * */
                if (viewModel.quickOrder == null && viewModel.orderListUri.isNullOrEmpty()) {
                    showErrorSnackBar("Please add your purchase list image to get Estimate", true)
                    return@setOnClickListener
                }
                showExitSheet(this@QuickOrderActivity, "To get Estimate price, Your List will be sent for validation and we will contact you with the price breakdown for each product and Total Order. Please click PROCEED below to start uploading order list.", "estimate")
            }
            btnPlaceOrder.setOnClickListener {
                /*
                * So if quick order is null and there is no uri means no pic is added yet. Hence for first pic to be uploaded
                * we click from place order button
                * */
                if (viewModel.quickOrder == null && viewModel.orderListUri.isNullOrEmpty()) {
                    if (PermissionsUtil.hasStoragePermission(this@QuickOrderActivity)) {
                        getAction.launch(pickImageIntent)
                    } else {
                        showExitSheet(this@QuickOrderActivity, "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
                    }
                    return@setOnClickListener
                }
                showListBottomSheet(this@QuickOrderActivity, arrayListOf<String>("Online", "Wallet (Rs: ${viewModel.wallet?.amount})", "Cash On Delivery"))
            }
        }
    }

    private fun initObservers() {
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
        viewModel.uiUpdate.observe(this@QuickOrderActivity) { event ->
            when (event) {
                is QuickOrderViewModel.UiUpdate.WalletData -> {
                    viewModel.wallet = event.wallet
                }
                is QuickOrderViewModel.UiUpdate.StartingTransaction -> {
                    showLoadStatusDialog("", "Processing payment from Wallet...", "transaction")
                }
                is QuickOrderViewModel.UiUpdate.PlacingOrder -> {
                    updateLoadStatusDialogText("placingOrder", event.message)

                }
                is QuickOrderViewModel.UiUpdate.OrderPlaced -> {
                    updateLoadStatusDialogText("success", event.message)
                    lifecycleScope.launch {
                        delay(1800)
                        dismissLoadStatusDialog()
                        if (viewModel.placeOrderByCOD) {
                            showExitSheet(
                                this@QuickOrderActivity,
                                "We have received your order. Once we verify all the product in the list, We will add all the products to your cart. You can track the progress from Purchase History",
                            "close"
                            )
                        }
                    }
                }
                is QuickOrderViewModel.UiUpdate.WalletTransactionFailed -> {
                    lifecycleScope.launch {
                        delay(1800)
                        dismissLoadStatusDialog()
                        showExitSheet(
                            this@QuickOrderActivity,
                            "Server Error! Transaction Failed. If you have already paid or money is deducted from wallet, Please contact customer support we will verify the transaction and refund the amount. Click CUSTOMER SUPPORT to open customer support",
                            "cs"
                        )
                    }
                }
                is QuickOrderViewModel.UiUpdate.OrderPlacementFailed -> {
                    viewModel.placeOrderByCOD = false
                    dismissLoadStatusDialog()
                    showExitSheet(
                        this,
                        "Server Error! Failed to place order. If you have already paid or money is deducted from wallet, Please contact customer support we will verify the transaction and refund the amount. Click CUSTOMER SUPPORT to open customer support",
                        "cs"
                        )
                }
                is QuickOrderViewModel.UiUpdate.BeginningUpload -> {
                    showLoadStatusDialog(
                        "",
                        "Starting to Upload your order List... Please wait",
                        "upload"
                    )
                }
                is QuickOrderViewModel.UiUpdate.UploadingImage -> {
                    updateLoadStatusDialogText("upload", event.message)
                }
                is QuickOrderViewModel.UiUpdate.UploadComplete -> {
                    /*
                    * This comes after the images are uploaded. So if the user chose COD then we move
                    * to COD method to place order with COD option. If COD is false that means the user has
                    * requested only for get estimate request
                    * */
                    if (viewModel.placeOrderByCOD) {
                        viewModel.placeCashOnDeliveryOrder(generateOrderDetailsMap())
                    } else {
                        updateLoadStatusDialogText("success", event.message)
                        lifecycleScope.launch {
                            delay(1800)
                            dismissLoadStatusDialog()
                            showExitSheet(
                                this@QuickOrderActivity,
                                "We have received your order estimate request. This might take some time. After verification we will contact you with price breakdown.",
                                "close"
                            )
                        }
                    }
                }
                is QuickOrderViewModel.UiUpdate.UploadFailed -> {
                    viewModel.placeOrderByCOD = false
                    dismissLoadStatusDialog()
                    showErrorSnackBar(event.message, true)
                }
                is QuickOrderViewModel.UiUpdate.DeletingImages -> {
                    showLoadStatusDialog(
                        "",
                        event.message,
                        "upload"
                    )
                }
                is QuickOrderViewModel.UiUpdate.DeletingQuickOrder -> {
                    /*
                    * This is the callback to delete quick order request from customer
                    * by using the delete icon in the expanded cart bottomsheet
                    * */
                    event.data?.let {
                        when(event.data) {
                            "order" -> updateLoadStatusDialogText("placingOrder", event.message)
                            "success" -> lifecycleScope.launch{
                                updateLoadStatusDialogText(
                                    "success",
                                    event.message
                                )
                                resetQuickOrderUI()
                                delay(1800)
                                dismissLoadStatusDialog()
                            }
                            else -> lifecycleScope.launch {
                                updateLoadStatusDialogText(
                                    "fail",
                                    event.message
                                )
                                delay(1800)
                                dismissLoadStatusDialog()
                            }
                        }
                    }
                }
                is QuickOrderViewModel.UiUpdate.ValidatingPurchase -> {
                    showLoadStatusDialog(
                        "",
                        "Validating Transaction...",
                    "purchaseValidation"
                    )
                }
                is QuickOrderViewModel.UiUpdate.AddressUpdate -> {
                    if (event.isSuccess) {
                        if (event.message == "update") {
                            showToast(this@QuickOrderActivity, "Address Updated")
                        }
                        populateAddressDetails(event.data as List<Address>)
                    } else {
                        showErrorSnackBar(event.message, true)
                    }
                }
                is QuickOrderViewModel.UiUpdate.EstimateData -> {
//                    isSuccess means there is estimate data available
                    if (event.isSuccess) {
                        event.data?.let {
                            //if data is not null there is estimate request available to be displayed
                            populateEstimateDetails(it)
                        } ?:let {
                            /*
                            * since null no previous request is available and we do the empty page animation
                            * we hide the cart bottom sheet
                            */
                            binding.clBody.visible()
                            binding.clBody.startAnimation(AnimationUtils.loadAnimation(this@QuickOrderActivity, R.anim.slide_up))
                            binding.rvAddress.visible()
                            binding.rvAddress.startAnimation(AnimationUtils.loadAnimation(this@QuickOrderActivity, R.anim.slide_in_right_bounce))
                            setCartBottom(null)
                            cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    } else {
                        showErrorSnackBar(event.message as String, true)
                    }
                }
                is QuickOrderViewModel.UiUpdate.PopulateOrderDetails -> {
                    populateOrderDetails(event.order)
                }
                is QuickOrderViewModel.UiUpdate.CouponApplied -> {
                    updateCheckoutText()
                    event.message?.let {
                        this.hideKeyboard()
                        showErrorSnackBar(it, false)
                        applyUiChangesWithCoupon(true)
                    } ?: applyUiChangesWithCoupon(false)
                }
                is QuickOrderViewModel.UiUpdate.UpdateCartData -> {
                    updateCheckoutText()
                    updateCartBadge()
                    event.count?.let {
                        cartAdapter.updateItemsCount(event.position, it)
                    } ?: cartAdapter.deleteCartItem(event.position)
                }
                is QuickOrderViewModel.UiUpdate.Empty -> return@observe
                else -> Unit
            }
            viewModel.setEmptyStatus()
        }
    }

    private fun resetQuickOrderUI() {
        binding.apply {
            applyUiChangesWithCoupon(false)
            etDeliveryNote.setText("")
            btnGetEstimate.visible()
            btnPlaceOrder.visible()
            quickOrderListAdapter.quickOrderList = listOf()
            quickOrderListAdapter.quickOrderListUrl = listOf()
            quickOrderListAdapter.notifyDataSetChanged()
            cartAdapter.emptyCart()
            cartBtn.badgeValue = 0
        }
        viewModel.apply {
            orderListUri.clear()
            quickOrder = null
            orderID = null
            placeOrderByCOD = false
            couponAppliedPrice = null
            appliedCoupon = null
        }
        updatePlaceOrderButton()
        cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
    }

    private fun initRecyclerView() {
        addressAdapter = AddressAdapter(
            this,
            viewModel.mCheckedAddressPosition,
            arrayListOf(),
            this
            )
        binding.rvAddress.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvAddress.adapter = addressAdapter

        quickOrderListAdapter = QuickOrderListAdapter(
            this,
            listOf(),
            listOf(),
            this
        )
        binding.rvOrderList.layoutManager =
            GridLayoutManager(this, 3)
        binding.rvOrderList.adapter = quickOrderListAdapter

        cartAdapter = CartAdapter(
            this,
            mutableListOf(),
            viewModel
        )
    }

    private fun setCartBottom(cartEntity: ArrayList<Cart>?) {
        val bottomSheet = findViewById<ConstraintLayout>(R.id.clBottomCart)
        filterBtn = findViewById<ImageView>(R.id.ivFilter)
        val checkoutBtn = findViewById<LinearLayout>(R.id.rlCheckOutBtn)
        val cartRecycler = findViewById<RecyclerView>(R.id.rvCart)
        checkoutText = findViewById<TextView>(R.id.tvCheckOut)

        cartBtn = findViewById(R.id.ivCart)

        cartBottomSheet = BottomSheetBehavior.from(bottomSheet)

        cartRecycler.layoutManager = LinearLayoutManager(this)
        cartAdapter.cartItems = if (cartEntity == null) {
            mutableListOf<CartEntity>()
        } else {
            cartEntity.map { it.toCartEntity() } as MutableList<CartEntity>
        }
        cartAdapter.notifyDataSetChanged()
        cartRecycler.adapter = cartAdapter

        filterBtn.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_wallet))

        updateCheckoutText()
        cartBottomSheet.isDraggable = true

        cartEntity?.let { cart ->
            cartBottomSheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    updateCheckoutText()
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })

            cartBtn.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                cartBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
            }

            filterBtn.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
                if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
                    cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                    if (cart.isNotEmpty() && checkoutText.text != "PURCHASE HISTORY") {
                        showExitSheet(this, "Do you wish to delete and Create a New Order", "delete")
                    }
                } else {
                    Intent(this, WalletActivity::class.java).also { intent ->
                        intent.putExtra(Constants.NAVIGATION, Constants.QUICK_ORDER)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    }
                }
            }

            checkoutBtn.setOnClickListener {
                if (NetworkHelper.isOnline(this)) {
                    when {
                        viewModel.quickOrder?.orderPlaced == true -> {
                            Intent(this, PurchaseHistoryActivity::class.java).also { intent ->
                                startActivity(intent)
                                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                                finish()
                            }
                        }
                        cart.isEmpty() -> {
                            showErrorSnackBar("Estimate not yet available. Please wait", true)
                            return@setOnClickListener
                        }
                        else -> showListBottomSheet(this@QuickOrderActivity, arrayListOf<String>("Online", "Wallet (Rs: ${viewModel.wallet?.amount})", "Cash On Delivery"))

                    }
                } else {
                    showErrorSnackBar("Please check network connection", true)
                }
            }
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

    private fun updateCheckoutText() {
        when(cartBottomSheet.state) {
            BottomSheetBehavior.STATE_COLLAPSED -> {
                if (viewModel.quickOrder?.orderPlaced ?: false) {
                    checkoutText.text = "PURCHASE HISTORY"
                } else {
                    checkoutText.setTextAnimation("CHECKOUT", 200)
                }
                setBottomSheetIcon("wallet")
            }
            BottomSheetBehavior.STATE_EXPANDED -> {
                lifecycleScope.launch {
                    if (viewModel.quickOrder?.orderPlaced ?: false) {
                        checkoutText.text = "PURCHASE HISTORY"
                    } else {
                        checkoutText.setTextAnimation(
                            "Rs: ${viewModel.couponAppliedPrice ?: viewModel.getTotalCartPrice()} + ${viewModel.getDeliveryCharge()}",
                            200
                        )
                        if (viewModel.quickOrder?.cart?.isNotEmpty() ?: false) {
                            setBottomSheetIcon("delete")
                        }
                    }
                }
            }
        }
    }

    private fun updateCartBadge() {
        viewModel.quickOrder?.let {
            cartBtn.badgeValue = viewModel.getCartItemsQuantity(it.cart)
        }
    }

    private fun showLoadStatusDialog(title: String, body: String, content: String) {
        LoadStatusDialog.newInstance(title, body, content).show(supportFragmentManager, LOAD_DIALOG)
    }

    private fun dismissLoadStatusDialog() {
        (supportFragmentManager.findFragmentByTag(LOAD_DIALOG) as? DialogFragment)?.dismiss()
    }

    private fun updateLoadStatusDialogText(filter: String, content: String) {
        LoadStatusDialog.statusContent = content
        LoadStatusDialog.statusText.value = filter
    }

    private fun populateAddressDetails(addresses: List<Address>) {
        addressAdapter.setAddressData(addresses)
    }

    private fun populateEstimateDetails(quickOrder: QuickOrder) {
        viewModel.quickOrder = quickOrder
        binding.apply {
            quickOrderListAdapter.quickOrderListUrl = quickOrder.imageUrl
            quickOrderListAdapter.notifyDataSetChanged()
            if (quickOrder.note.isNotEmpty()) {
                ivNotification.badgeValue = 1
            }
            setCartBottom(quickOrder.cart)
            updateCartBadge()
            clBody.visible()
            clBody.startAnimation(AnimationUtils.loadAnimation(this@QuickOrderActivity, R.anim.slide_up))
            rvAddress.visible()
            rvAddress.startAnimation(AnimationUtils.loadAnimation(this@QuickOrderActivity, R.anim.slide_in_right_bounce))
            btnGetEstimate.remove()
            btnPlaceOrder.remove()
        }
    }

    private fun populateOrderDetails(order: OrderEntity) {
        binding.apply {
            populateAddressDetails(arrayListOf(order.address))
            etDeliveryNote.setText(order.deliveryNote)
            etCoupon.setText(order.appliedCoupon)
            if (order.appliedCoupon.isNotEmpty()) {
                applyUiChangesWithCoupon(true)
            }
            btnApplyCoupon.disable()
            hideProgressDialog()
            val preferences = resources.getStringArray(R.array.delivery_preference_array)
            for (i in preferences.indices) {
                if (preferences[i] == order.deliveryPreference) {
                    spDeliveryPreference.setSelection(i)
                    return
                }
            }
        }
    }

    private fun applyUiChangesWithCoupon(isCouponApplied: Boolean) {
        binding.apply {
            if (isCouponApplied) {
                etCoupon.disable()
                btnApplyCoupon.text = "Remove"
                btnApplyCoupon.setBackgroundColor(
                    ContextCompat.getColor(
                        baseContext,
                        R.color.matteRed
                    )
                )
            } else {
                viewModel.couponAppliedPrice = null
                viewModel.appliedCoupon = null
                etCoupon.enable()
                etCoupon.setText("")
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

    private fun updatePlaceOrderButton() {
        if(
            viewModel.quickOrder == null &&
            viewModel.orderListUri.isNullOrEmpty()
        ) {
            binding.btnPlaceOrder.text = "Add List"
        } else {
            binding.btnPlaceOrder.text = "Place Order"
        }
    }

    private fun loadNewImage() {
        quickOrderListAdapter.quickOrderList = viewModel.orderListUri
        quickOrderListAdapter.notifyDataSetChanged()
        updatePlaceOrderButton()
    }

    fun selectedPaymentMode(paymentMethod: String) {
        lifecycleScope.launch {
            when(paymentMethod) {
                "Online" -> {
                    if (viewModel.quickOrder == null) {
                        showExitSheet(
                            this@QuickOrderActivity,
                            "Generate Estimate to get the Total Price for the Items in the list to pay online or choose Cash on Delivery to place order immediately",
                            "okay"
                        )
                        return@launch
                    }
                    val mrp = (viewModel.couponAppliedPrice ?: viewModel.getTotalCartPrice()) + viewModel.getDeliveryCharge()
                    viewModel.userProfile?.let {
                        startPayment(
                            this@QuickOrderActivity,
                            mailID = it.mailId,
                            mrp  * 100f,
                            name = it.name,
                            userID = it.id,
                            phoneNumber = it.phNumber
                        ).also { status ->
                            if (!status) {
                                showToast(this@QuickOrderActivity, "Error in processing payment")
                            }
                        }
                    }
                }
                "Cash On Delivery" -> {
                    viewModel.placeOrderByCOD = true
                    viewModel.quickOrder?.let {
                        showLoadStatusDialog(
                            "",
                            "Validating your purchase...",
                            "purchaseValidation"
                        )
                        viewModel.placeCashOnDeliveryOrder(generateOrderDetailsMap())
                    } ?: sendEstimateRequest()
                }
                else -> {
                    if (viewModel.quickOrder == null) {
                        showExitSheet(
                            this@QuickOrderActivity,
                            "Generate Estimate to get the Total Price for the Items in the list to pay online or choose Cash on Delivery to place order immediately",
                            "okay"
                        )
                        return@launch
                    }
                    val orderDetailsMap: HashMap<String, Any> = generateOrderDetailsMap()
                    viewModel.proceedForWalletPayment(
                        orderDetailsMap
                    )
                }
            }
        }
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
        orderDetailsMap["appliedCoupon"] = if (viewModel.couponAppliedPrice != null) {
            binding.etCoupon.text.toString().trim()
        } else {
            ""
        }
        viewModel.userProfile?.let { profile ->
            orderDetailsMap["userID"] = profile.id
            orderDetailsMap["name"] = profile.name
            orderDetailsMap["phoneNumber"] = profile.phNumber
            orderDetailsMap["address"] = profile.address[viewModel.mCheckedAddressPosition]
        }

        orderDetailsMap["orderID"] = viewModel.orderID ?: viewModel.quickOrder!!.orderID

        return orderDetailsMap
    }

    fun deleteQuickOrder() {
         viewModel.deleteQuickOrder()
    }

    fun sendEstimateRequest() {
        val imageExtensionList = mutableListOf<String>()
        for (uri in viewModel.orderListUri) {
            imageExtensionList.add(imageExtension(this@QuickOrderActivity, uri)!!)
        }

        viewModel.sendGetEstimateRequest (
            imageExtensionList
        )
    }

    fun moveToCustomerSupport() {
        Intent(this, QuickOrderActivity::class.java).also {
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private val getAction = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val newOrderImage = result.data?.data
        newOrderImage?.let {
            viewModel.addNewImageUri(newOrderImage)
            loadNewImage()
        }
    }

    fun proceedToRequestPermission() = PermissionsUtil.requestStoragePermissions(this)

    fun proceedToRequestManualPermission() = this.openAppSettingsIntent()

    override fun onBackPressed() {
        when {
            isPreviewOpened -> {
                cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
                binding.ivPreviewImage.startAnimation(
                    AnimationUtils.loadAnimation(this, R.anim.scale_small)
                )
                binding.ivPreviewImage.remove()
                isPreviewOpened = false
            }
            cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED ->
                cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
            else -> {
                Intent(this, HomeActivity::class.java).also {
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        viewModel.userProfile = null
        viewModel.quickOrder = null
        viewModel.addressContainer = null
        viewModel.wallet = null
        super.onDestroy()
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

    //from address adapter
    override fun selectedAddress(position: Int) {
        viewModel.mCheckedAddressPosition = position
        addressAdapter.checkedAddressPosition = position
        addressAdapter.notifyDataSetChanged()
//        updatePriceButton()
    }

    override fun addAddress(position: Int) {
        if (viewModel.quickOrder?.orderPlaced ?: false) {
            showErrorSnackBar("Can't edit Address. Order placed already", true)
            return
        }
        AddressDialog().show(supportFragmentManager, "addressDialog")
    }

    override fun deleteAddress(position: Int) {
        if (viewModel.quickOrder?.orderPlaced ?: false) {
            showErrorSnackBar("Can't edit Address. Order placed already", true)
            return
        }
        viewModel.deleteAddress(position)
        viewModel.mCheckedAddressPosition = 0
        addressAdapter.checkedAddressPosition = 0
        addressAdapter.notifyDataSetChanged()
    }

    override fun updateAddress(position: Int) {
        if (viewModel.quickOrder?.orderPlaced ?: false) {
            showErrorSnackBar("Can't edit Address. Order placed already", true)
            return
        }
        viewModel.addressPosition = position
        viewModel.userProfile?.let {
            val dialog = AddressDialog()
            val bundle = Bundle()
            bundle.putParcelable("address", it.address[position])
            dialog.arguments = bundle
            dialog.show(supportFragmentManager, "addressDialog")
        }
    }

    //from order list adapter
    override fun selectedListImage(position: Int, imageUri: Any) {
        cartBottomSheet.state = BottomSheetBehavior.STATE_HIDDEN
        binding.apply {
            isPreviewOpened = true
//            GlideLoader().loadUserPictureWithoutCrop(this@QuickOrderActivity, imageUri, ivPreviewImage)
            ivPreviewImage.startAnimation(
                AnimationUtils.loadAnimation(this@QuickOrderActivity, R.anim.scale_big)
            )
            ivPreviewImage.visible()
        }
    }

    override fun deleteListItem(position: Int, imageUri: Any) {
        viewModel.orderListUri.removeAt(position)
        loadNewImage()
    }

    override fun addImage() {
        if (PermissionsUtil.hasStoragePermission(this)) {
            getAction.launch(pickImageIntent)
        } else {
            showExitSheet(this, "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
        }
    }

    //from address dialog
    override fun savedAddress(addressMap: HashMap<String, Any>, isNew: Boolean) {
        viewModel.addressContainer?.let { address ->
            address.userId = addressMap["userId"].toString()
            address.addressLineOne = addressMap["addressLineOne"].toString()
            address.addressLineTwo = addressMap["addressLineTwo"].toString()
            address.LocationCode = addressMap["LocationCode"].toString()
            address.LocationCodePosition = addressMap["LocationCodePosition"].toString().toInt()
            address.city = addressMap["city"].toString()
            if (isNew) {
                viewModel.addAddress(address)
            } else {
                viewModel.updateAddress(address)
            }
        }
    }
}




