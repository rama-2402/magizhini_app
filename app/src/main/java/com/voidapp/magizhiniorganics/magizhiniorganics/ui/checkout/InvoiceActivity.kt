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
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.OrderItemsAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Order
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityCheckoutBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateTotalOrderItemService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.shoppingItems.ShoppingMainActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.dialogs.ItemsBottomSheet
import kotlinx.coroutines.*
import org.json.JSONObject


class InvoiceActivity : BaseActivity(), KodeinAware, PaymentResultListener {

//TODO PLACE WALLET ICON IN TOOLBAR
    //TODO ERROR MESSAGE FOR EMPTY CART BEFORE PLACING ORDER

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

    private var mDiscountedPrice: Float = 0F
    private var mPaymentPreference: String = "COD"
    private var mCurrentUserID: String = ""
    private var mTransactionID: String = ""

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

        mCurrentCoupon =
            SharedPref(this).getData(Constants.CURRENT_COUPON, Constants.STRING, "").toString()
        mCurrentUserID =
            SharedPref(this).getData(Constants.USER_ID, Constants.STRING, "").toString()

        with(binding) {
            rvAddress.startAnimation(AnimationUtils.loadAnimation(this@InvoiceActivity, R.anim.slide_in_right_bounce))
            nsvScrollBody.startAnimation(AnimationUtils.loadAnimation(this@InvoiceActivity, R.anim.slide_up))
        }

        Checkout.preload(applicationContext)

