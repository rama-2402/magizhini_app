package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.FoodStatusAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.FoodStatusOnClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.AmmaSpecialOrder
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityFoodSubHistoryBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.CustomAlertClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.CustomAlertDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.LoadStatusDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.ADDRESS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LOAD_DIALOG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SINGLE_DAY_LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_ID
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Utils
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.startPayment
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.util.*

class FoodSubHistoryActivity :
    BaseActivity(),
    KodeinAware,
    FoodStatusOnClickListener,
    PaymentResultListener,
    CustomAlertClickListener {

    private val CALENDAR_VIEW: String = "calendar"
    private val FULL_VIEW: String = "full"

    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityFoodSubHistoryBinding
    private val factory: FoodSubscriptionViewModelFactory by instance()
    private lateinit var viewModel: FoodSubscriptionViewModel
    private lateinit var statusAdapter: FoodStatusAdapter

    private var date: Long = System.currentTimeMillis()
    private var currentView: String = FULL_VIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_food_sub_history)
        viewModel = ViewModelProvider(this, factory)[FoodSubscriptionViewModel::class.java]

        Checkout.preload(applicationContext)

        showProgressDialog(true)

        initData()
        initLiveData()
        initListeners()
    }

    private fun initListeners() {
        binding.apply {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }
            ivPrevMonth.setOnClickListener {
                calendarView.scrollLeft()
            }
            ivNextMonth.setOnClickListener {
                calendarView.scrollRight()
            }
            tvFoodStatusText.setOnClickListener {
                viewModel.selectedOrder = null
                statusAdapter.selectedPosition = null
                statusAdapter.notifyDataSetChanged()
                btnGoToBox.remove()
            }
            calendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
                override fun onDayClick(dateClicked: Date?) {
                    val instanceToGetLongDate = Calendar.getInstance()
                    instanceToGetLongDate.time = dateClicked!!

                    removeEvent(date)

                    date = instanceToGetLongDate.timeInMillis

                    if (
                        TimeUtil().getDayName(date) == "Saturday" ||
                        TimeUtil().getDayName(date) == "Sunday"
                    ) {
                        showToast(
                            this@FoodSubHistoryActivity,
                            "Delivery not available on Saturdays and Sundays"
                        )
                        rvFoodStatus.remove()
                        return
                    }

                    addEvent(date)

                    if (viewModel.ammaSpecialOrders.isNotEmpty()) {
                        showProgressDialog(true)
                        viewModel.getFoodStatus(date)
                    } else {
                        if (viewModel.userID.isNullOrEmpty()) {
                            userNotLoggedIn()
                        }
                    }
                }

                override fun onMonthScroll(firstDayOfNewMonth: Date?) {
                    val month = SimpleDateFormat("MMMM - yyyy")
                    tvMonth.text = month.format(firstDayOfNewMonth!!)
                    getListOfSundays(firstDayOfNewMonth.time)
                }
            })
            btnGoToBox.setOnClickListener {
                if (btnGoToBox.text == "OPEN MENU") {
                    showListBottomSheet(
                        this@FoodSubHistoryActivity,
                        arrayListOf("Amma Samayal (Veg)", "Amma Samayal (Non-Veg)"),
                        "menu"
                    )
                } else {
                    showExitSheet(
                        this@FoodSubHistoryActivity,
                        "You are about to cancel your Magizhini Amma Samayal Food Subscription. If you are unsubscribed your refund for the not delivered days will be reverted back in 3 to 5 business days. \n \nClick PROCEED to confirm cancellation",
                        "cancel"
                    )
                }
            }
