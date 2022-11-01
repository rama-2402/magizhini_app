package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
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
import kotlin.collections.HashMap

class FoodOrderActivity :
    BaseActivity(),
    PaymentResultListener,
    KodeinAware,
    CustomAlertClickListener
{

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

//        viewModel.lunchMap = intent.extras!!.get("lunch") as HashMap<String, Double>

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
//            ivPrevMonth.setColor(R.color.green_light)
            ivMinusOnePerson.setColor(R.color.green_light)

            tvRenewal.isSelected = true
            tvTimeLimit.isSelected = true

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
                            this@FoodOrderActivity,
                            "Delivery not available on ${TimeUtil().getCustomDate(dateLong = instanceToGetLongDate.timeInMillis)}"
                        )
                        return
                    }

                    if (
                        TimeUtil().getDayName(instanceToGetLongDate.timeInMillis) == "Sunday"
                    ) {
                        showToast(
                            this@FoodOrderActivity,
                            "Delivery Not Available on Sundays"
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
                        1 -> {
                            populateMonthEvents(System.currentTimeMillis() + SINGLE_DAY_LONG)
                            "month"
                        }
                        else -> "custom"
                    }
                    setPrice()
                    getListOfSundays(calendarView.firstDayOfCurrentMonth.time)
                    viewModel.getNonDeliveryDays()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }
            spFoodOptions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    viewModel.currentServingOption = position
                    setPrice()
                    getListOfSundays(calendarView.firstDayOfCurrentMonth.time)
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
                    getListOfSundays(calendarView.firstDayOfCurrentMonth.time)
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
                    }
                    else -> {
                        tvPersonCount.setTextAnimation("Food For ${viewModel.currentCountOption} Persons")
                    }
                }
                setPrice()
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
                setPrice()
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
                viewModel.userID == null || viewModel.userID == "" -> {
                    CustomAlertDialog(
                        this@FoodOrderActivity,
                        "User not Signed In",
                        "You have not Signed In to Magizhini Organics. To utilize the feature to fullest please consider Signing In. You can still subscribe to Amma's Special Food via whatsapp. Click SIGN IN to login or SUBSCRIBE button to place subscription via whatsapp",
                        "Sign In",
                        "food",
                        this@FoodOrderActivity
                    ).show()
                }
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
            orderDetailsMap["start"] = viewModel.selectedEventDates.min()
            orderDetailsMap["end"] = viewModel.selectedEventDates.max()
            orderDetailsMap["leaf"] = cbxLeaf.isChecked
            orderDetailsMap["name"] = etName.text.toString().trim()
            orderDetailsMap["phoneNumber"] = etAlternateNumber.text.toString().trim()
            orderDetailsMap["mailID"] = etEmailId.text.toString().trim()
            orderDetailsMap["one"] = etAddressOne.text.toString().trim()
            orderDetailsMap["two"] = etAddressTwo.text.toString().trim()
            orderDetailsMap["city"] = etCity.text.toString().trim()
            orderDetailsMap["code"] = etArea.text.toString().trim()
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
            if (TimeUtil().getDayName(dayLong) == "Sunday") {
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
        var totalPrice: Double = when(viewModel.currentServingOption) {
                    0 -> {
                        viewModel.selectedEventDates.size * 98.0
                    }
                    1 -> {
                        viewModel.selectedEventDates.size * 89.0
                    }
                    else -> {
                        (viewModel.selectedEventDates.size * 98.0) + (viewModel.selectedEventDates.size * 89.0)
                    }
                }
            //we have to calculate the price based on if it is lunch or dinner or both for the total number of days selected by the user for delivery
//            totalPrice += viewModel.lunchMap[TimeUtil().getDayName(dateLong)] ?: 0.0
        totalPrice *= viewModel.currentCountOption

        if (binding.cbxLeaf.isChecked) {
            totalPrice += 5 * viewModel.selectedEventDates.size
        }

        totalPrice = (totalPrice * 118)/100

        viewModel.totalPrice = totalPrice
        binding.tvPlaceOrder.setTextAnimation("Order Box for Rs: $totalPrice (Incl 18% GST)")
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
                TimeUtil().getDayName(currentDate) != "Sunday" &&
                !viewModel.nonDeliveryDatesString.contains(TimeUtil().getCustomDate(dateLong = currentDate))
            ) {
                addEvent(currentDate)
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

            val foodType = when (viewModel.currentServingOption) {
                0 -> "Lunch Only"
                1 -> "Dinner Only"
                else -> "Lunch and Dinner"
            }

            return "Subscription ID: ${System.currentTimeMillis()} \n" +
            "Customer Name: ${etName.text} \n" +
            "Contact Number: ${etAlternateNumber.text} \n" +
            "Address: ${etAddressOne.text}, ${etAddressTwo.text}, ${etCity.text}, ${etArea.text} \n" +
            "Email ID: ${etEmailId.text} \n" +
            "Subscription Status: New Subscription \n" +
            "Subscription Type: ${viewModel.currentSubOption} ${if (viewModel.currentSubOption == "custom") "- ${viewModel.selectedEventDates.size} days" else ""}\n" +
            "Food Type: $foodType \n" +
            "No of Serving: ${viewModel.currentCountOption} \n" +
            "Banana Leaf: ${if (cbxLeaf.isChecked) "Yes" else "No"} \n" +
                    "Total Price: ${viewModel.totalPrice} \n" +
            "Start Date: ${TimeUtil().getCustomDate(dateLong = viewModel.selectedEventDates.min())} \n" +
            "End Date: ${TimeUtil().getCustomDate(dateLong = viewModel.selectedEventDates.max())} \n" +
            "Delivery Dates: $deliveryDates"
        }

//        orderDetailsMap["start"] = viewModel.selectedEventDates.min()
//        orderDetailsMap["end"] = viewModel.selectedEventDates.max()
//        orderDetailsMap["leaf"] = cbxLeaf.isChecked
//        orderDetailsMap["name"] = etName.text.toString().trim()
//        orderDetailsMap["phoneNumber"] = etAlternateNumber.text.toString().trim()
//        orderDetailsMap["mailID"] = etEmailId.text.toString().trim()
//        orderDetailsMap["one"] = etAddressOne.text.toString().trim()
//        orderDetailsMap["two"] = etAddressTwo.text.toString().trim()
//        orderDetailsMap["city"] = etCity.text.toString().trim()
//        orderDetailsMap["code"] = etArea.text.toString().trim()
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