        initRecyclerView()
        setCartBottom()
        iniLiveData()
        listeners()
    }

    private fun startPayment() {
        /*
        *  You need to pass current activity in order to let Razorpay create CheckoutActivity
        * */
        val co = Checkout()

        try {
            val options = JSONObject()
            options.put("name","Razorpay Corp")
            options.put("description","Demoing Charges")
            //You can omit the image option to fetch the image from dashboard
            options.put("image","https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("theme.color", "#86C232");
            options.put("currency","INR");
//            options.put("order_id", "orderIDkjhasgdfkjahsdf");
            options.put("amount","100")//pass amount in currency subunits

//            val retryObj = JSONObject();
//            retryObj.put("enabled", true);
//            retryObj.put("max_count", 4);
//            options.put("retry", retryObj);

            val prefill = JSONObject()
            prefill.put("email","magizhiniorganics2018@gmail.com")  //this place should have customer name
            prefill.put("contact","7299827393")     //this place should have customer phone number

            options.put("prefill",prefill)
            co.open(this,options)
        }catch (e: Exception){
            Toast.makeText(this,"Error in payment: "+ e.message,Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(response: String?) {
        mTransactionID = response!!
        validateItemAvailability()
    }

    override fun onPaymentError(p0: Int, p1: String?) {
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
            viewModel
        )
        binding.rvAddress.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvAddress.adapter = adapter
        setWalletAmountRadioButton()
    }

    private fun setWalletAmountRadioButton() = lifecycleScope.launch(Dispatchers.IO) {
        val wallet = async { viewModel.getWallet() }
        withContext(Dispatchers.Main) {
            binding.rbWallet.text = "Wallet (${wallet.await()!!.amount})"
        }
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
            cartBottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
        }

        checkoutBtn.setOnClickListener {
            //TODO FUNCTION TO PLACE ORDER
            showSwipeConfirmationDialog(this)
//        calculateTotalPurchaseAmount()
        }

    }

    private fun hideAddressBs() {
        mAddressBottomSheet.dismiss()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun iniLiveData() {
        viewModel.getAddress()
        viewModel.getAllCoupons(Constants.ACTIVE)

        viewModel.addressList.observe(this, {
            adapter.addressList = it
            mSelectedAddress = it[0]
            adapter.notifyDataSetChanged()
            setUpDeliveryChargeForTheLocation(mSelectedAddress)
        })

        viewModel.address.observe(this, {
            showAddressBs(it)
        })

        //we are getting all the items in cart and creating an array of id's that contains the variant names
        //so in setting the recycler view, if the variant name is in that array then it will be considered as part of cart
        //and remaining variant names will be removed when displaying the item
        viewModel.getAllCartItems().observe(this, {
            mCartItems = it
            viewModel.itemsInCart = mCartItems
            val itemNames = arrayListOf<String>()
            mCartItems.forEach { cartItem ->
                itemNames.add(cartItem.variant)
            }

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

        viewModel.selectedAddress.observe(this, {
            mSelectedAddress = it
            setUpDeliveryChargeForTheLocation(mSelectedAddress)
        })

        viewModel.selectedAddressPosition.observe(this, {
            mCheckedAddressPosition = it
            adapter.checkedAddressPosition = mCheckedAddressPosition
            adapter.notifyDataSetChanged()
        })

        viewModel.orderPlacementFailed.observe(this, {
            hideSuccessDialog()
            showErrorSnackBar(it, true)
        })

        viewModel.limitedItemUpdateStatus.observe(this, {
            lifecycleScope.launch {
                delay(1500)
                withContext(Dispatchers.Main) {
                    hideSuccessDialog()
                    showSuccessDialog("","Placing Order... ","order")
                    when(it) {
                        "complete" -> {
                            with(mOrder) {
                                orderId = ""
                                customerId = mCurrentUserID
                                cart = mCartItems
                                purchaseDate = Time().getCurrentDate()
                                isPaymentDone =
                                    true   //todo we have to get the boolean data from transaction success
                                paymentMethod = mPaymentPreference
                                deliveryPreference =
                                    binding.spDeliveryPreference.selectedItem.toString()
                                deliveryNote = binding.etDeliveryNote.text.toString().trim()
                                appliedCoupon = mCurrentCoupon
                                address = mSelectedAddress
                                price = binding.tvTotalAmt.text.toString().toFloat()
                                orderStatus = Constants.PENDING
                                monthYear = "${Time().getMonth()}${Time().getYear()}"
                            }
                            startWorkerThread(mOrder)
                            viewModel.placeOrder(mOrder)
                        }
                    }
                }
            }
        })

        viewModel.orderCompleted.observe(this, {
            lifecycleScope.launch() {
                delay(1500)
                withContext(Dispatchers.Main) {
                    hideSuccessDialog()
                    showSuccessDialog("", "Order Placed Successfully!", "complete")
                    lifecycleScope.launch {
                        viewModel.clearCart(mCartItems)
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
            }
        })

        viewModel.addNewAddress.observe(this, {
            showAddressBs()
        })
    }

    fun approved(status: Boolean) = lifecycleScope.launch(Dispatchers.Main) {
        delay(250)
        startPayment()
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

    private fun setUpDeliveryChargeForTheLocation(address: Address) = lifecycleScope.launch {
        val code = async {
            withContext(Dispatchers.IO) {
                viewModel.getDeliveryChargeForTheLocation(address.LocationCode)
            }}
        binding.tvDeliveryChargeAmt.text = code.await().deliveryCharge.toString()
        setDataToViews()
    }

    private fun setDataToViews() {
        with(binding) {
            tvItemsOrderedCount.text = viewModel.getCartItemsQuantity(mCartItems).toString()
            tvMrpAmount.text = viewModel.getCartOriginalPrice(mCartItems).toString()
            mDiscountedPrice =
                viewModel.getCartOriginalPrice(mCartItems) - viewModel.getCartPrice(mCartItems)
            tvSavingsInDiscountAmt.text = mDiscountedPrice.toString()
            val totalAfterCouponDiscount = viewModel.getCartPrice(mCartItems)-applyCouponDiscountToInvoice(mCoupon) + binding.tvDeliveryChargeAmt.text.toString().toFloat()
            tvTotalAmt.text = totalAfterCouponDiscount.toString()
            if (etCoupon.text.toString().isEmpty()) {
                ivCouponInfo.gone()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listeners() {
        with(binding) {
            etDeliveryNote.setOnTouchListener { _, _ ->
                binding.nsvScrollBody.requestDisallowInterceptTouchEvent(true)
                false
            }
            btnApplyCoupon.setOnClickListener {
                val couponCode: String = binding.etCoupon.text.toString().trim()
                if (binding.btnApplyCoupon.text == "Apply") {
                    if (viewModel.isCouponAvailable(mCoupons, couponCode)) {
                        binding.etlCoupon.isErrorEnabled = false
//                        Toast.makeText(this@CheckoutActivity, "Coupon Applied", Toast.LENGTH_SHORT)
//                            .show()
                        setRemoveButton()
                    } else {
                        binding.etlCoupon.error = "Coupon expired or does not exists. Check Again"
                    }
                } else if (binding.btnApplyCoupon.text == "Remove") {
                    removeCouponDiscountFromInvoice()
                    setDataToViews()
                    setAddButton()
                }
            }
            ivCouponInfo.setOnClickListener {
                val content = "Minimum Purchase Amount: ${mCoupon.purchaseLimit} \n" +
                        "Maximum Discount Amount: ${mCoupon.maxDiscount}\n" +
                        "${mCoupon.description}"
                showDescriptionBs(content)
            }
            rgPaymentType.setOnCheckedChangeListener { _, checkedId ->
                val selectedId = findViewById<RadioButton>(checkedId)
                mPaymentPreference = when(selectedId.text) {
                    " UPI" -> "UPI"
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
    private fun applyCouponDiscountToInvoice(coupon: CouponEntity): Float {
        var couponDiscount: Float = 0f
        val cartPrice = viewModel.getCartPrice(mCartItems)
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
//            if(!binding.etCoupon.text.isNullOrEmpty()) {
//                showErrorSnackBar("Coupon Applied", false)
//            }
            binding.ivCouponInfo.show()
            return couponDiscount
        } else {
            if(!binding.etCoupon.text.isNullOrEmpty()) {
                binding.ivCouponInfo.show()
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
            ivCouponInfo.gone()
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

    private fun calculateTotalPurchaseAmount() {

        //TODO GET THE DELIVERY CHARGE AFTER IMPLEMENTING IN THE STORE APP
        //TODO IMPLEMENT LOGIC FOR UPI TRANSACTION AFTER GETTING THE MERCHANT UPI ID

//        showSwipeConfirmationDialog(this)



//        val GOOGLE_PAY_PACKAGE_NAME = "com.google.android.apps.nbu.paisa.user"
//        val GOOGLE_PAY_REQUEST_CODE = 123
//
//        val uri: Uri = Uri.Builder()
//            .scheme("upi")
//            .authority("pay")
//            .appendQueryParameter("pa", "9486598819@okbizaxis")
//            .appendQueryParameter("pn", "test")
//            .appendQueryParameter("mc", "")
//            .appendQueryParameter("tr", "12345")
//            .appendQueryParameter("tn", "test-transaction")
//            .appendQueryParameter("am", "1")
//            .appendQueryParameter("cu", "INR")
////            .appendQueryParameter("url", "your-transaction-url")
//            .build()
//        val intent = Intent(Intent.ACTION_VIEW)
//        intent.data = uri
//        val chooser: Intent = Intent.createChooser(intent, "pay with")
//        startActivityForResult(chooser,GOOGLE_PAY_REQUEST_CODE)

//        val easyUpiPayment = EasyUpiPayment(this) {
//            this.payeeVpa = "9486598819@okbizaxis"
//            this.payeeName = "test"
//            this.payeeMerchantCode = "12345"
//            this.transactionId = "T2020090212345"
//            this.transactionRefId = "T2020090212345"
//            this.description = "Description"
//            this.amount = "1.00"
//            PaymentApp.ALL
//        }
//        easyUpiPayment.startPayment()
//        easyUpiPayment.setPaymentStatusListener(object: PaymentStatusListener {
//            override fun onTransactionCancelled() {
//                Toast.makeText(this@CheckoutActivity, "payment failed", Toast.LENGTH_SHORT).show()
//            }
//
//            override fun onTransactionCompleted(transactionDetails: TransactionDetails) {
//                Toast.makeText(this@CheckoutActivity, "payment done", Toast.LENGTH_SHORT).show()
//            }
//        })
    }

    private fun validateItemAvailability() {
        showSuccessDialog("","Validating Purchase... ", "limited")
        lifecycleScope.launch (Dispatchers.IO) {
            val outOfStockItems = async { viewModel.validateItemAvailability(mCartItems) }
            withContext(Dispatchers.Main) {
                val items = outOfStockItems.await()
                if (items.isNotEmpty()) {
                    Toast.makeText(this@InvoiceActivity, "Items Out of Stock", Toast.LENGTH_LONG).show()
                    ootItemsDialog(items)
                } else {
                    viewModel.limitedItemsUpdater(mCartItems)
                }
            }
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


//    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode==123 && resultCode == RESULT_OK && data?.data !== null ) {
//            Toast.makeText(this, "payment done", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(this, "payment failed", Toast.LENGTH_SHORT).show()
//        }
//    }
}