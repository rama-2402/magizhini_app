package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.sundeepk.compactcalendarview.CompactCalendarView
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
import kotlinx.coroutines.launch
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
    CustomAlertClickListener {

    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityFoodSubHistoryBinding
    private val factory: FoodSubscriptionViewModelFactory by instance()
    private lateinit var viewModel: FoodSubscriptionViewModel

    private var date: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_food_sub_history)
        viewModel = ViewModelProvider(this, factory)[FoodSubscriptionViewModel::class.java]

        Checkout.preload(applicationContext)

        initData()
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
            calendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
                override fun onDayClick(dateClicked: Date?) {
                    val instanceToGetLongDate = Calendar.getInstance()
                    instanceToGetLongDate.time = dateClicked!!

                    date = instanceToGetLongDate.timeInMillis

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
                    showExitSheet(
                        this@FoodSubHistoryActivity,
                        "You are about to cancel your Magizhini Amma's Special Food Subscription. If you are unsubscribed your balance money will be reverted back in 3 to 5 business days. \n \nClick PROCEED to confirm cancellation",
                        "sub"
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
        showProgressDialog(true)
        viewModel.userID =
            SharedPref(this).getData(USER_ID, STRING, "").toString()
        viewModel.getAmmaSpecialsOrderDetails(date)
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

                            if (it.leafNeeded) {
                                totalPrice += 5 * viewModel.selectedEventDates.size
                            }

                            it.price = totalPrice

                            CustomAlertDialog(
                                this,
                                "Subscription renewal for ${TimeUtil().getMonth(dateLong = viewModel.selectedEventDates.random())}",
                                "Your Subscription for Amma's Special Authentic Home-made Food ends on ${
                                    TimeUtil().getCustomDate(
                                        dateLong = it.endDate
                                    )
                                }. Please renew your subscription to continue your daily delivery for the next month. \n \nYou are required to pay Rs: $totalPrice to renew your subscription for the next month. For further queries please contact customer support.",
                                "RENEW SUBSCRIPTION",
                                "food",
                                this
                            )
                        }
                    }
                }
                is FoodSubscriptionViewModel.UiUpdate.UpdateFoodDeliveryStatus -> {
                    hideProgressDialog()
                    if (event.status == null) {
                        statusNotAvailableUI()
                    } else {
                        populateDeliveryStatus(event.status)
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
        var newMonthCounter = 0
        var currentDate: Long = endDate
        while (true) {
            currentDate += SINGLE_DAY_LONG
            if (TimeUtil().getDateNumber(currentDate) == "01") {
                newMonthCounter += 1
                if (newMonthCounter == 2) {
                    return viewModel.selectedEventDates.size
                }
            }
            if (
                TimeUtil().getDayName(currentDate) != "Sunday"
            ) {
                viewModel.selectedEventDates.add(currentDate)
            }
        }
    }

    private fun statusNotAvailableUI() {
        binding.apply {
            rvFoodStatus.remove()
            tvFoodStatus.visible()
            btnGoToBox.visible()
            btnRenewSub.remove()
            tvFoodStatus.text =
                "You don't have any food subscriptions yet. To eat Healthy Authentic Home-Made Food made with 100% Organic materials please check out our different Lunch Boxes to place an order "
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
                this@FoodSubHistoryActivity
            ).let {
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
        viewModel.getAmmaSpecials()
    }

    fun selectedPaymentMode(item: String) {
        if (item == "Use Magizhini Wallet") {
            showSwipeConfirmationDialog(this, "")
        } else {
        }
    }

    fun confirmWalletPayment() {
        viewModel.renewSubWithWallet()
    }

    override fun selectedOrder(order: AmmaSpecialOrder) {
        viewModel.selectedOrder = order
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
                btnGoToBox.text = "CANCEL \nDELIVERY"
                btnRenewSub.text = "RENEW \nSUBSCRIPTION"
            } else {
                btnRenewSub.remove()
                btnGoToBox.text = "CANCEL DELIVERY"
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