//            btnRenewSub.setOnClickListener {
//                showExitSheet(
//                    this@FoodSubHistoryActivity,
//                    "You are about to renew your Monthly Subscription for Magizhini's Amma Samayal Authentic Home-made Food plan. \n \nClick PROCEED to renew your subscription for the next month",
//                    "renew"
//                )
//            }
            ivViewFilter.setOnClickListener {
                if (currentView == CALENDAR_VIEW) {
                    currentView = FULL_VIEW
                    ivViewFilter.setImageDrawable(
                        ContextCompat.getDrawable(
                            ivViewFilter.context,
                            R.drawable.ic_calendar
                        )
                    )
                    binding.tvToolbarTitle.text = "Pending..."
                    populateDeliveryStatusGeneralView(hashMapOf())
                    fabFilter.visible()
                } else {
                    currentView = CALENDAR_VIEW
                    ivViewFilter.setImageDrawable(
                        ContextCompat.getDrawable(
                            ivViewFilter.context,
                            R.drawable.ic_filter
                        )
                    )
                    binding.tvToolbarTitle.text = "Food Delivery Status"
                    calendarView.showCalendarWithAnimation()
                    ivPrevMonth.visible()
                    ivNextMonth.visible()
                    tvMonth.visible()
                    tvFoodStatusText.visible()
                    rvFoodStatus.remove()
                    fabFilter.remove()
                }
            }
            fabFilter.setOnClickListener {
                showListBottomSheet(
                    this@FoodSubHistoryActivity,
                    arrayListOf("Pending", "Delivered", "Cancelled", "Failed"),
                    "filter"
                )
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

    private fun initData() {
        binding.tvMonth.text = SimpleDateFormat("MMMM - yyyy").format(date)
        viewModel.userID =
            SharedPref(this).getData(USER_ID, STRING, "").toString()
        viewModel.getAmmaSpecialsOrderDetails(date)
        viewModel.getNonDeliveryDays()
        getListOfSundays(binding.calendarView.firstDayOfCurrentMonth.time)
        binding.apply {
            calendarView.setUseThreeLetterAbbreviation(true)
            calendarView.shouldDrawIndicatorsBelowSelectedDays(true)
            calendarView.shouldScrollMonth(true)
            tvToolbarTitle.text = "Pending..."
        }
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
                is FoodSubscriptionViewModel.UiUpdate.PopulateAmmaSpecials -> {
//                    viewModel.selectedOrder?.let {
//                        calculateRenewMonthDays(it.endDate).let { days ->
//                            CustomAlertDialog(
//                                this,
//                                "Monthly Subscription Renewal",
//                                "Your Subscription for Amma Samayal Authentic Home-made Food ends on ${
//                                    TimeUtil().getCustomDate(
//                                        dateLong = it.endDate
//                                    )
//                                }. Please renew your subscription to continue your daily delivery for the next month. \n \nYou are required to pay Rs: ${it.price} to renew your subscription for the next month. For further queries please contact customer support.",
//                                "RENEW SUBSCRIPTION",
//                                "food",
//                                this
//                            ).show()
//                        }
//                    }
                }
                is FoodSubscriptionViewModel.UiUpdate.UpdateFoodDeliveryStatus -> {
                    hideProgressDialog()
                    if (event.status == null) {
                        if (!event.isOrdersAvailable) {
                            statusNotAvailableUI()
                        } else {
                            populateDeliveryStatusGeneralView(hashMapOf())
                        }
                    } else {
                        populateDeliveryStatusCalendarView(event.status)
                    }

                    if (
                        TimeUtil().getDayName(date) == "Saturday" ||
                        TimeUtil().getDayName(date) == "Sunday"
                    ) {
                        showToast(
                            this@FoodSubHistoryActivity,
                            "Delivery not available on Saturdays and Sundays"
                        )
                    }
                }
                is FoodSubscriptionViewModel.UiUpdate.CancelOrderStatus -> {
                    hideProgressDialog()
                    if (event.status) {
                        showToast(this, event.message)
                    } else {
                        showToast(this, event.message, LONG)
                    }
                    viewModel.selectedOrder = null
                    viewModel.orderStatusMap = hashMapOf()
                    viewModel.getAmmaSpecialsOrderDetails(date)
                    statusAdapter.selectedPosition = null
                    statusAdapter.notifyDataSetChanged()
                    binding.btnGoToBox.remove()
                }
                is FoodSubscriptionViewModel.UiUpdate.PopulateNonDeliveryDates -> {
                    populateNonDeliveryDates(event.dates)
                }
                is FoodSubscriptionViewModel.UiUpdate.CancelSubscription -> {
                    hideProgressDialog()
                    if (event.status) {
                        showToast(this, event.message)
                    } else {
                        showToast(this, event.message, LONG)
                    }
                    viewModel.selectedOrder = null
                    viewModel.orderStatusMap = hashMapOf()
                    viewModel.getAmmaSpecialsOrderDetails(date)
                }
                is FoodSubscriptionViewModel.UiUpdate.CreateStatusDialog -> {
                    if (event.message == "wallet") {
                        LoadStatusDialog.newInstance("", "Fetching your wallet...", "placingOrder")
                            .show(
                                supportFragmentManager,
                                LOAD_DIALOG
                            )
                    } else {
                        LoadStatusDialog.newInstance(
                            "",
                            "Renewing your Subscription...",
                            "placingOrder"
                        ).show(
                            supportFragmentManager,
                            LOAD_DIALOG
                        )
                    }
                }
                is FoodSubscriptionViewModel.UiUpdate.ValidatingTransaction -> {
                    updateLoadStatusDialog(event.message, event.data)
                }
                is FoodSubscriptionViewModel.UiUpdate.PlacingOrder -> {
                    updateLoadStatusDialog(event.message, event.data)
                }
                is FoodSubscriptionViewModel.UiUpdate.PlacedOrder -> {
                    for (i in viewModel.ammaSpecialOrders.indices) {
                        viewModel.selectedOrder?.let {
                            if (viewModel.ammaSpecialOrders[i].id == it.id) {
                                viewModel.ammaSpecialOrders[i] = it
                            }
                        }
                    }
                    statusAdapter.orders = viewModel.ammaSpecialOrders
                    statusAdapter.notifyDataSetChanged()
                    updateLoadStatusDialog(event.message, event.data)
                }
                is FoodSubscriptionViewModel.UiUpdate.DismissStatusDialog -> {
                    (supportFragmentManager.findFragmentByTag(LOAD_DIALOG) as DialogFragment).dismiss()
                    if (event.dismiss) {
                        showToast(this, "Your Subscription has been renewed!")
                    } else {
                        showExitSheet(
                            this,
                            "Server Error! Something went wrong while creating your subscription. \n \n If Money is already debited, Please contact customer support and the transaction will be reverted in 24 Hours",
                            "cs"
                        )
                    }
                }
                is FoodSubscriptionViewModel.UiUpdate.Empty -> return@observe
                else -> Unit
            }
            viewModel.setEmptyStatus()
        }
    }

    private fun userNotLoggedIn() {
        SharedPref(this).getData(ADDRESS, STRING, "").let { address ->
            if (address != "") {
                viewModel.tempAddress = Utils.toAddressDataClass(address as String)
            }
            CustomAlertDialog(
                this,
                "User not Signed In",
                "You have not Signed In to Magizhini Organics. To utilize the feature to fullest please consider Signing In. You can still check the progress of your Food Subscription and makes changes to the order through whatsapp. Click SIGN IN to login",
                "Sign In",
                "whatsapp",
                this
            ).show()
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

    private fun updateLoadStatusDialog(message: String, data: String) {
        LoadStatusDialog.statusContent = message
        LoadStatusDialog.statusText.value = data
    }

    override fun onPaymentSuccess(response: String?) {
        response?.let {
            viewModel.renewSubWithOnlinePayment(it)
        }
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }

    private fun calculateRenewMonthDays(endDate: Long): Int {
        viewModel.selectedEventDates.clear()
        var currentDate: Long = endDate
        var dayCount = 1

        while (dayCount <= 30) {
            currentDate += SINGLE_DAY_LONG

            if (
                TimeUtil().getDayName(currentDate) != "Saturday" &&
                TimeUtil().getDayName(currentDate) != "Sunday" &&
                !viewModel.nonDeliveryDatesString.contains(TimeUtil().getCustomDate(dateLong = currentDate))
            ) {
                viewModel.selectedEventDates.add(currentDate)
            }

            dayCount += 1
        }

        return viewModel.selectedEventDates.size
    }

    private fun statusNotAvailableUI() {
        binding.apply {
            rvFoodStatus.remove()
            tvFoodStatus.visible()
            btnGoToBox.visible()
            btnRenewSub.remove()
            tvFoodStatus.text =
                "You don't have any food subscriptions yet 🍱 \n\n\nTo eat Healthy Authentic Home-Made Food made with 100% Organic materials please check out our different Lunch Boxes to place an order 😋😋😋"
            btnGoToBox.text = "OPEN MENU"
        }
    }

    private fun populateDeliveryStatusGeneralView(status: HashMap<String, String>) {
        binding.apply {
            if (viewModel.ammaSpecialOrders.isEmpty()) {
                statusNotAvailableUI()
                return
            }
            calendarView.hideCalendarWithAnimation()
            tvMonth.remove()
            ivPrevMonth.remove()
            ivNextMonth.remove()
            tvFoodStatusText.remove()
            tvFoodStatus.remove()
            rvFoodStatus.visible()
            btnGoToBox.remove()
            btnRenewSub.remove()
            FoodStatusAdapter(
                viewModel.ammaSpecialOrders.filter { it.status != "success" && it.status != "fail" && it.status != "cancel" },
                status,
                null,
                this@FoodSubHistoryActivity
            ).let {
                statusAdapter = it
                rvFoodStatus.adapter = it
                rvFoodStatus.layoutManager = LinearLayoutManager(this@FoodSubHistoryActivity)
            }
        }
    }

    private fun populateDeliveryStatusCalendarView(status: HashMap<String, String>) {
        binding.apply {
            if (viewModel.ammaSpecialOrders.isEmpty()) {
                statusNotAvailableUI()
                return
            }
            tvMonth.visible()
            ivPrevMonth.visible()
            ivNextMonth.visible()
            tvFoodStatusText.visible()
            tvFoodStatus.remove()
            rvFoodStatus.visible()
            btnGoToBox.remove()
            btnRenewSub.remove()
            FoodStatusAdapter(
                viewModel.ammaSpecialOrders.filter { it.endDate >= date && !status[it.id].isNullOrEmpty() },
                status,
                null,
                this@FoodSubHistoryActivity
            ).let {
                statusAdapter = it
                rvFoodStatus.adapter = it
                rvFoodStatus.layoutManager = LinearLayoutManager(this@FoodSubHistoryActivity)
            }
        }
    }

    fun cancelDeliveryConfirmed() {
        showProgressDialog(true)
        when {
            TimeUtil().getCurrentDate() == TimeUtil().getCustomDate(dateLong = date) -> {
                if (checkTimeLimit()) {
                    viewModel.cancelDeliveryOn(date, "yes")
                } else {
                    viewModel.cancelDeliveryOn(date, "no")
                }
            }
            else -> {
                viewModel.cancelDeliveryOn(date, "yes")
            }
        }
    }

    fun cancelSubscriptionConfirmed() {
        showProgressDialog(true)
        viewModel.cancelSubscription()
    }

    fun renewSubscriptionConfirmed() {
        viewModel.getProfile()
    }

    private fun removeEvent(time: Long) {
        Event(
            resources.getColor(
                R.color.green_base,
                theme
            ), time, "date"
        ).let { event ->
            binding.calendarView.removeEvent(event)
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
        }
    }

    fun selectedPaymentMode(item: String) {
        if (item == "Use Magizhini Wallet") {
            showSwipeConfirmationDialog(this, "")
        } else {
            viewModel.userProfile?.let {
                viewModel.selectedOrder?.let { order ->
                    startPayment(
                        this,
                        it.mailId,
                        (order.price * 100).toFloat(),
                        it.name,
                        it.id,
                        it.phNumber
                    ).also { status ->
                        if (!status) {
                            Toast.makeText(
                                this,
                                "Error in processing payment. Try Later ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } ?: showToast(this, "Please select the order and try again")
            } ?: showToast(this, "Failed to fetch your profile. Relaunch App and try again")
        }
    }

    fun confirmWalletPayment() {
        viewModel.renewSubWithWallet()
    }

    override fun selectedOrder(order: AmmaSpecialOrder, position: Int) {
        viewModel.selectedOrder?.let {
            viewModel.selectedOrder = null
            statusAdapter.selectedPosition = null
            statusAdapter.notifyDataSetChanged()
            binding.btnGoToBox.remove()
        } ?: let {
            viewModel.selectedOrder = order
            statusAdapter.selectedPosition = position
            statusAdapter.notifyDataSetChanged()
//            binding.apply {
//                btnGoToBox.visible()
//                if (
//                    System.currentTimeMillis() > TimeUtil().getCustomDateFromDifference(
//                        order.endDate,
//                        -7
//                    ) &&
//                    order.orderType == "month"
//                ) {
//                    btnRenewSub.visible()
//                    btnGoToBox.text = "CANCEL \nSUBSCRIPTION"
//                    btnRenewSub.text = "RENEW \nSUBSCRIPTION"
//                } else {
            binding.btnRenewSub.remove()
            binding.btnGoToBox.visible()
            binding.btnGoToBox.text = "CANCEL SUBSCRIPTION"
//                }
//            }
        }
    }

    override fun cancelDelivery(order: AmmaSpecialOrder) {
        viewModel.selectedOrder = order
        when {
            TimeUtil().getCurrentDate() == TimeUtil().getCustomDate(dateLong = date) -> {
                if (checkTimeLimit()) {
                    showExitSheet(
                        this,
                        "You are about to cancel your food delivery for ${
                            TimeUtil().getCustomDate(dateLong = date)
                        }. Refund for your cancelled food will reverted back to you in 3 to 5 business days. \n \nClick PROCEED To cancel delivery",
                        "delivery"
                    )
                } else {
                    showExitSheet(
                        this,
                        "You are about to cancel your food delivery for ${
                            TimeUtil().getCustomDate(dateLong = date)
                        }. Your food order has already been placed for preparation. You can still cancel your delivery but refund will not be provided. To know more please contact Customer Support. \n \nClick PROCEED To cancel delivery",
                        "delivery"
                    )
                }
            }
            System.currentTimeMillis() > date -> Unit
            else -> {
                showExitSheet(
                    this,
                    "You are about to cancel your food delivery for ${
                        TimeUtil().getCustomDate(dateLong = date)
                    }. Refund for your cancelled food will reverted back to you in 3 to 5 business days. \n \nClick PROCEED To cancel delivery",
                    "delivery"
                )
            }
        }
    }

    override fun onClick() {
        showListBottomSheet(
            this@FoodSubHistoryActivity,
            arrayListOf("Use Magizhini Wallet", "Use Paytm, UPI, cards...")
        )
    }

    private fun checkTimeLimit(): Boolean {
//         var min: Long = 0
        var difference: Long = 0
        try {
            val simpleDateFormat =
                SimpleDateFormat("hh:mm aa") // for 12-hour system, hh should be used instead of HH
            // There is no minute different between the two, only 8 hours difference. We are not considering Date, So minute will always remain 0
            val date1: Date =
                simpleDateFormat.parse(simpleDateFormat.format(System.currentTimeMillis()))
            val date2: Date = simpleDateFormat.parse("08:30 AM")

            difference = (date2.time - date1.time) / 1000
            val hours: Long = difference % (24 * 3600) / 3600 // Calculating Hours
            val minute: Long =
                difference % 3600 / 60 // Calculating minutes if there is any minutes difference
            return (minute + hours * 60) >= 0
//            min =
//                minute + (hours * 60) // This will be our final minutes. Multiplying by 60 as 1 hour contains 60 mins
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override fun onBackPressed() {
        viewModel.apply {
            selectedOrder = null
            userID = null
            tempAddress = null
            orderStatusMap.clear()
        }
        super.onBackPressed()
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
            val message: String =
                "Hi, ${viewModel.tempAddress?.let { "I\'m ${it.userId}" } ?: "I haven't Logged In."}. I need support with my Amma Samayal Subscription. I have provided my Name, Subscription ID and Reason below for further reference. Thank you."

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

    fun filterOrders(position: Int) {
        showProgressDialog(true)
        viewModel.selectedOrder = null
        statusAdapter.selectedPosition = null
        binding.btnRenewSub.remove()
        binding.btnGoToBox.remove()
        when (position) {
            0 -> {
                statusAdapter.orders =
                    viewModel.ammaSpecialOrders.filter { it.status != "success" && it.status != "fail" && it.status != "cancel" }
                if (statusAdapter.orders.isNullOrEmpty()) {
                    showToast(this, "No current pending deliveries")
                }
                binding.tvToolbarTitle.text = "Pending..."
                statusAdapter.notifyDataSetChanged()
            }
            1 -> {
                statusAdapter.orders = viewModel.ammaSpecialOrders.filter { it.status == "success" }
                if (statusAdapter.orders.isNullOrEmpty()) {
                    showToast(this, "No successfully delivered orders available")
                }
                binding.tvToolbarTitle.text = "Delivered..."
                statusAdapter.notifyDataSetChanged()
            }
            2 -> {
                statusAdapter.orders = viewModel.ammaSpecialOrders.filter { it.status == "cancel" }
                if (statusAdapter.orders.isNullOrEmpty()) {
                    showToast(this, "No cancelled orders available")
                }
                binding.tvToolbarTitle.text = "Cancelled..."
                statusAdapter.notifyDataSetChanged()
            }
            else -> {
                statusAdapter.orders = viewModel.ammaSpecialOrders.filter { it.status == "fail" }
                if (statusAdapter.orders.isNullOrEmpty()) {
                    showToast(this, "No failed orders available")
                }
                binding.tvToolbarTitle.text = "Failed..."
                statusAdapter.notifyDataSetChanged()
            }
        }
        hideProgressDialog()
    }

    fun selectedMenuType(position: Int) {
        if (position == 0) {
            Intent(this@FoodSubHistoryActivity, FoodSubscriptionActivity::class.java).also {
                it.putExtra("food", "amma")
                startActivity(it)
                finish()
            }
        } else {
Intent(this@FoodSubHistoryActivity, FoodSubscriptionActivity::class.java).also {
                it.putExtra("food", "aachi")
                startActivity(it)
                finish()
            }
        }
    }
}

