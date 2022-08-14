package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityFoodOrderBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.LoadStatusDialog
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
import kotlin.collections.HashMap

class FoodOrderActivity :
    BaseActivity(),
    PaymentResultListener,
    KodeinAware {

    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivityFoodOrderBinding
    private val factory: FoodSubscriptionViewModelFactory by instance()
    private lateinit var viewModel: FoodSubscriptionViewModel

    val month = SimpleDateFormat("MMMM - yyyy")
    var loop = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_food_order)
        viewModel = ViewModelProvider(this, factory)[FoodSubscriptionViewModel::class.java]

        viewModel.lunchMap = intent.extras!!.get("lunch") as HashMap<String, Double>

        initData()
        initLiveData()
        initListeners()
    }

    private fun initData() {
        binding.apply {
            showProgressDialog(true)
            viewModel.getProfile()

            checkTimeLimit()

            calendarView.setUseThreeLetterAbbreviation(true)
            calendarView.shouldDrawIndicatorsBelowSelectedDays(true)
            calendarView.shouldScrollMonth(false)

            tvMonth.text = month.format(System.currentTimeMillis())
            ivPrevMonth.setColor(R.color.green_light)
            ivMinusOnePerson.setColor(R.color.green_light)

            tvRenewal.isSelected = true
            tvTimeLimit.isSelected = true
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
                this@FoodOrderActivity
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
            cbxLeaf.setOnClickListener {
                setPrice()
            }
            ivPrevMonth.setOnClickListener {
                if (tvMonth.text == month.format(System.currentTimeMillis())) {
                    showToast(this@FoodOrderActivity, "Already in current month")
                } else {
                    calendarView.scrollLeft()
                    ivPrevMonth.setColor(R.color.green_light)
                    ivNextMonth.setColor(R.color.green_base)
                }
            }
            ivNextMonth.setOnClickListener {
                if (tvMonth.text == month.format(System.currentTimeMillis())) {
                    calendarView.scrollRight()
                    ivNextMonth.setColor(R.color.green_light)
                    ivPrevMonth.setColor(R.color.green_base)
                } else {
                    showToast(this@FoodOrderActivity, "Pre-Order not available for other months")
                }
            }
            calendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
                override fun onDayClick(dateClicked: Date?) {

                    val instanceToGetLongDate = Calendar.getInstance()
                    instanceToGetLongDate.time = dateClicked!!

                    if (viewModel.nonDeliveryDatesString.contains(TimeUtil().getCustomDate(dateLong = instanceToGetLongDate.timeInMillis))) {
                        showToast(
                            this@FoodOrderActivity,
                            "Delivery not available on ${TimeUtil().getCustomDate(dateLong = instanceToGetLongDate.timeInMillis)}"
                        )
                        return
                    }

                    if (
                        TimeUtil().getDayName(instanceToGetLongDate.timeInMillis) == "Saturday" ||
                        TimeUtil().getDayName(instanceToGetLongDate.timeInMillis) == "Sunday"
                    ) {
                        showToast(
                            this@FoodOrderActivity,
                            "Delivery Not Available on Saturdays and Sundays"
                        )
                        return
                    }

                    if (instanceToGetLongDate.timeInMillis < System.currentTimeMillis()) {
                        showToast(this@FoodOrderActivity, "Delivery only available from tomorrow.")
                        return
                    }

                    when (viewModel.currentSubOption) {
                        "month" -> {
                            viewModel.selectedEventDates.clear()
                            calendarView.removeAllEvents()
                            populateMonthEvents(instanceToGetLongDate.timeInMillis)
                        }
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
                        1 -> {
                            populateMonthEvents(System.currentTimeMillis() + SINGLE_DAY_LONG)
                            "month"
                        }
                        else -> "custom"
                    }
                    setPrice()
                    viewModel.getNonDeliveryDays()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }
            spCountOptions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    viewModel.currentCountOption = when (position) {
                        0 -> {
                            tvPersonCount.setTextAnimation("Food For 1 Person")
                            ivMinusOnePerson.setColor(R.color.green_light)
                            1
                        }
                        1 -> {
                            tvPersonCount.setTextAnimation("Food For 2 Persons")
                            ivMinusOnePerson.setColor(R.color.green_base)
                            2
                        }
                        else -> {
                            tvPersonCount.setTextAnimation("Food For 3 Persons")
                            ivMinusOnePerson.setColor(R.color.green_base)
                            3
                        }
                    }
                    setPrice()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }
            ivAddOnePerson.setOnClickListener {
                if (viewModel.currentCountOption == 10) {
                    return@setOnClickListener
                }
                viewModel.currentCountOption += 1
                ivMinusOnePerson.setColor(R.color.green_base)
                when (viewModel.currentCountOption) {
                    1 -> spCountOptions.setSelection(0)
                    2 -> spCountOptions.setSelection(1)
                    3 -> {
                        viewModel.currentCountOption -= 1
                        spCountOptions.setSelection(2)
                    }
                    10 -> {
                        ivAddOnePerson.setColor(R.color.green_light)
                        tvPersonCount.setTextAnimation("Food For ${viewModel.currentCountOption} Persons")
                        setPrice()
                    }
                    else -> {
                        setPrice()
                        tvPersonCount.setTextAnimation("Food For ${viewModel.currentCountOption} Persons")
                    }
                }
            }
            ivMinusOnePerson.setOnClickListener {
                ivAddOnePerson.setColor(R.color.green_base)
                if (viewModel.currentCountOption == 1) {
                    return@setOnClickListener
                }
                viewModel.currentCountOption -= 1
                tvPersonCount.setTextAnimation("Food For ${viewModel.currentCountOption} Persons")
                when (viewModel.currentCountOption) {
                    1 -> {
                        spCountOptions.setSelection(0)
                        ivMinusOnePerson.setColor(R.color.green_light)
                    }
                    2 -> spCountOptions.setSelection(1)
                    3 -> spCountOptions.setSelection(2)
                    else -> setPrice()
                }
            }
            tvPlaceOrder.setOnClickListener {
                validateEntries()
            }
        }
    }

    private fun validateEntries() {
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
            when {
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
                else -> {
                    if (!NetworkHelper.isOnline(this@FoodOrderActivity)) {
                        showErrorSnackBar("Please check your Internet Connection", true)
                        return
                    }
                    showListBottomSheet(
                        this@FoodOrderActivity,
                        arrayListOf("Online", "Magizhini Wallet")
                    )
                }
            }
        }
    }

    fun selectedPaymentMode(paymentMode: String) = lifecycleScope.launch {
        if (!NetworkHelper.isOnline(this@FoodOrderActivity)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return@launch
        }
        if (paymentMode == "Online") {
            viewModel.userProfile?.let {
                startPayment(
                    this@FoodOrderActivity,
                    it.mailId,
                    (viewModel.totalPrice * 100).toFloat(),
                    it.name,
                    it.id,
                    it.phNumber
                ).also { status ->
                    if (!status) {
                        Toast.makeText(
                            this@FoodOrderActivity,
                            "Error in processing payment. Try Later ",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            showProgressDialog(true)
            viewModel.fetchWallet()?.let {
                hideProgressDialog()
                if (it.amount < viewModel.totalPrice) {
                    showErrorSnackBar("Insufficient Balance in Wallet.", true)
                    return@launch
                }
                showSwipeConfirmationDialog(this@FoodOrderActivity, "swipe right to make payment")
            } ?: showErrorSnackBar("Server Error! Failed to fetch Wallet", true)
        }
    }

    private fun generateOrderDetailsMap(): HashMap<String, Any> {
        val orderDetailsMap: HashMap<String, Any> = hashMapOf()
        binding.apply {
//                showProgressDialog(true)
            orderDetailsMap["start"] = viewModel.selectedEventDates.min()
            orderDetailsMap["end"] = viewModel.selectedEventDates.max()
            orderDetailsMap["leaf"] = cbxLeaf.isChecked
            orderDetailsMap["phoneNumber"] = etAlternateNumber.text.toString().trim()
            orderDetailsMap["mailID"] = etEmailId.text.toString().trim()
            orderDetailsMap["one"] = etAddressOne.text.toString().trim()
            orderDetailsMap["two"] = etAddressTwo.text.toString().trim()
            orderDetailsMap["city"] = etCity.text.toString().trim()
            orderDetailsMap["code"] = etArea.text.toString().trim()
//                viewModel.placeOrder(orderDetailsMap)
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
                    populateProfileData(event.userProfile)
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
                    (supportFragmentManager.findFragmentByTag(Constants.LOAD_DIALOG) as DialogFragment).dismiss()
                    if (event.dismiss) {
                        showExitSheet(
                            this,
                            "Food Subscription created Successfully! \n\n You can manager your subscriptions in Amma's Special Subscription History page. To open click PROCEED below. ",
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
                binding.calendarView.addEvent(event)
            }
        }

        var today: Long = System.currentTimeMillis()
        var monthNum = 0
        while (true) {
            today += SINGLE_DAY_LONG
            if (TimeUtil().getDayName(dateLong = today) == "Saturday" ||
                TimeUtil().getDayName(dateLong = today) == "Sunday"
            ) {

                Event(
                    resources.getColor(
                        R.color.errorRed,
                        theme
                    ), today, "leave"
                ).let { event ->
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
        var totalPrice = 0.0
        viewModel.selectedEventDates.forEach { dateLong ->
            totalPrice += viewModel.lunchMap[TimeUtil().getDayName(dateLong)] ?: 0.0
        }
        totalPrice *= viewModel.currentCountOption

        if (binding.cbxLeaf.isChecked) {
            totalPrice += 5 * viewModel.selectedEventDates.size
        }

        viewModel.totalPrice = totalPrice
        binding.tvPlaceOrder.setTextAnimation("Order Box (Rs: $totalPrice)")
    }

    private fun populateProfileData(userProfile: UserProfileEntity) {
        binding.apply {
            etAlternateNumber.setText(userProfile.phNumber)
            etEmailId.setText(userProfile.mailId)
            etAddressOne.setText(userProfile.address[0].addressLineOne)
            etAddressTwo.setText(userProfile.address[0].addressLineTwo)
            etCity.setText(userProfile.address[0].city)
            etArea.setText(userProfile.address[0].LocationCode)
        }
    }

    private fun populateMonthEvents(startDate: Long) {
        var dayCount = 1
        var currentDate: Long = startDate

        while (dayCount <= 30) {
            if (
                TimeUtil().getDayName(currentDate) != "Saturday" &&
                TimeUtil().getDayName(currentDate) != "Sunday" &&
                !viewModel.nonDeliveryDatesString.contains(TimeUtil().getCustomDate(dateLong = currentDate))
            ) {
                addEvent(currentDate)
                viewModel.selectedEventDates.add(currentDate)
            }

            currentDate += SINGLE_DAY_LONG
            dayCount += 1
        }
    }

    private fun checkTimeLimit() = lifecycleScope.launch(Dispatchers.IO) {
        while (loop) {
//         var min: Long = 0
            var difference: Long = 0
            try {
                val simpleDateFormat =
                    SimpleDateFormat("hh:mm aa") // for 12-hour system, hh should be used instead of HH
                // There is no minute different between the two, only 8 hours difference. We are not considering Date, So minute will always remain 0
                val date1: Date =
                    simpleDateFormat.parse(simpleDateFormat.format(System.currentTimeMillis()))
                val date2: Date = simpleDateFormat.parse("11:59 PM")

                difference = (date2.time - date1.time) / 1000
                val hours: Long = difference % (24 * 3600) / 3600 // Calculating Hours
                val minute: Long =
                    difference % 3600 / 60 // Calculating minutes if there is any minutes difference
                withContext(Dispatchers.Main) {
                    if ((minute + hours * 60) < 0) {
                        binding.tvTimeLimit.text =
                            "Order Intake closed for today. You can still place order for tomorrow"
                        loop = false
                    } else {
                        binding.tvTimeLimit.text =
                            "Order Intake for tomorrow is closing in next $hours Hours and $minute Minutes"
                    }
                }
                delay(10000)
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