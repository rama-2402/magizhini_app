package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import com.google.android.material.imageview.ShapeableImageView
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityNvfoodOrderBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.PreviewActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.CustomAlertClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.CustomAlertDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.LoadStatusDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SINGLE_DAY_LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.util.*

class NVFoodOrderActivity :
    BaseActivity(),
    PaymentResultListener,
    KodeinAware,
    CustomAlertClickListener
{

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityNvfoodOrderBinding
    private val factory: FoodSubscriptionViewModelFactory by instance()
    private lateinit var viewModel: FoodSubscriptionViewModel

    val month = SimpleDateFormat("MMMM - yyyy")
    var loop = true
    var isLunchTimeEnd: Boolean = false
    var isDinnerTimeEnd: Boolean = false
    var deliveryMealNumber: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_nvfood_order)
        viewModel = ViewModelProvider(this, factory)[FoodSubscriptionViewModel::class.java]

//        viewModel.lunchMap = intent.extras!!.get("lunch") as HashMap<String, Double>
//        viewModel.lunchPrice = intent.getDoubleExtra("lunch", 118.0)
//        viewModel.dinnerPrice = intent.getDoubleExtra("dinner", 98.0)
//        viewModel.lunchWoRicePrice = intent.getDoubleExtra("lunchWoRice", 100.0)

        initData()
        initLiveData()
        initListeners()
    }

    private fun initData() {
        binding.apply {
            showProgressDialog(true)
            viewModel.getProfile()

            checkTimeLimit()

            viewModel.ammaSpecials = Converters().stringToMenuConverter(intent.getStringExtra("menu")!!)

            calendarView.setUseThreeLetterAbbreviation(true)
            calendarView.shouldDrawIndicatorsBelowSelectedDays(true)
            calendarView.shouldScrollMonth(true)

            tvMonth.text = month.format(System.currentTimeMillis())

            tvLunch.text = viewModel.ammaSpecials[0].name
            tvLunchWithoutRice.text = viewModel.ammaSpecials[1].name
//            tvDinner.text = viewModel.ammaSpecials[2].name
//            tvDD1.text = "${viewModel.ammaSpecials[3].name}"
//            tvDD2.text = "${viewModel.ammaSpecials[4].name}"

            tvRenewal.isSelected = true
            tvTimeLimit.isSelected = true
            tvLunch.isSelected = true
            tvLunchWithoutRice.isSelected = true
            tvVD1.isSelected = true
            tvVD2.isSelected = true
            tvVD3.isSelected = true
            tvVD4.isSelected = true

            getListOfSundays(calendarView.firstDayOfCurrentMonth.time)
        }
    }

    override fun onPaymentSuccess(response: String?) {
        val orderDetailsMap: HashMap<String, Any> = generateOrderDetailsMap()
        orderDetailsMap["transactionID"] = response!!
        viewModel.placeOrderOnlinePayment(orderDetailsMap)
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }

    fun approved(status: Boolean) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        viewModel.placeOrderWalletPayment(generateOrderDetailsMap())
    }

    private fun initListeners() {
        binding.apply {
            KeyboardVisibilityEvent.setEventListener(
                this@NVFoodOrderActivity
            ) { isOpen ->
                if (isOpen) {
                    llPlaceOrder.remove()
                } else {
                    llPlaceOrder.visible()
                }
            }
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            ivHistory.setOnClickListener {
                navigateToOtherPage("")
            }
//            cbxLeaf.setOnClickListener {
//                setPrice()
//            }
            ivPrevMonth.setOnClickListener {
//                if (tvMonth.text == month.format(System.currentTimeMillis())) {
//                    showToast(this@FoodOrderActivity, "Already in current month")
//                } else {
                    calendarView.scrollLeft()
//                    ivPrevMonth.setColor(R.color.green_light)
//                    ivNextMonth.setColor(R.color.green_base)
//                }
            }
            ivNextMonth.setOnClickListener {
//                if (tvMonth.text == month.format(System.currentTimeMillis())) {
                    calendarView.scrollRight()
//                    ivNextMonth.setColor(R.color.green_light)
//                    ivPrevMonth.setColor(R.color.green_base)
//                } else {
//                    showToast(this@FoodOrderActivity, "Pre-Order not available for other months")
//                }
            }
            calendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
                override fun onDayClick(dateClicked: Date?) {

                    val instanceToGetLongDate = Calendar.getInstance()
                    instanceToGetLongDate.time = dateClicked!!

                    if (viewModel.nonDeliveryDatesString.contains(TimeUtil().getCustomDate(dateLong = instanceToGetLongDate.timeInMillis))) {
                        showToast(
                            this@NVFoodOrderActivity,
                            "Delivery not available on ${TimeUtil().getCustomDate(dateLong = instanceToGetLongDate.timeInMillis)}"
                        )
                        return
                    }

                    if (
                        TimeUtil().getDayName(instanceToGetLongDate.timeInMillis) == "Saturday" ||
                        TimeUtil().getDayName(instanceToGetLongDate.timeInMillis) == "Sunday"
                    ) {
                        showToast(
                            this@NVFoodOrderActivity,
                            "Delivery Not Available on Saturdays and Sundays"
                        )
                        return
                    }

                    if (
                        instanceToGetLongDate.timeInMillis < System.currentTimeMillis() &&
                        TimeUtil().getCustomDate(dateLong = instanceToGetLongDate.timeInMillis) != TimeUtil().getCurrentDate()
                    ) {
                        showToast(this@NVFoodOrderActivity, "Please pick a valid date")
                        return
                    }

                    when (viewModel.currentSubOption) {
//                        "month" -> {
//                            viewModel.selectedEventDates.clear()
//                            calendarView.removeAllEvents()
//                            populateMonthEvents(instanceToGetLongDate.timeInMillis)
//                        }
                        "custom" -> {
                            if (viewModel.selectedEventDates.contains(instanceToGetLongDate.timeInMillis)) {
                                removeEvent(instanceToGetLongDate.timeInMillis)
                            } else {
                                addEvent(instanceToGetLongDate.timeInMillis)
                            }
                        }
                        else -> {
                            if (viewModel.selectedEventDates.isNotEmpty()) {
                                removeEvent(viewModel.selectedEventDates[0])
                            }
                            viewModel.selectedEventDates.clear()
                            addEvent(instanceToGetLongDate.timeInMillis)
                        }
                    }
                    populateNonDeliveryDates(viewModel.nonDeliveryDatesLong)

                    setPrice()
                }

                override fun onMonthScroll(firstDayOfNewMonth: Date?) {
                    tvMonth.text = month.format(firstDayOfNewMonth!!)
                    getListOfSundays(firstDayOfNewMonth.time)
                }
            })
            spSubOptions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    calendarView.removeAllEvents()
                    viewModel.selectedEventDates.clear()
                    viewModel.currentSubOption = when (position) {
                        0 -> "single"
//                        1 -> {
//                            populateMonthEvents(System.currentTimeMillis() + SINGLE_DAY_LONG)
//                            "month"
//                        }
                        else -> "custom"
                    }
                    setPrice()
                    getListOfSundays(calendarView.firstDayOfCurrentMonth.time)
                    viewModel.getNonDeliveryDays()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }
            ivAddLunch.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                binding.tvLunchCount.text = "${binding.tvLunchCount.text.toString().toInt() + 1}"
                setPrice()
            }
            ivAddLunchWORice.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                binding.tvLunchWORiceCount.text = "${binding.tvLunchWORiceCount.text.toString().toInt() + 1}"
                setPrice()
            }
            ivAddVD1.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                binding.tvVD1Count.text = "${binding.tvVD1Count.text.toString().toInt() + 1}"
                setPrice()
            }
            ivAddVD2.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                binding.tvVD2Count.text = "${binding.tvVD2Count.text.toString().toInt() + 1}"
                setPrice()
            }
            ivAddVD3.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                binding.tvVD3Count.text = "${binding.tvVD3Count.text.toString().toInt() + 1}"
                setPrice()
            }
            ivAddVD4.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                binding.tvVD4Count.text = "${binding.tvVD4Count.text.toString().toInt() + 1}"
                setPrice()
            }
            ivAddPlate.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                binding.tvPlateCount.text = "${binding.tvPlateCount.text.toString().toInt() + 1}"
                setPrice()
            }
            ivMinusLunch.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                if (binding.tvLunchCount.text.toString() == "0") {
                    return@setOnClickListener
                } else {
                    binding.tvLunchCount.text = "${binding.tvLunchCount.text.toString().toInt() - 1}"
                    setPrice()
                }
            }
            ivMinusLunchWORice.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                if (binding.tvLunchWORiceCount.text.toString() == "0") {
                    return@setOnClickListener
                } else {
                    binding.tvLunchWORiceCount.text = "${binding.tvLunchWORiceCount.text.toString().toInt() - 1}"
                    setPrice()
                }
            }
            ivMinusVD1.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                if (binding.tvVD1Count.text.toString() == "0") {
                    return@setOnClickListener
                } else {
                    binding.tvVD1Count.text = "${binding.tvVD1Count.text.toString().toInt() - 1}"
                    setPrice()
                }
            }
            ivMinusVD2.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                if (binding.tvVD2Count.text.toString() == "0") {
                    return@setOnClickListener
                } else {
                    binding.tvVD2Count.text =
                        "${binding.tvVD2Count.text.toString().toInt() - 1}"
                    setPrice()
                }
            }
            ivMinusVD3.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                if (binding.tvVD3Count.text.toString() == "0") {
                    return@setOnClickListener
                } else {
                    binding.tvVD3Count.text =
                        "${binding.tvVD3Count.text.toString().toInt() - 1}"
                    setPrice()
                }
            }
            ivMinusVD4.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                if (binding.tvVD4Count.text.toString() == "0") {
                    return@setOnClickListener
                } else {
                    binding.tvVD4Count.text =
                        "${binding.tvVD4Count.text.toString().toInt() - 1}"
                    setPrice()
                }
            }
            ivMinusPlate.setOnClickListener {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showToast(this@NVFoodOrderActivity, "Please pick a start date from calendar")
                    return@setOnClickListener
                }
                if (binding.tvPlateCount.text.toString() == "0") {
                    return@setOnClickListener
                } else {
                    binding.tvPlateCount.text = "${binding.tvPlateCount.text.toString().toInt() - 1}"
                    setPrice()
                }
            }
            ivLunchMenu.setOnClickListener {
                previewImage(viewModel.ammaSpecials[0].thumbnailUrl, ivLunchMenu)
            }
            ivLunchWORiceMenu.setOnClickListener {
                previewImage(viewModel.ammaSpecials[1].thumbnailUrl, ivLunchWORiceMenu)
            }
            ivVD1Menu.setOnClickListener {
                previewImage(viewModel.ammaSpecials[2].thumbnailUrl, ivVD1Menu)
            }
            ivVD2Menu.setOnClickListener {
                previewImage(viewModel.ammaSpecials[2].thumbnailUrl, ivVD2Menu)
            }
            ivVD3Menu.setOnClickListener {
                previewImage(viewModel.ammaSpecials[2].thumbnailUrl, ivVD3Menu)
            }
            ivVD4Menu.setOnClickListener {
                previewImage(viewModel.ammaSpecials[2].thumbnailUrl, ivVD4Menu)
            }
            btnApplyCoupon.setOnClickListener {
                if (btnApplyCoupon.text.toString() == "Apply") {
                    val couponCode: String = binding.etCoupon.text.toString().trim()
                    if (couponCode.isNullOrEmpty()) {
                        showToast(this@NVFoodOrderActivity, "Enter a coupon code")
                        return@setOnClickListener
                    }
                    viewModel.currentCoupon?.let {
                        applyUiChangesWithCoupon(true)
                    } ?: viewModel.verifyCoupon(
                        etCoupon.text.toString().trim(),
                        viewModel.totalPrice
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
            tvPlaceOrder.setOnClickListener {
                validateEntries()
            }
            tvInfo.setOnClickListener {
                requestToJoinCommunity()
            }
        }

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
        setPrice()
    }

    private fun validateEntries() {
        viewModel.deliveryCharge = 0.0
        binding.apply {
            if (viewModel.currentSubOption == "single" || viewModel.currentSubOption == "custom") {
                if (viewModel.selectedEventDates.isEmpty()) {
                    showErrorSnackBar(
                        "Select delivery dates in the calendar to place order",
                        true
                    )
                    return
                }
            }
            if (TimeUtil().getCustomDate(dateLong = viewModel.selectedEventDates.min()) == TimeUtil().getCurrentDate()) {
                when {
                    binding.tvLunchWORiceCount.text.toString() != "0" -> {
                        if (isLunchTimeEnd) {
                            showErrorSnackBar("Today's order intake for lunch is closed. You can select from Tomorrow", true)
                            return@apply
                        }
                    }
                    binding.tvLunchCount.text.toString() != "0" -> {
                        if (isLunchTimeEnd) {
                            showErrorSnackBar("Today's order intake for lunch is closed. You can select from Tomorrow", true)
                            return@apply
                        }
                    }
                    binding.tvVD1Count.text.toString() != "0" -> {
                        if (isDinnerTimeEnd) {
                            showErrorSnackBar("Today's order intake for dinner is closed. You can select from Tomorrow", true)
                            return@apply
                        }
                    }
                    binding.tvVD2Count.text.toString() != "0" -> {
                        if (isDinnerTimeEnd) {
                            showErrorSnackBar("Today's order intake for dinner is closed. You can select from Tomorrow", true)
                            return@apply
                        }
                    }
                    binding.tvVD3Count.text.toString() != "0" -> {
                        if (isDinnerTimeEnd) {
                            showErrorSnackBar("Today's order intake for dinner is closed. You can select from Tomorrow", true)
                            return@apply
                        }
                    }
//                    else -> {
//                        if (isLunchTimeEnd || isDinnerTimeEnd) {
//                            showErrorSnackBar("Today's order intake for lunch and dinner is closed. You can select from Tomorrow", true)
//                            return@apply
//                        }
//                    }
                }
            }
            when {
                etName.text.isNullOrEmpty() -> showErrorSnackBar(
                    "Please Enter a Valid Customer Name for contact",
                    true
                )
                etAlternateNumber.text.isNullOrEmpty() -> showErrorSnackBar(
                    "Please Enter a Valid Mobile Number",
                    true
                )
                etEmailId.text.isNullOrEmpty() -> showErrorSnackBar(
                    "Please Enter a Valid Email ID",
                    true
                )
                etAddressOne.text.isNullOrEmpty() -> showErrorSnackBar(
                    "Please Enter a Valid Address",
                    true
                )
                etAddressTwo.text.isNullOrEmpty() -> showErrorSnackBar(
                    "Please Enter a Valid Address",
                    true
                )
                etCity.text.isNullOrEmpty() -> showErrorSnackBar(
                    "Please Enter a Valid City Name",
                    true
                )
                etArea.text.isNullOrEmpty() -> showErrorSnackBar(
                    "Please Enter a Valid Area Code",
                    true
                )
                tvLunchCount.text.toString() == "0" &&
                tvLunchWORiceCount.text.toString() == "0" &&
                tvVD1Count.text.toString() == "0" &&
                tvVD2Count.text.toString() == "0" &&
                tvVD3Count.text.toString() == "0" ->
            {
                    showErrorSnackBar("Please pick the number of Order from Order Options", true)
                    return
                }

                else -> {
                    if (!NetworkHelper.isOnline(this@NVFoodOrderActivity)) {
                        showErrorSnackBar("Please check your Internet Connection", true)
                        return
                    }
                    lifecycleScope.launch {
                        deliveryMealNumber = 0
                        val deliveryCharge = viewModel.getDeliveryCharge(etArea.text.toString())
                        if (
                            tvLunchCount.text.toString() != "0" ||
                            tvLunchWORiceCount.text.toString() != "0"
                        ) {
                            viewModel.deliveryCharge = viewModel.deliveryCharge + (deliveryCharge * viewModel.selectedEventDates.size)
                            deliveryMealNumber += 1
                        }
                        if (
                            tvVD1Count.text.toString() != "0" ||
                            tvVD2Count.text.toString() != "0" ||
                            tvVD3Count.text.toString() != "0"
                        ) {
                            viewModel.deliveryCharge = viewModel.deliveryCharge + (deliveryCharge * viewModel.selectedEventDates.size)
                            deliveryMealNumber += 1
                        }
                        if (viewModel.deliveryCharge != 0.0) {
                             showExitSheet(
                                this@NVFoodOrderActivity,
                                "Total Delivery charge (Rs: ${(viewModel.deliveryCharge / deliveryMealNumber) / viewModel.selectedEventDates.size}) for ${viewModel.selectedEventDates.size} days is Rs:${viewModel.deliveryCharge} \n Separate delivery charge for Lunch and Dinner",
                                "order"
                            )
                        }
                   }
               }
            }
        }
    }

    fun placeOrder() {
        if (viewModel.userID == null || viewModel.userID == "") {
                    CustomAlertDialog(
                        this@NVFoodOrderActivity,
                        "User not Signed In",
                        "You have not Signed In to Magizhini Organics. To utilize the feature to fullest please consider Signing In. You can still subscribe to Amma Samayal (Non-Veg) Food via whatsapp. Click SIGN IN to login or SUBSCRIBE button to place subscription via whatsapp",
                        "Sign In",
                        "food",
                        this@NVFoodOrderActivity
                    ).show()
                } else {
         showListBottomSheet(
            this@NVFoodOrderActivity,
            arrayListOf("Pay on Delivery", "Online", "Magizhini Wallet")
        )
        }
   }

    fun selectedPaymentMode(paymentMode: String) = lifecycleScope.launch {
        if (!NetworkHelper.isOnline(this@NVFoodOrderActivity)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return@launch
        }
        when (paymentMode) {
            "Online" -> {
            viewModel.userProfile?.let {
                startPayment(
                    this@NVFoodOrderActivity,
                    it.mailId,
                    ((viewModel.totalPrice + viewModel.deliveryCharge) * 100).toFloat(),
                    it.name,
                    it.id,
                    it.phNumber
                ).also { status ->
                    if (!status) {
                        Toast.makeText(
                            this@NVFoodOrderActivity,
                            "Error in processing payment. Try Later ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
            "Pay on Delivery" -> {
                showProgressDialog(true)
                val orderDetailsMap: HashMap<String, Any> = generateOrderDetailsMap()
                orderDetailsMap["transactionID"] = "COD"
                viewModel.placeOrderCOD(orderDetailsMap)
            }
            else -> {
            showProgressDialog(true)
            viewModel.fetchWallet()?.let {
                hideProgressDialog()
                if (it.amount < (viewModel.totalPrice + viewModel.deliveryCharge)) {
                    showErrorSnackBar("Insufficient Balance in Wallet.", true)
                    return@launch
                }
                showSwipeConfirmationDialog(this@NVFoodOrderActivity, "swipe right to make payment")
            } ?: showErrorSnackBar("Server Error! Failed to fetch Wallet", true)
        }
        }
    }

    private fun generateOrderDetailsMap(): HashMap<String, Any> {
        val orderDetailsMap: HashMap<String, Any> = hashMapOf()
        val orders = arrayListOf<String>()
        if (binding.tvLunchCount.text.toString() == "0") {
            orders.add("")
        } else {
            orders.add("Lunch With Rice - ${binding.tvLunchCount.text}")
        }
        if (binding.tvLunchWORiceCount.text.toString() == "0") {
            orders.add("")
        } else {
            orders.add("Lunch Without Rice - ${binding.tvLunchWORiceCount.text}")
        }
        if (binding.tvVD1Count.text.toString() == "0") {
            orders.add("")
        } else {
            orders.add("VD1 - ${binding.tvVD1Count.text}")
        }
        if (binding.tvVD2Count.text.toString() == "0") {
            orders.add("")
        } else {
            orders.add("VD2 - ${binding.tvVD2Count.text}")
        }
        if (binding.tvVD3Count.text.toString() == "0") {
            orders.add("")
        } else {
            orders.add("VD3 - ${binding.tvVD3Count.text}")
        }
        if (binding.tvVD4Count.text.toString() == "0") {
            orders.add("")
        } else {
            orders.add("Extra chicken gravy and egg - ${binding.tvVD4Count.text}")
        }
        binding.apply {
            orderDetailsMap["start"] = viewModel.selectedEventDates.min()
            orderDetailsMap["end"] = viewModel.selectedEventDates.max()
            orderDetailsMap["leaf"] = tvPlateCount.text.toString().toInt()
            orderDetailsMap["name"] = etName.text.toString().trim()
            orderDetailsMap["phoneNumber"] = etAlternateNumber.text.toString().trim()
            orderDetailsMap["mailID"] = etEmailId.text.toString().trim()
            orderDetailsMap["one"] = etAddressOne.text.toString().trim()
            orderDetailsMap["two"] = etAddressTwo.text.toString().trim()
            orderDetailsMap["city"] = etCity.text.toString().trim()
            orderDetailsMap["code"] = etArea.text.toString().trim()
            orderDetailsMap["orders"] = orders
        }
        return orderDetailsMap
    }

    private fun initLiveData() {
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
                is FoodSubscriptionViewModel.UiUpdate.PopulateUserProfile -> {
                    hideProgressDialog()
                    event.userProfile?.let { populateProfileData(it) }
                }
                is FoodSubscriptionViewModel.UiUpdate.PopulateNonDeliveryDates -> {
                    populateNonDeliveryDates(event.dates)
                }
                is FoodSubscriptionViewModel.UiUpdate.CreateStatusDialog -> {
                    LoadStatusDialog.newInstance("", "Creating Subscription...", "placingOrder")
                        .show(
                            supportFragmentManager,
                            Constants.LOAD_DIALOG
                        )
                }
                is FoodSubscriptionViewModel.UiUpdate.ValidatingTransaction -> {
                    updateLoadStatusDialog(event.message, event.data)
                }
                is FoodSubscriptionViewModel.UiUpdate.PlacingOrder -> {
                    updateLoadStatusDialog(event.message, event.data)
                }
                is FoodSubscriptionViewModel.UiUpdate.PlacedOrder -> {
                    updateLoadStatusDialog(event.message, event.data)
                }
                is FoodSubscriptionViewModel.UiUpdate.DismissStatusDialog -> {
                    hideProgressDialog()
                    (supportFragmentManager.findFragmentByTag(Constants.LOAD_DIALOG) as DialogFragment).dismiss()
                    if (event.dismiss) {
                        showExitSheet(
                            this,
                            "Food Subscription created Successfully! \n\n You can manager your subscriptions in Food Subscription History page. To open click PROCEED below. ",
                            "purchaseHistory"
                        )
                    } else {
                        showExitSheet(
                            this,
                            "Server Error! Something went wrong while creating your subscription. \n \n If Money is already debited, Please contact customer support and the transaction will be reverted in 24 Hours",
                            "cs"
                        )
                    }
                }
                 is FoodSubscriptionViewModel.UiUpdate.CouponApplied -> {
                    this.hideKeyboard()
                    if (event.message == "") {
                        applyUiChangesWithCoupon(false)
                    } else {
                        showErrorSnackBar(event.message, false)
                        applyUiChangesWithCoupon(true)
                    }
                }
                is FoodSubscriptionViewModel.UiUpdate.Empty -> return@observe
                else -> viewModel.setEmptyStatus()
            }
            viewModel.setEmptyStatus()
        }
    }

    private fun populateNonDeliveryDates(dates: List<Long>?) {
        dates?.forEach {
            Event(
                resources.getColor(
                    R.color.errorRed,
                    theme
                ), it, "leave"
            ).let { event ->
                if (!binding.calendarView.getEvents(it).isNullOrEmpty()) {
                    if (binding.calendarView.getEvents(it)[0].data == "leave") {
                        binding.calendarView.removeEvents(it)
                    }
                }
                binding.calendarView.addEvent(event)
            }
        }

        var today: Long = System.currentTimeMillis()
        var monthNum = 0
        while (true) {
            today += SINGLE_DAY_LONG
            if (
                TimeUtil().getDayName(dateLong = today) == "Saturday" ||
                TimeUtil().getDayName(dateLong = today) == "Sunday"
            ) {

                Event(
                    resources.getColor(
                        R.color.errorRed,
                        theme
                    ), today, "leave"
                ).let { event ->
                    if (!binding.calendarView.getEvents(today).isNullOrEmpty()) {
                        if (binding.calendarView.getEvents(today)[0].data == "leave") {
                            binding.calendarView.removeEvents(today)
                        }
                    }
                    binding.calendarView.addEvent(event)
                }
            }
            if (TimeUtil().getDateNumber(dateLong = today) == "01") {
                monthNum += 1
            }
            if (monthNum == 2) {
                return
            }
        }
    }

    private fun getListOfSundays(dayCount: Long) {
        var dayLong = dayCount
        for (count in 1..30) {
            if (
                TimeUtil().getDayName(dayLong) == "Saturday" ||
                TimeUtil().getDayName(dayLong) == "Sunday"
            ) {
                Event(
                    resources.getColor(
                        R.color.errorRed,
                        theme
                    ), dayLong, "leave"
                ).let { event ->
                    if (!binding.calendarView.getEvents(dayLong).isNullOrEmpty()) {
                        if (binding.calendarView.getEvents(dayLong)[0].data == "leave") {
                            binding.calendarView.removeEvents(dayLong)
                        }
                    }
                    binding.calendarView.addEvent(event)
                }
            }
            dayLong += SINGLE_DAY_LONG
        }
    }

    private fun removeEvent(time: Long) {
        Event(
            resources.getColor(
                R.color.green_base,
                theme
            ), time, "date"
        ).let { event ->
            binding.calendarView.removeEvent(event)
            viewModel.selectedEventDates.remove(time)
        }
    }

    private fun addEvent(time: Long) {
        Event(
            resources.getColor(
                R.color.green_base,
                theme
            ), time, "date"
        ).let { event ->
            binding.calendarView.addEvent(event)
            viewModel.selectedEventDates.add(time)
        }
    }

    private fun setPrice() {
        var totalPrice: Double = (binding.tvLunchCount.text.toString().toInt() * viewModel.ammaSpecials[0].price) +
                (binding.tvLunchWORiceCount.text.toString().toInt() * viewModel.ammaSpecials[1].price) +
                (binding.tvVD1Count.text.toString().toInt() * viewModel.ammaSpecials[2].price) +
                (binding.tvVD2Count.text.toString().toInt() * viewModel.ammaSpecials[2].price) +
                (binding.tvVD3Count.text.toString().toInt() * viewModel.ammaSpecials[2].price) +
                (binding.tvVD4Count.text.toString().toInt() * 65) +
        (binding.tvPlateCount.text.toString().toInt() * 9)

        totalPrice *= viewModel.selectedEventDates.size

        viewModel.currentCoupon?.let {
            viewModel.couponPrice = viewModel.couponDiscount(it, totalPrice)
            if (totalPrice < it.purchaseLimit) {
                showToast(this, "Coupon removed. Purchase value is less that minimum requirements.")
                applyUiChangesWithCoupon(false)
                return
            }
        }
//        totalPrice = (totalPrice * 118)/100  //GST calculation

        viewModel.totalPrice = viewModel.couponPrice?.let { totalPrice - it } ?: totalPrice
        binding.tvPlaceOrder.setTextAnimation("Place Order - Rs: ${viewModel.totalPrice}")
    }

    private fun populateProfileData(userProfile: UserProfileEntity) {
        viewModel.userID = userProfile.id
        binding.apply {
            etName.setText(if (userProfile.name.isNullOrEmpty()) "" else userProfile.name)
            etAlternateNumber.setText(if (userProfile.phNumber.isNullOrEmpty()) "" else userProfile.phNumber)
            etEmailId.setText(if (userProfile.mailId.isNullOrEmpty()) "" else userProfile.mailId)
            etAddressOne.setText(if (userProfile.address[0].addressLineOne.isNullOrEmpty()) "" else userProfile.address[0].addressLineOne)
            etAddressTwo.setText(if (userProfile.address[0].addressLineTwo.isNullOrEmpty()) "" else userProfile.address[0].addressLineTwo)
            etCity.setText(if (userProfile.address[0].city.isNullOrEmpty()) "" else userProfile.address[0].city)
            etArea.setText(if (userProfile.address[0].LocationCode.isNullOrEmpty()) "" else userProfile.address[0].LocationCode)
        }
    }

//    private fun populateMonthEvents(startDate: Long) {
//        var dayCount = 1
//        var currentDate: Long = startDate
//
//        while (dayCount <= 30) {
//            if (
//                TimeUtil().getDayName(currentDate) != "Saturday" &&
//                TimeUtil().getDayName(currentDate) != "Sunday" &&
//                !viewModel.nonDeliveryDatesString.contains(TimeUtil().getCustomDate(dateLong = currentDate))
//            ) {
//                addEvent(currentDate)
//            }
//
//            currentDate += SINGLE_DAY_LONG
//            dayCount += 1
//        }
//    }

    private fun checkTimeLimit() = lifecycleScope.launch(Dispatchers.IO) {
        while (loop) {
//         var min: Long = 0
            var lunchTime: Long = 0
            var dinnerTime: Long = 0
            try {
                val simpleDateFormat =
                    SimpleDateFormat("hh:mm aa") // for 12-hour system, hh should be used instead of HH
                // There is no minute different between the two, only 8 hours difference. We are not considering Date, So minute will always remain 0
                val date1: Date =
                    simpleDateFormat.parse(simpleDateFormat.format(System.currentTimeMillis()))
                val lunch: Date = simpleDateFormat.parse("08:00 AM")
                val dinner: Date = simpleDateFormat.parse("04:00 PM")

                lunchTime = (lunch.time - date1.time) / 1000
                dinnerTime = (dinner.time - date1.time) / 1000
                val lunchHours: Long = lunchTime % (24 * 3600) / 3600 // Calculating Hours
                val dinnerHours: Long = dinnerTime % (24 * 3600) / 3600 // Calculating Hours
                 val lunchMinute: Long =
                    lunchTime % 3600 / 60 // Calculating minutes if there is any minutes difference
                val dinnerMinute: Long =
                    dinnerTime % 3600 / 60 // Calculating minutes if there is any minutes difference
                withContext(Dispatchers.Main) {
                    binding.tvTimeLimit.text = when {
                        ((lunchMinute + lunchHours) * 60 > 0) && ((dinnerMinute + dinnerMinute) * 60 > 0) -> {
                            isLunchTimeEnd = false
                            isDinnerTimeEnd = false
                            "Today's order intake for lunch closing in $lunchHours Hour $lunchMinute Minutes and Dinner closing in $dinnerHours hours $dinnerMinute Minutes"
                        }
                         ((lunchMinute + lunchHours) * 60 < 0) && ((dinnerMinute + dinnerMinute) * 60 > 0) -> {
                             isLunchTimeEnd = true
                             isDinnerTimeEnd = false
                             "Today's order intake for lunch is closed and Dinner closing in $dinnerHours hours and $dinnerMinute minutes"
                         }
                        else -> {
                            isLunchTimeEnd = true
                            isDinnerTimeEnd = true
                            loop = false
                            "Order Intake closed for today. You can still place order for tomorrow"
                        }
                    }
//                    if ((minute + hours * 60) < 0) {
//                    } else {
//                        binding.tvTimeLimit.text =
//                            "Order Intake for tomorrow is closing in next $hours Hours and $minute Minutes"
//                    }
                }
                delay(60000)
//            min =
//                minute + (hours * 60) // This will be our final minutes. Multiplying by 60 as 1 hour contains 60 mins
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateLoadStatusDialog(message: String, data: String) {
        LoadStatusDialog.statusContent = message
        LoadStatusDialog.statusText.value = data
    }

    fun navigateToOtherPage(s: String) {
        Intent(this, FoodSubHistoryActivity::class.java).also {
            viewModel.apply {
                userProfile = null
                wallet = null
                ammaSpecials.clear()
                selectedEventDates.clear()
            }
            startActivity(it)
            finish()
        }
    }

    override fun onClick() {

    }

    override fun goToSignIn() {
        Intent(this, SignInActivity::class.java).also {
            it.putExtra("goto", "food")
            startActivity(it)
            finish()
        }
    }

    override fun placeOrderWithWhatsapp() {
        lifecycleScope.launch {
            val message: String = generateOrderDetailsForWhatsapp()
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

    private fun requestToJoinCommunity() {
         lifecycleScope.launch {
            val message: String = "Hi, I would like to join Amma Samayal (Non-Veg) Whatsapp Community!"
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

    private fun generateOrderDetailsForWhatsapp(): String {
        binding.apply {
            var deliveryDates: String = ""
            for (item in 0 until viewModel.selectedEventDates.size) {
                deliveryDates = if (item == 0) {
                    TimeUtil().getCustomDate(dateLong = viewModel.selectedEventDates[item])
                } else {
                    "$deliveryDates, ${TimeUtil().getCustomDate(dateLong = viewModel.selectedEventDates[item])}"
                }
            }
            var foodType: String = ""
            if (binding.tvLunchCount.text.toString() != "0") {
               foodType = "Lunch With Rice (${binding.tvLunchCount.text}),"
            }
            if (binding.tvLunchWORiceCount.text.toString() != "0") {
                foodType = "$foodType Lunch without rice (${binding.tvLunchWORiceCount.text}),"
            }
            if (binding.tvVD1Count.text.toString() != "0") {
              foodType = "$foodType VD1 - (${binding.tvVD1Count.text}),"
            }
            if (binding.tvVD2Count.text.toString() != "0") {
              foodType = "$foodType VD2 - (${binding.tvVD2Count.text})"
            }
            if (binding.tvVD3Count.text.toString() != "0") {
              foodType = "$foodType VD3 - (${binding.tvVD3Count.text})"
            }
            if (binding.tvVD4Count.text.toString() != "0") {
              foodType = "$foodType Extra chicken gravy with egg - (${binding.tvVD4Count.text})"
            }

            return "Subscription ID: ${System.currentTimeMillis()} \n" +
            "Customer Name: ${etName.text} \n" +
            "Contact Number: ${etAlternateNumber.text} \n" +
            "Address: ${etAddressOne.text}, ${etAddressTwo.text}, ${etCity.text}, ${etArea.text} \n" +
            "Email ID: ${etEmailId.text} \n" +
            "Subscription Status: New Subscription \n" +
            "Subscription Type: ${viewModel.currentSubOption} ${if (viewModel.currentSubOption == "custom") "- ${viewModel.selectedEventDates.size} days" else ""}\n" +
            "Food Type: $foodType \n" +
//            "No of Serving: ${viewModel.currentCountOption} \n" +
            "Paaku Mattai Plate Leaf: ${tvPlateCount.text} \n" +
                    "Delivery Charge: ${viewModel.deliveryCharge} \n" +
            "Total Price (incl delivery): ${viewModel.totalPrice + viewModel.deliveryCharge} \n" +
            "Start Date: ${TimeUtil().getCustomDate(dateLong = viewModel.selectedEventDates.min())} \n" +
            "End Date: ${TimeUtil().getCustomDate(dateLong = viewModel.selectedEventDates.max())} \n" +
            "Delivery Dates: $deliveryDates"
        }
    }

   private fun previewImage(url: String, thumbnail: ShapeableImageView) {
        Intent(this, PreviewActivity::class.java).also { intent ->
            intent.putExtra("url", url)
            intent.putExtra("contentType", "image")
            val options: ActivityOptionsCompat =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this, thumbnail, "thumbnail")
            startActivity(intent, options.toBundle())
//            startActivity(it)
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
}