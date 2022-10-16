package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
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
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LOAD_DIALOG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SINGLE_DAY_LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_ID
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.startPayment
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class FoodSubHistoryActivity :
    BaseActivity(),
    KodeinAware,
    FoodStatusOnClickListener,
    PaymentResultListener,
    CustomAlertClickListener
{

    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityFoodSubHistoryBinding
    private val factory: FoodSubscriptionViewModelFactory by instance()
    private lateinit var viewModel: FoodSubscriptionViewModel
    private lateinit var statusAdapter: FoodStatusAdapter

    private var date: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_food_sub_history)
        viewModel = ViewModelProvider(this, factory)[FoodSubscriptionViewModel::class.java]

        Checkout.preload(applicationContext)

        initData()
        initLiveData()
        initListeners()
    }

    private fun initListeners() {
        binding.apply {
            ivHistory.setOnClickListener {
                Intent(this@FoodSubHistoryActivity, FoodSubscriptionActivity::class.java).also {
                    startActivity(it)
                    finish()
                }
            }
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
                statusAdapter.selectedPosition = null
                statusAdapter.notifyDataSetChanged()
                btnGoToBox.remove()
            }
            calendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
                override fun onDayClick(dateClicked: Date?) {
                    val instanceToGetLongDate = Calendar.getInstance()
                    instanceToGetLongDate.time = dateClicked!!

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

                    if (viewModel.ammaSpecialOrders.isNotEmpty()) {
                        showProgressDialog(true)
                        viewModel.getFoodStatus(date)
                    }
                }

                override fun onMonthScroll(firstDayOfNewMonth: Date?) {
                    val month = SimpleDateFormat("MMMM - yyyy")
                    tvMonth.text = month.format(firstDayOfNewMonth!!)
                }
            })
            btnGoToBox.setOnClickListener {
                if (btnGoToBox.text == "OPEN MENU") {
                    Intent(this@FoodSubHistoryActivity, FoodSubscriptionActivity::class.java).also {
                        startActivity(it)
                        finish()
                    }
                } else {
//                    showExitSheet(
//                        this@FoodSubHistoryActivity,
//                        "You are about to cancel your Magizhini Amma's Special Food Subscription. If you are unsubscribed your refund for the not delivered days will be reverted back in 3 to 5 business days. \n \nClick PROCEED to confirm cancellation",
//                        "sub"
//                    )
                    showExitSheet(
                        this@FoodSubHistoryActivity,
                        "You are about to renew your Monthly Subscription for Magizhini's Amma's Special Authentic Home-made Food plan. \n \nClick PROCEED to renew your subscription for the next month",
                        "renew"
                    )
                }
            }
            btnRenewSub.setOnClickListener {
                showExitSheet(
                    this@FoodSubHistoryActivity,
                    "You are about to renew your Monthly Subscription for Magizhini's Amma's Special Authentic Home-made Food plan. \n \nClick PROCEED to renew your subscription for the next month",
                    "renew"
                )
            }
        }
    }

    private fun initData() {
//        showProgressDialog(true)

        binding.tvMonth.text = SimpleDateFormat("MMMM - yyyy").format(date)

        viewModel.userID =
            SharedPref(this).getData(USER_ID, STRING, "").toString()
        viewModel.getAmmaSpecialsOrderDetails(date)
        viewModel.getNonDeliveryDays()
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
                    viewModel.selectedOrder?.let {
                        calculateRenewMonthDays(it.endDate).let { days ->
                            var totalPrice = 0.0
                            viewModel.selectedEventDates.forEach { dateLong ->
                                totalPrice += viewModel.lunchMap[TimeUtil().getDayName(dateLong)]
                                    ?: 0.0
                            }
                            totalPrice *= it.orderCount
                            totalPrice = (totalPrice * 5)/100

                            if (it.leafNeeded) {
                                totalPrice += 5 * viewModel.selectedEventDates.size
                            }

                            it.price = totalPrice

                            CustomAlertDialog(
                                this,
                                "Monthly Subscription Renewal",
                                "Your Subscription for Amma's Special Authentic Home-made Food ends on ${
                                    TimeUtil().getCustomDate(
                                        dateLong = it.endDate
                                    )
                                }. Please renew your subscription to continue your daily delivery for the next month. \n \nYou are required to pay Rs: $totalPrice (Incl 5% GST) to renew your subscription for the next month. For further queries please contact customer support.",
                                "RENEW SUBSCRIPTION",
                                "food",
                                this
                            ).show()
                        }
                    }
                }
                is FoodSubscriptionViewModel.UiUpdate.UpdateFoodDeliveryStatus -> {
                    hideProgressDialog()
                    if (event.status == null) {
                        if (!event.isOrdersAvailable) {
                            statusNotAvailableUI()
                        }
                    }

                    event.status?.let { populateDeliveryStatus(it) }

                    if (
                        TimeUtil().getDayName(date) == "Saturday" ||
                        TimeUtil().getDayName(date) == "Sunday"
                    ) {
                        showToast(
                            this@FoodSubHistoryActivity,
                            "Delivery not available on Saturdays and Sundays"
                        )
                        binding.rvFoodStatus.remove()
                    }
                }
                is FoodSubscriptionViewModel.UiUpdate.CancelOrderStatus -> {
                    hideProgressDialog()
                    if (event.status) {
                        showToast(this, event.message)
                    } else {
                        showToast(this, event.message, LONG)
                    }
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

    private fun populateDeliveryStatus(status: HashMap<String, String>) {
        binding.apply {
            if (viewModel.ammaSpecialOrders.isEmpty()) {
                statusNotAvailableUI()
                return
            }
            tvFoodStatus.remove()
            rvFoodStatus.visible()
            btnGoToBox.remove()
            btnRenewSub.remove()
            FoodStatusAdapter(
                viewModel.ammaSpecialOrders.filter { it.endDate >= date },
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
        viewModel.getAmmaSpecials()
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
            } ?: showToast(this, "Failed to fetch your profile. Refresh and try again")
        }
    }

    fun confirmWalletPayment() {
        viewModel.renewSubWithWallet()
    }

    override fun selectedOrder(order: AmmaSpecialOrder, position: Int) {
        viewModel.selectedOrder = order
        statusAdapter.selectedPosition = position
        statusAdapter.notifyDataSetChanged()
        binding.apply {
            btnGoToBox.visible()
            if (
                System.currentTimeMillis() > TimeUtil().getCustomDateFromDifference(
                    order.endDate,
                    -7
                ) &&
                order.orderType == "month"
            ) {
                btnRenewSub.visible()
                btnGoToBox.text = "CANCEL \nSUBSCRIPTION"
                btnRenewSub.text = "RENEW \nSUBSCRIPTION"
            } else {
                btnRenewSub.remove()
                btnGoToBox.text = "CANCEL SUBSCRIPTION"
            }
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
            orderStatusMap.clear()
        }
        super.onBackPressed()
    }
}

