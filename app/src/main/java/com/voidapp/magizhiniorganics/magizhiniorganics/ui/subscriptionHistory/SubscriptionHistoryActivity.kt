package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.SubscriptionHistoryAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySubscriptionHistoryBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogCalendarBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.LoadStatusDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.MONTHLY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUB_CANCELLED
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.startPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.util.*


class SubscriptionHistoryActivity :
    BaseActivity(),
    KodeinAware,
    PaymentResultListener,
    SubscriptionHistoryAdapter.SubscriptionHistoryListener {
    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivitySubscriptionHistoryBinding
    private lateinit var viewModel: SubscriptionHistoryViewModel
    private val factory: SubscriptionViewModelFactory by instance()

    private lateinit var subAdapter: SubscriptionHistoryAdapter

    private val mSubscriptionStatusFilter: MutableList<String> = mutableListOf(
        Constants.SUB_ACTIVE,
        Constants.SUB_CANCELLED
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_subscription_history)
        viewModel = ViewModelProvider(this, factory).get(SubscriptionHistoryViewModel::class.java)

        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            onBackPressed()
        }

        showShimmer()

        initLiveDate()
        initRecyclerView()
        liveDateObservers()
        listeners()
    }

    private fun listeners() {
        with(binding) {
            ivBackBtn.setOnClickListener {
                onBackPressed()
            }

            binding.ivFilter.setOnClickListener {
                showListBottomSheet(
                    this@SubscriptionHistoryActivity,
                    mSubscriptionStatusFilter as ArrayList<String>,
                    "filter"
                )
            }

        }
    }

    private fun initRecyclerView() {
        subAdapter = SubscriptionHistoryAdapter(
            mutableListOf(),
            this
        )
        binding.rvSubscriptionHistory.layoutManager = LinearLayoutManager(this)
        binding.rvSubscriptionHistory.adapter = subAdapter
    }

    private fun liveDateObservers() {
        viewModel.uiUpdate.observe(this) { event ->
            when (event) {
                is SubscriptionHistoryViewModel.UiUpdate.PopulateSubscriptions -> {
                    hideShimmer()
                    event.subscriptions?.let {
                        if (it.isNullOrEmpty()) {
                            binding.llEmptyLayout.visible()
                        } else {
                            subAdapter.setSubHistoryData(it)
                        }
                    } ?: let {
                        binding.llEmptyLayout.visible()
                        showErrorSnackBar(event.message!!, true)
                    }
                }
                is SubscriptionHistoryViewModel.UiUpdate.UpdateSubscription -> {
                    subAdapter.updateSubscription(event.position, event.subscription)
                }
                is SubscriptionHistoryViewModel.UiUpdate.SubRenewalStatus -> {
                    if (event.status) {
                        updateLoadStatusDialog("Subscription Renewed Successfully.!", "success")
                        viewModel.subscriptionsList[viewModel.subPosition] = viewModel.subscription!!
                        subAdapter.updateSubscription(viewModel.subPosition, viewModel.subscription!!)
                    } else {
                        updateLoadStatusDialog(event.message.toString(), "fail")
                        showExitSheet(
                            this,
                            "Server Error! Subscription renewal failed. If money is debited please contact customer support. \n \n For further queries please click this message to contact Customer Support",
                            "cs"
                        )
                    }
                }
                is SubscriptionHistoryViewModel.UiUpdate.SubCancelStatus -> {
                    if (event.status) {
                        updateLoadStatusDialog("Subscription Cancelled Successfully.!", "success")
                        viewModel.subscription?.let {
                            viewModel.subscriptionsList[viewModel.subPosition] = it
                            subAdapter.updateSubscription(viewModel.subPosition, it)
                            viewModel.refundSubBalance()
                        }
                    } else {
                        updateLoadStatusDialog(event.message.toString(), "fail")
                        showExitSheet(
                            this,
                            "Server Error! Failed to Cancel Subscription. Please contact Customer Support for futher assistance",
                            "cs"
                        )
                    }
                }
                is SubscriptionHistoryViewModel.UiUpdate.TransactionStatus -> {
                    if (event.status) {
                        viewModel.subscription?.let { sub ->
                            if (sub.status == Constants.CANCELLED) {
                                updateLoadStatusDialog("Wallet Refund Success.!", "success")
                                viewModel.getProfile()
                            } else {
                                updateLoadStatusDialog("Renewing Your Subscription...", "validating")
                                startTransaction(null)
                            }
                        }
                    } else {
                        updateLoadStatusDialog(event.message.toString(), "fail")
                        showExitSheet(
                            this,
                            "Server Error! Could not record wallet transaction. \n \n If Money is already debited from Wallet, Please contact customer support and the transaction will be reverted in 24 Hours",
                            "cs"
                        )
                    }
                }
                is SubscriptionHistoryViewModel.UiUpdate.ShowLoadStatusDialog -> {
                    LoadStatusDialog.newInstance("", event.message.toString(), event.data.toString()).show(supportFragmentManager,
                        Constants.LOAD_DIALOG
                    )
                }
                is SubscriptionHistoryViewModel.UiUpdate.UpdateLoadStatusDialog -> {
                    updateLoadStatusDialog(event.message.toString(), event.data.toString())
                }
                is SubscriptionHistoryViewModel.UiUpdate.DismissLoadStatusDialog -> {
                    dismissLoadStatusDialog()
                    if (viewModel.subscription!!.status == SUB_CANCELLED) {
                        showExitSheet(
                            this@SubscriptionHistoryActivity,
                            "Outstanding Balance for Delivery Cancelled Dates, Delivery Failed Days and Remaining Days is Added to the Wallet Successfully!\n" +
                                    " \n" +
                                    " Please Click the message to contact Customer Support for any queries or further assistance",
                            "cs"
                        )
                    }
                }
                is SubscriptionHistoryViewModel.UiUpdate.Empty -> return@observe
                else -> Unit
            }
            viewModel.setEmptyStatus()
        }

        viewModel.uiEvent.observe(this) { event ->
            when (event) {
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
    }

    fun setSubscriptionFilter(filter: String) {
        binding.tvEmptyMessage.text = "You don't have any $filter subscriptions !"
        when (filter) {
            Constants.SUB_ACTIVE -> {
                showShimmer()
                viewModel.getSubscriptions(Constants.SUB_ACTIVE)
            }
            Constants.SUB_CANCELLED -> {
                showShimmer()
                viewModel.getSubscriptions(Constants.SUB_CANCELLED)
            }
        }
    }

    private fun initLiveDate() {
        viewModel.getSubscriptions(Constants.SUB_ACTIVE)
    }

    override fun onStart() {
        viewModel.getProfile()
        super.onStart()
    }

    override fun onDestroy() {
        viewModel.let {
            it.subscriptionsList.clear()
            it.subscription = null
            it.currentUserProfile = null
            it.liveWallet = null
        }
        super.onDestroy()
    }

    private fun showCalendarDialog(sub: SubscriptionEntity) {
        var unSubscribe: Boolean = true
        var cancelDate: Long = 0L
        val cancelDates: MutableList<String> = mutableListOf()

        val calendarDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)

        val view: DialogCalendarBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(baseContext),
                R.layout.dialog_calendar,
                null,
                false
            )

        if (sub.status == Constants.SUB_CANCELLED) {
            view.unsubscribe.visibility = View.GONE
        }

        view.tvDueDate.text = TimeUtil().getCustomDate(dateLong = sub.endDate)

        view.calendarView.setUseThreeLetterAbbreviation(true)
        view.calendarView.shouldDrawIndicatorsBelowSelectedDays(true)

        val month = SimpleDateFormat("MMMM - yyyy")
        view.tvMonth.text = month.format(System.currentTimeMillis())

        for (date in sub.cancelledDates) {
            val event = Event(resources.getColor(R.color.red900, theme), date, "Cancelled")
            cancelDates.add(TimeUtil().getCustomDate(dateLong = date))      //we are creating list of date strings to check
            view.calendarView.addEvent(event)
        }

        for (date in sub.deliveredDates) {
            val event = Event(resources.getColor(R.color.green_base, theme), date, "Delivered")
            view.calendarView.addEvent(event)
        }

        for (date in sub.notDeliveredDates) {
            val event = Event(resources.getColor(R.color.orange200, theme), date, "Not Delivered")
            view.calendarView.addEvent(event)
        }

        val verificationTimeInMillis = if (System.currentTimeMillis() < sub.startDate) {
            sub.startDate
        } else {
            System.currentTimeMillis()
        }

        view.calendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
            override fun onDayClick(dateClicked: Date?) {
                val instanceToGetLongDate = Calendar.getInstance()
                instanceToGetLongDate.time = dateClicked!!
                if (
                    instanceToGetLongDate.timeInMillis > verificationTimeInMillis &&
                    instanceToGetLongDate.timeInMillis < sub.endDate &&
                    !cancelDates.contains(TimeUtil().getCustomDate(dateLong = instanceToGetLongDate.timeInMillis)) &&
                    sub.status == Constants.SUB_ACTIVE
                ) {
                    cancelDate = instanceToGetLongDate.timeInMillis
                    unSubscribe = false
                    view.tvDueText.text = "Cancel Delivery on "
                    view.tvDueDate.text = "${TimeUtil().getCustomDate(dateLong = cancelDate)}?"
                    view.tvUnsubscribe.text = "CONFIRM"
                } else {
                    unSubscribe = true
                    view.tvDueText.text = "Due Date: "
                    view.tvDueDate.text = TimeUtil().getCustomDate(dateLong = sub.endDate)
                    view.tvUnsubscribe.text = "UNSUBSCRIBE"
                }
            }

            override fun onMonthScroll(firstDayOfNewMonth: Date?) {
                view.tvMonth.text = month.format(firstDayOfNewMonth!!)
            }
        })

        view.unsubscribe.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(view.unsubscribe.context, R.anim.bounce))
            if (unSubscribe && sub.status == Constants.SUB_ACTIVE) {
                calendarDialog.dismiss()
                showExitSheet(this, "You will be Unsubscribed from ${viewModel.subscription!!.productName}. Outstanding Balance for Delivery Cancelled Dates, Delivery Failed Days and Remaining Days will be added to your Magizhini Wallet. Click PROCEED to continue")
            } else {
                lifecycleScope.launch {
                    if (!NetworkHelper.isOnline(this@SubscriptionHistoryActivity)) {
                        showToast(this@SubscriptionHistoryActivity, "Please check your Internet Connection")
                        return@launch
                    }
                    val cancel = async { viewModel.cancelDate(sub, cancelDate) }
                    if (cancel.await()) {
                        val event =
                            Event(
                                resources.getColor(R.color.matteRed, theme),
                                cancelDate,
                                "Delivered"
                            )
                        cancelDates.add(TimeUtil().getCustomDate(dateLong = cancelDate))      //we are creating list of date strings to check
                        view.calendarView.addEvent(event)
                        unSubscribe = true
                        view.tvDueText.text = "Due Date: "
                        view.tvDueDate.text = TimeUtil().getCustomDate(dateLong = sub.endDate)
                        view.tvUnsubscribe.text = "UNSUBSCRIBE"
                        Toast.makeText(
                            this@SubscriptionHistoryActivity,
                            "Delivery Cancelled",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@SubscriptionHistoryActivity,
                            "Cancellation failed! Try later",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            }
        }

        calendarDialog.setCancelable(true)
        calendarDialog.setContentView(view.root)
        hideProgressDialog()
        calendarDialog.show()
    }

    fun confirmCancellation() {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        viewModel.cancelSubscription()
    }

    private fun updateLoadStatusDialog(message: String, data: String) {
        LoadStatusDialog.statusContent = message
        LoadStatusDialog.statusText.value = data
    }

    private fun dismissLoadStatusDialog() {
        (supportFragmentManager.findFragmentByTag(Constants.LOAD_DIALOG) as? DialogFragment)?.dismiss()
    }

    fun moveToCustomerSupport() {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        Intent(this, ChatActivity::class.java).also {
            startActivity(it)
        }
    }

    private fun showShimmer() {
        with(binding) {
            flShimmerPlaceholder.visible()
            rvSubscriptionHistory.remove()
        }
    }

    private fun hideShimmer() {
        with(binding) {
            flShimmerPlaceholder.remove()
            rvSubscriptionHistory.visible()
        }
    }

    fun selectedPaymentMode(payment: String) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        viewModel.subscription ?: return
        when (payment) {
            "Online" -> {
                showSuccessDialog("", "Processing Payment...", "wallet")
                viewModel.currentUserProfile?.let {
                    startPayment(
                        this@SubscriptionHistoryActivity,
                        it.mailId,
                        viewModel.subscription!!.estimateAmount * 100,
                        it.name,
                        it.id,
                        it.phNumber
                    ).also { status ->
                        if (!status) {
                            Toast.makeText(
                                this@SubscriptionHistoryActivity,
                                "Error in processing payment. Try Later ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } ?: showErrorSnackBar("User Authentication Missing!. Please Log In again to make the purchase.", true)
            }
            else -> {
                showSwipeConfirmationDialog(this, "swipe right to make payment")
            }
        }
    }

    override fun onPaymentSuccess(response: String?) {
        startTransaction(response ?: "")
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }

    fun approved(status: Boolean) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        viewModel.liveWallet?.let {
            viewModel.subscription?.let { sub ->
                if (it.amount >= sub.estimateAmount) {
                    sub.paymentMode = "Wallet"
                   viewModel.initiateWalletTransaction(
                           sub.estimateAmount,
                           sub.customerID,
                           sub.id,
                           "Remove"
                   )
                } else {
                    showErrorSnackBar(
                        "Insufficient Wallet Balance. Please recharge your wallet to continue",
                        true
                    )
                }
            }
        } ?: showErrorSnackBar("Wallet data not available. Please pick other payment method", true)
    }

    private fun startTransaction(transactionID: String?) = lifecycleScope.launch(Dispatchers.IO) {
        viewModel.subscription?.let { sub ->
            val renewedDate = TimeUtil().getCustomDateFromDifference(sub.endDate, 30)
            sub.endDate = renewedDate
            sub.paymentMode = transactionID?.let {
                "Online"
            } ?: "Wallet"
            viewModel.renewSubscription(sub, transactionID ?: "")
        }
    }

    override fun renewSub(position: Int) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        lifecycleScope.launch {
            showProgressDialog()
            viewModel.subPosition = position
            viewModel.subscription = viewModel.subscriptionsList[position]
            viewModel.subscription?.let {
                if (it.subType != MONTHLY) {
                    async {
                        viewModel.getCancellationDays(it)
                    }.await()
                }
                val getUpdatedEstimateForNewSubJob =
                    async { viewModel.getUpdatedEstimateForNewSubJob(it) }
                val updatedEstimateForNewSub = getUpdatedEstimateForNewSubJob.await()
                if (updatedEstimateForNewSub == 0f) {
                    hideProgressDialog()
                    showToast(
                        this@SubscriptionHistoryActivity,
                        "Product is not available for purchase anymore. Please contact customer support for further assistance.",
                        LONG
                    )
                } else {
                    it.estimateAmount = updatedEstimateForNewSub
                    hideProgressDialog()
                    //THIS CAN BE USED IF WE WANT THE RENEWED SUB TO HAVE CURRENT PRODUCT PRICE
                    showExitSheet(
                        this@SubscriptionHistoryActivity,
                        "Renewing the Existing Subscription plan with revised Product MRP is Rs: ${viewModel.subscription!!.estimateAmount}. There is no additional cost added for renewal. To Continue with renewal click PROCEED below.",
                        "price"
                    )
                }
            }
        }
    }

    override fun cancelSub(position: Int) {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        viewModel.subPosition = position
        viewModel.subscription = viewModel.subscriptionsList[position]
        showExitSheet(this, "You will be Unsubscribed from ${viewModel.subscription!!.productName}. Outstanding Balance for Delivery Cancelled Dates, Delivery Failed Days and Remaining Days will be added to your Magizhini Wallet. Click PROCEED to continue")
    }

    override fun showCalendar(position: Int) {
        showProgressDialog()
        viewModel.subPosition = position
        viewModel.subscription = viewModel.subscriptionsList[position]
        showCalendarDialog(viewModel.subscription!!)
    }

    fun showPaymentMethod() {
        if (!NetworkHelper.isOnline(this)) {
            showErrorSnackBar("Please check your Internet Connection", true)
            return
        }
        viewModel.liveWallet?.let {
            Toast.makeText(
                this@SubscriptionHistoryActivity,
                "Choose Payment Method",
                Toast.LENGTH_SHORT
            ).show()
            showListBottomSheet(
                this@SubscriptionHistoryActivity,
                arrayListOf("Online", "Wallet - Rs: ${it.amount}"),
                "payment"
            )
        } ?: selectedPaymentMode("Online")
    }
}