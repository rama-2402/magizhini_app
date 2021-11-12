package com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.AddressAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.CartAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CartEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CouponEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddressBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.gson.Gson
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.ChatAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.OrderItemsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TransactionHistory
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityCheckoutBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateTotalOrderItemService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.dialogs.ItemsBottomSheet
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.startPayment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect


class InvoiceActivity :
    BaseActivity(),
    KodeinAware,
    PaymentResultListener,
    AddressAdapter.OnAddressClickListener
{

    //TODO WRONG COUPON WHEN APPLIED AFTER RIGHT COUPON MAKES COUPON INFO WONKY

    override val kodein: Kodein by kodein()
    private val factory: CheckoutViewModelFactory by instance()
    private lateinit var binding: ActivityCheckoutBinding
    private lateinit var viewModel: CheckoutViewModel

    private lateinit var adapter: AddressAdapter
    private lateinit var mAddressBottomSheet: BottomSheetDialog
    private var cartBottomSheet: BottomSheetBehavior<LinearLayout> = BottomSheetBehavior()
    private lateinit var checkoutText: TextView
    private lateinit var cartAdapter: CartAdapter
    private lateinit var orderItemsAdapter: OrderItemsAdapter

    private var mCartItems: List<CartEntity> = listOf()
    private var mCoupons: List<CouponEntity> = listOf()
    private var mCoupon: CouponEntity = CouponEntity()
    private var mCouponIndex: Int = 0
    private var mCurrentCoupon: String = ""
    private var mCheckedAddressPosition: Int = 0
    private var mSelectedAddress: Address = Address()
    private var mOrder: Order = Order()
    private var mProfile: UserProfileEntity = UserProfileEntity()
    private var mWallet: Wallet = Wallet()

    private var mDiscountedPrice: Float = 0F
    private var mPaymentPreference: String = "COD"
    private var mCurrentUserID: String = ""
    private var mTransactionID: String = "COD"
    private var mPhoneNumber: String = ""

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

        SharedPref(this).apply {
            mCurrentCoupon = getData(Constants.CURRENT_COUPON, Constants.STRING, "").toString()
            mCurrentUserID = getData(Constants.USER_ID, Constants.STRING, "").toString()
            mPhoneNumber = getData(Constants.PHONE_NUMBER, Constants.STRING, "").toString()
        }

        binding.apply {
            rvAddress.startAnimation(AnimationUtils.loadAnimation(this@InvoiceActivity, R.anim.slide_in_right_bounce))
            nsvScrollBody.startAnimation(AnimationUtils.loadAnimation(this@InvoiceActivity, R.anim.slide_up))
        }

        Checkout.preload(applicationContext)

        initRecyclerView()
        setCartBottom()
        iniLiveData()
        listeners()
    }

    override fun onPaymentSuccess(response: String?) {
        mTransactionID = response!!
        mOrder.isPaymentDone = true
        showSuccessDialog("","Placing Order... ","order")
        placeOrder()
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        mOrder.isPaymentDone = false
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }

    private fun checkExistingCoupon() {
        if (mCurrentCoupon.isNotEmpty()) {
            mCoupons.forEach {
                if (it.id == mCurrentCoupon) {
                    mCoupon = it
                    binding.etCoupon.setText(mCoupon.code)
                    setDataToViews()
                    setRemoveButton()
                }
            }
        } else {
            setDataToViews()
        }
    }

    private fun initRecyclerView() {
        adapter = AddressAdapter(
            this,
            mCheckedAddressPosition,
            arrayListOf(),
            this
        )
        binding.rvAddress.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvAddress.adapter = adapter
    }

    private fun setCartBottom() {
        val bottomSheet = findViewById<LinearLayout>(R.id.clBottomCart)
        val cartBtn = findViewById<ImageView>(R.id.ivCart)
        val checkoutBtn = findViewById<LinearLayout>(R.id.rlCheckOutBtn)
        val cartRecycler = findViewById<RecyclerView>(R.id.rvCart)
        checkoutText = findViewById(R.id.tvCheckOut)

        cartBottomSheet = BottomSheetBehavior.from(bottomSheet)

        cartAdapter = CartAdapter(
            this,
            arrayListOf(),
            viewModel
        )

        cartRecycler.layoutManager = LinearLayoutManager(this)
        cartRecycler.adapter = cartAdapter

        cartBottomSheet.isDraggable = true

        cartBottomSheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {

                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        checkoutText.text = "Rs: ${viewModel.getCartPrice(mCartItems)}"
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        checkoutText.text = "PLACE ORDER"
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })

        cartBtn.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(it.context, R.anim.bounce))
            cartBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
        }

        checkoutBtn.setOnClickListener {
            if (mCartItems.isEmpty()) {
                showErrorSnackBar("Add some Items to Cart to proceed", true)
            } else {
                if (mPaymentPreference == "COD") {
                    showSwipeConfirmationDialog(this, "swipe right to place order")
                } else {
                    showSwipeConfirmationDialog(this, "swipe right to make payment")
                }
            }
        }

    }

    private fun hideAddressBs() {
        mAddressBottomSheet.dismiss()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun iniLiveData() {
        viewModel.getAllCoupons(Constants.ACTIVE)
        viewModel.getUserProfileData()
        viewModel.getWallet(mCurrentUserID)

        //we are getting all the items in cart and creating an array of id's that contains the variant names
        //so in setting the recycler view, if the variant name is in that array then it will be considered as part of cart
        //and remaining variant names will be removed when displaying the item
        viewModel.getAllCartItems().observe(this, {
            mCartItems = it
            viewModel.itemsInCart = mCartItems
            cartAdapter.setCartData(mCartItems)

            setDataToViews()
            setCheckoutText()
        })

        //getting all the coupons
        viewModel.coupons.observe(this, {
            mCoupons = it
            checkExistingCoupon()
        })

        viewModel.couponIndex.observe(this, {
            //everytime a new coupon is applied the index changes and we observe it and
            //save it to the preference and apply the changes to the invoice accordingly
            mCouponIndex = it
            mCoupon = mCoupons[mCouponIndex]
            SharedPref(this).putData(Constants.CURRENT_COUPON, Constants.STRING, mCoupon.id)
            setDataToViews()
        })

        viewModel.profile.observe(this, {
            mProfile = it
            adapter.addressList = it.address
            mSelectedAddress = it.address[0]
            adapter.notifyDataSetChanged()
            setUpDeliveryChargeForTheLocation(mSelectedAddress)
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

    private fun checkPaymentMode() {
        when (mPaymentPreference) {
            "Online" -> {
                with(mProfile) {
                    startPayment(
                        this@InvoiceActivity,
                        mailId,
                        binding.tvTotalAmt.text.toString().toFloat() * 100,
                        name,
                        id,
                        phNumber
                    ).also { status ->
                        if (!status) {
                            Toast.makeText(
                                this@InvoiceActivity,
                                "Error in processing payment. Try Later ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            "COD" -> {
                mOrder.isPaymentDone = false
                showSuccessDialog("","Placing Order... ","order")
                placeOrder()
            }
            else -> {
                val amount = binding.tvTotalAmt.text.toString().toFloat()
                showSuccessDialog("", "Processing payment from Wallet...", "wallet")
                lifecycleScope.launch {
                    if (mWallet.amount > amount) {
                        withContext(Dispatchers.IO) {
                            viewModel.makeTransactionFromWallet(amount, mCurrentUserID, mOrder.orderId)
                        }
                    } else {
                        delay(1000)
                        hideSuccessDialog()
                        showErrorSnackBar("Insufficient balance in Wallet. Pick another payment method", true)
                    }
                }
            }
        }
    }

    private fun validatingTransactionBeforeOrder(id: String) = lifecycleScope.launch {
        mTransactionID = id
        mOrder.isPaymentDone = true
        delay(1500)
        hideSuccessDialog()
        showSuccessDialog("","Placing Order... ","order")
        placeOrder()
    }

    private fun placeOrder() {
        mOrder.apply {
            customerId = mCurrentUserID
            transactionID = mTransactionID
            cart = mCartItems
            purchaseDate = TimeUtil().getCurrentDate()
            paymentMethod = mPaymentPreference
            deliveryPreference =
                binding.spDeliveryPreference.selectedItem.toString()
            deliveryNote = binding.etDeliveryNote.text.toString().trim()
            appliedCoupon = mCurrentCoupon
            address = mSelectedAddress
            price = binding.tvTotalAmt.text.toString().toFloat()
            orderStatus = Constants.PENDING
            monthYear = "${TimeUtil().getMonth()}${TimeUtil().getYear()}"
            phoneNumber = mPhoneNumber
        }
        startWorkerThread(mOrder)
        viewModel.placeOrder(mOrder)
    }

    fun approved(status: Boolean) = lifecycleScope.launch {
        val orderID = async { viewModel.generateOrderID() }
        mOrder.orderId = orderID.await()
        delay(250)
        withContext(Dispatchers.IO) {
            viewModel.validateItemAvailability(mCartItems)
        }
    }

    private fun startWorkerThread(order: Order) {
        val stringConvertedOrder = order.toStringConverter(order)
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateTotalOrderItemService>()
                .setInputData(workDataOf(
                    "order" to stringConvertedOrder
                ))
                .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun Order.toStringConverter(order: Order): String {
        return Gson().toJson(order)
    }

    private fun setUpDeliveryChargeForTheLocation(address: Address) = lifecycleScope.launch (Dispatchers.IO) {
        val getAreaCodeJob = async { viewModel.getDeliveryChargeForTheLocation(address.LocationCode) }
        val areaCode = getAreaCodeJob.await()
        withContext(Dispatchers.Main) {
            binding.tvDeliveryChargeAmt.text = areaCode.deliveryCharge.toString()
            setDataToViews()
        }
    }

    private fun setDataToViews() {
        val cartPrice = viewModel.getCartPrice(mCartItems)
       with(binding) {
            tvItemsOrderedCount.text = viewModel.getCartItemsQuantity(mCartItems).toString()
            tvMrpAmount.text = viewModel.getCartOriginalPrice(mCartItems).toString()
            mDiscountedPrice =
                viewModel.getCartOriginalPrice(mCartItems) - cartPrice
            tvSavingsInDiscountAmt.text = mDiscountedPrice.toString()
           tvTotalAmt.text = "${cartPrice - applyCouponDiscountToInvoice(cartPrice, mCoupon) + binding.tvDeliveryChargeAmt.text.toString().toFloat()}"
            if (etCoupon.text.toString().isEmpty()) {
                ivCouponInfo.remove()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listeners() {
        binding.apply {
            etDeliveryNote.setOnTouchListener { _, _ ->
                binding.nsvScrollBody.requestDisallowInterceptTouchEvent(true)
                false
            }
            btnApplyCoupon.setOnClickListener {
                val couponCode: String = binding.etCoupon.text.toString().trim()
                if (couponCode.isEmpty()) {
                    binding.etlCoupon.isErrorEnabled = false
                    showToast(this@InvoiceActivity, "Enter a valid coupon code")
                    return@setOnClickListener
                }
                if (binding.btnApplyCoupon.text == "Apply") {
                    if (viewModel.isCouponAvailable(mCoupons, couponCode)) {
                        binding.etlCoupon.isErrorEnabled = false
                        setRemoveButton()
                    } else {
                        binding.ivCouponInfo.remove()
                        binding.etlCoupon.error = "Coupon expired or does not exists. Check Again"
                        return@setOnClickListener
                    }
                } else if (binding.btnApplyCoupon.text == "Remove") {
                    removeCouponDiscountFromInvoice()
                    setDataToViews()
                    setAddButton()
                }
            }
            ivCouponInfo.setOnClickListener {
                ivCouponInfo.startAnimation(AnimationUtils.loadAnimation(ivCouponInfo.context, R.anim.bounce))
                val content = "\nThe Coupon code ${mCoupon.code} only applies for the following criteria \n \n Minimum Purchase Amount: ${mCoupon.purchaseLimit} \n " +
                        "Maximum Discount Amount: ${mCoupon.maxDiscount}\n" +
                        "${mCoupon.description}"
                showDescriptionBs(content)
            }
            rgPaymentType.setOnCheckedChangeListener { _, checkedId ->
                val selectedId = findViewById<RadioButton>(checkedId)
                mPaymentPreference = when(selectedId.text) {
                    " Online" -> "Online"
                    " COD" -> "COD"
                    else -> "Wallet"
                }
            }
        }

        binding.ivBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.ivWallet.setOnClickListener {
            Intent(this, WalletActivity::class.java).also {
                startActivity(it)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                finish()
            }
        }
    }

    //this will populate the add/edit address bottom sheet
    private fun showAddressBs(address: Address = Address()) {
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
                    if (address.userId.isEmpty()) {
                        viewModel.addAddress(mCurrentUserID, newAddress)
                    } else {
                        viewModel.updateAddress(newAddress)
                    }
                    hideAddressBs()
                }
            }
        }

        mAddressBottomSheet.setCancelable(true)
        mAddressBottomSheet.setCanceledOnTouchOutside(true)
        mAddressBottomSheet.setContentView(view.root)

        mAddressBottomSheet.show()

    }

    //this is setting the checkout btn text for the bottomsheet
    private fun setCheckoutText() {
        if (cartBottomSheet.state == BottomSheetBehavior.STATE_COLLAPSED) {
            checkoutText.text = "PLACE ORDER"
        } else {
            checkoutText.text = "Rs: ${viewModel.getCartPrice(mCartItems)}"
        }
    }

    //this will return a value after applying the coupon discount to the price of the cart items including the coupon and discounts
    private fun applyCouponDiscountToInvoice(cartPrice: Float, coupon: CouponEntity): Float {
        var couponDiscount: Float = 0f
        if(cartPrice > coupon.purchaseLimit.toFloat()) {
            with(binding) {
                //setting up the product discount info based on the discount type
                when (coupon.type) {
                    "percent" -> couponDiscount = (cartPrice * coupon.amount / 100)
                    "rupees" -> couponDiscount = coupon.amount
                }

                if (couponDiscount > coupon.maxDiscount) {
                    couponDiscount = coupon.maxDiscount
                }
                tvSavingsInCoupon.setTextColor(
                    ContextCompat.getColor(
                        this@InvoiceActivity,
                        R.color.green_base
                    )
                )
                tvSavingsInCouponAmt.text = couponDiscount.toString()
            }
            binding.ivCouponInfo.visible()
            return couponDiscount
        } else {
            if(!binding.etCoupon.text.isNullOrEmpty()) {
                binding.ivCouponInfo.visible()
                this.hideKeyboard()
                showErrorSnackBar("Coupon Not Applicable! Check Info", true)
            }
            return couponDiscount
        }
    }

    private fun removeCouponDiscountFromInvoice() {
        with(binding) {
            etCoupon.setText("")
            etlCoupon.isErrorEnabled = false
            tvSavingsInCoupon.setTextColor(
                ContextCompat.getColor(
                    this@InvoiceActivity,
                    R.color.black
                )
            )
            tvSavingsInCouponAmt.text = "0.00"
            mCoupon = CouponEntity()
            SharedPref(this@InvoiceActivity).putData(
                Constants.CURRENT_COUPON,
                Constants.STRING,
                ""
            )
            ivCouponInfo.remove()
        }
    }

    //this is to get the applied coupon details on app restart or activity restart
    private fun validateAddress(text: String?): Boolean {
        return text.isNullOrBlank()
    }

    private fun setAddButton() {
        with(binding) {
            btnApplyCoupon.text = "Apply"
            btnApplyCoupon.setBackgroundColor(
                ContextCompat.getColor(
                    baseContext,
                    R.color.green_base
                )
            )
            etCoupon.isEnabled = true
        }
    }

    private fun setRemoveButton() {
        with(binding) {
            btnApplyCoupon.text = "Remove"
            btnApplyCoupon.setBackgroundColor(
                ContextCompat.getColor(
                    baseContext,
                    R.color.matteRed
                )
            )
            etCoupon.isEnabled = false
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

    private fun validateItemAvailability(items: List<CartEntity>) {
        if (items.isNotEmpty()) {
            hideSuccessDialog()
            Toast.makeText(this@InvoiceActivity, "Items Out of Stock", Toast.LENGTH_LONG).show()
            ootItemsDialog(items)
        } else {
            viewModel.limitedItemsUpdater(mCartItems)
        }
    }

    private fun ootItemsDialog(outOfStockItems: List<CartEntity>) {
        orderItemsAdapter = OrderItemsAdapter(
            this,
            outOfStockItems,
            viewModel,
            arrayListOf(),
            "checkout"
        )
        hideSuccessDialog()
        ItemsBottomSheet(this, orderItemsAdapter).show()
    }

    override fun onBackPressed() {
        if (cartBottomSheet.state == BottomSheetBehavior.STATE_EXPANDED) {
            cartBottomSheet.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            Intent(this, ShoppingMainActivity::class.java).also {
                it.putExtra(Constants.CATEGORY, Constants.ALL_PRODUCTS)
                startActivity(it)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                finish()
            }
        }
    }

    private fun onSuccessCallback(message: String, data: Any?) {
        when(message) {
            "wallet" -> {
                data as Wallet
                mWallet = data
                binding.rbWallet.text = "Wallet (${data.amount})"
            }
            "ootItems" -> {
                data as List<CartEntity>
                validateItemAvailability(data)
            }
            "limitedItems" -> {
                lifecycleScope.launch {
                    delay(1500)
                    hideSuccessDialog()
                    checkPaymentMode()
                }
            }
            "orderPlacing" -> {
                lifecycleScope.launch {
                    delay(1500)
                        hideSuccessDialog()
                        showSuccessDialog("", "Order Placed Successfully!", "complete")
                        viewModel.clearCart(mCartItems)
                }
            }
            "orderPlaced" -> {
                lifecycleScope.launch {
                    removeCouponDiscountFromInvoice()
                    setAddButton()
                    delay(2000)
                    hideSuccessDialog()
                    Intent(this@InvoiceActivity, PurchaseHistoryActivity::class.java).also {
                        startActivity(it)
                        finish()
                    }
                }
            }
            "transaction" -> viewModel.updateTransaction(data as TransactionHistory)
            "transactionID" -> validatingTransactionBeforeOrder(data as String)
            "toast" -> {
                showToast(this, data as String)
            }
        }
    }

    private fun onFailedCallback(message: String, data: Any?) {
        when(message) {
            "wallet" -> showErrorSnackBar(data!! as String, true)
            "ootItems" -> {
                hideSuccessDialog()
                showErrorSnackBar(data!! as String, true)
            }
            "limitedItems" -> {
                hideSuccessDialog()
                showErrorSnackBar(data!! as String, true)
            }
            "orderPlacing" -> {
                hideSuccessDialog()
                showErrorSnackBar(data!! as String, true)
            }
            "orderPlaced" -> {
                hideSuccessDialog()
                showExitSheet(this, "Order Placed Successfully! \n \n There are some internal errors recorded while placing order. Please report this to customer support for further updates", "cs")
            }
            "transaction" -> {
                hideSuccessDialog()
                showErrorSnackBar(data!! as String, true)
            }
            "transactionID" -> {
                hideSuccessDialog()
                showExitSheet(this, "Server Error! Could not record wallet transaction. \n \n If Money is already debited from Wallet, Please contact customer support and the transaction will be reverted in 24 Hours", "cs")
            }
            "toast" -> {
                showToast(this, data as String)
            }
        }
    }

    fun moveToCustomerSupport() {
        Intent(this, ChatActivity::class.java).also {
            startActivity(it)
            finish()
        }
    }

    override fun selectedAddress(position: Int) {
        setUpDeliveryChargeForTheLocation(mSelectedAddress)
        mSelectedAddress = mProfile.address[position]
        mCheckedAddressPosition = position
        adapter.checkedAddressPosition = position
        adapter.notifyDataSetChanged()
    }

    override fun addAddress(position: Int) = showAddressBs()

    override fun deleteAddress(position: Int) {
        viewModel.deleteAddress(position)
        adapter.checkedAddressPosition = 0
        adapter.notifyDataSetChanged()
    }

    override fun updateAddress(position: Int) {
        viewModel.addressPosition = position
        showAddressBs(mProfile.address[position])
    }

}