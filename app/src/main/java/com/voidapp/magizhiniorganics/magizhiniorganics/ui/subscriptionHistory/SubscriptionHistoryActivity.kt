package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.razorpay.PaymentResultListener
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.SubscriptionHistoryAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySubscriptionHistoryBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogCalendarBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.customerSupport.ChatActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.startPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class SubscriptionHistoryActivity : BaseActivity(), KodeinAware, PaymentResultListener {
    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivitySubscriptionHistoryBinding
    private lateinit var viewModel: SubscriptionHistoryViewModel
    private val factory: SubscriptionViewModelFactory by instance()

    private lateinit var subAdapter: SubscriptionHistoryAdapter

    private var mSubscription: SubscriptionEntity = SubscriptionEntity()
    private var mAllSubscriptions: MutableList<SubscriptionEntity> = mutableListOf()
    private val mSubscriptionStatusFilter: MutableList<String> = mutableListOf()

    private var mWallet: Wallet = Wallet()
    private var mProfile: UserProfileEntity = UserProfileEntity()
    private val paymentList = mutableListOf<String>()
    private var id: String = ""

    private val TAG: String = "qqqq"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_subscription_history)
        viewModel = ViewModelProvider(this, factory).get(SubscriptionHistoryViewModel::class.java)
        binding.viewmodel = viewModel

        id = SharedPref(this).getData(Constants.USER_ID, Constants.STRING, "").toString()

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

            //scroll change listener to hide the fab when scrolling down
            binding.rvSubscriptionHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, up: Int, down: Int) {
                    super.onScrolled(recyclerView, up, down)
                    if (down > 0 && binding.fabMonthFilter.isVisible) {
                        binding.fabMonthFilter.hide()
                    } else if (down < 0 && binding.fabMonthFilter.isGone) {
                        binding.fabMonthFilter.show()
                    }
                }
            })

            binding.fabMonthFilter.setOnClickListener {
                showListBottomSheet(this@SubscriptionHistoryActivity, mSubscriptionStatusFilter as ArrayList<String>, "filter")
            }

        }
    }

    private fun initRecyclerView() {
        subAdapter = SubscriptionHistoryAdapter(
            this,
            viewModel,
            listOf()
        )
        binding.rvSubscriptionHistory.layoutManager = LinearLayoutManager(this)
        binding.rvSubscriptionHistory.adapter = subAdapter
    }

    private fun liveDateObservers() {
        viewModel.activeSub.observe(this, {
            mAllSubscriptions.clear()
            lifecycleScope.launch {
                mAllSubscriptions.addAll(it)
                subAdapter.subscriptions = it
                subAdapter.notifyDataSetChanged()
                delay(1500)
                hideShimmer()
            }
        })
        viewModel.error.observe(this, {
            lifecycleScope.launch {
                delay(1500)
                hideSuccessDialog()
                showErrorSnackBar(it, true)
            }
        })
        viewModel.subStatus.observe(this, {
            lifecycleScope.launch (Dispatchers.Main) {
                delay(1500)
                if (it == "complete") {
                    hideSuccessDialog()
                    showSuccessDialog(title = "", body = "Unsubscribed successfully!", content = "complete")
                    for (i in 0 until mAllSubscriptions.size) {
                        if (mSubscription.id == mAllSubscriptions[i].id) {
                            subAdapter.subscriptions[i].status = Constants.CANCELLED
                            subAdapter.notifyItemChanged(i)
                        }
                    }
                    delay(2000)
                    hideSuccessDialog()
                    showSuccessDialog(title = "",body = "Adding balance to the Wallet...", content = "wallet")
                    viewModel.calculateBalance(mSubscription)
                }
            }
        })
        viewModel.wallet.observe(this, {
            mWallet = it
            paymentList.clear()
            paymentList.add("Online")
            paymentList.add("Wallet - (${mWallet.amount})")
        })
        viewModel.renewSub.observe(this, {
            renewSubscription(it)
        })
        viewModel.cancelSub.observe(this, {
            unsubscribeSubscription(it)
        })
        viewModel.profile.observe(this, {
            mProfile = it
        })
        viewModel.walletTransactionStatus.observe(this, {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(1500)
                hideSuccessDialog()
                if (it) {
                    showExitSheet(this@SubscriptionHistoryActivity, "Outstanding Balance for Delivery Cancelled Dates, Delivery Failed Days and Remaining Days is Added to the Wallet Successfully!\n" +
                            " \n" +
                            " Please Click the message to contact Customer Support" , "cs")
                } else {
                    showExitSheet(this@SubscriptionHistoryActivity, "Server Error! Failed to add Balance to the Wallet. \n \n Please Click the message to contact Customer Support", "cs")
                }
            }
        })
    }

    fun setSubscriptionFilter(filter: String) {
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
//        viewModel.getWallet(id)
        viewModel.getProfile()
        with (mSubscriptionStatusFilter) {
            clear()
            add(Constants.SUB_ACTIVE)
            add(Constants.SUB_CANCELLED)
        }
    }

    fun showCalendarDialog(sub: SubscriptionEntity) {
        var unSubscribe: Boolean = true
        var cancelDate: Long = 0L
        val cancelDates: MutableList<String> = mutableListOf()

        val calendarDialog = BottomSheetDialog(this, R.style.BottomSheetDialog)
        mSubscription = sub

        val view: DialogCalendarBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(baseContext),
                R.layout.dialog_calendar,
                null,
                false)

        if (sub.status == Constants.SUB_CANCELLED) {
            view.unsubscribe.visibility = View.GONE
        }

        view.tvDueDate.text = TimeUtil().getCustomDate(dateLong = sub.endDate)

        view.calendarView.setUseThreeLetterAbbreviation(true)

        val month = SimpleDateFormat("MMMM - yyyy")
        view.tvMonth.text = month.format(System.currentTimeMillis())

        for (date in sub.cancelledDates) {
            val event = Event(resources.getColor(R.color.matteRed, theme), date, "Cancelled")
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
                showExitSheet(this, "Cancel Subscription")
            } else {
                if (viewModel.cancelDate(sub, cancelDate)) {
                    val event = Event(resources.getColor(R.color.matteRed, theme), cancelDate, "Delivered")
                    cancelDates.add(TimeUtil().getCustomDate(dateLong = cancelDate))      //we are creating list of date strings to check
                    view.calendarView.addEvent(event)
                    unSubscribe = true
                    view.tvDueText.text = "Due Date: "
                    view.tvDueDate.text = TimeUtil().getCustomDate(dateLong = sub.endDate)
                    view.tvUnsubscribe.text = "UNSUBSCRIBE"
                    Toast.makeText(this, "Delivery Cancelled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Cancellation failed! Try later", Toast.LENGTH_SHORT).show()
                }
            }
        }

        calendarDialog.setCancelable(true)
        calendarDialog.setContentView(view.root)
        lifecycleScope.launch {
            delay(1000)
            hideProgressDialog()
            calendarDialog.show()
        }
    }

    fun confirmCancellation() {
        //logic to cancel the current subscription
        hideExitSheet()
        showSuccessDialog("", "Cancelling Subscription...", "dates")
        viewModel.cancelSubscription(mSubscription)
    }

    fun moveToCustomerSupport() {
        hideExitSheet()
        Intent(this, ChatActivity::class.java).also {
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    private fun unsubscribeSubscription(subscription: SubscriptionEntity) {
        mSubscription = subscription
        showExitSheet(this, "Cancel Subscription")
    }

    private fun renewSubscription(subscription: SubscriptionEntity) {
        mSubscription = subscription
        Toast.makeText(this, "Choose Payment Method", Toast.LENGTH_SHORT).show()
        showListBottomSheet(this, paymentList as ArrayList<String>, "payment")
    }

    fun selectedPaymentMode(payment: String) {
        when (payment) {
            "Online" -> {
                showSuccessDialog("", "Processing Payment...", "wallet")
                with(mProfile) {
                    startPayment(
                        this@SubscriptionHistoryActivity,
                        mailId,
                        mSubscription.estimateAmount * 100,
                        name,
                        id,
                        phNumber
                    ).also { status ->
                        if (!status) {
                            Toast.makeText(
                                this@SubscriptionHistoryActivity,
                                "Error in processing payment. Try Later ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            else -> {
                showSwipeConfirmationDialog(this, "swipe right to make payment")
            }
        }
    }

    override fun onPaymentSuccess(response: String?) {
        showSuccessDialog("", "Processing payment ...", "wallet")
        lifecycleScope.launch {
            startTransaction()
        }
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        showErrorSnackBar("Payment Failed! Choose different payment method", true)
    }


    fun approved(status: Boolean) = lifecycleScope.launch (Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            showSuccessDialog("", "Processing payment from Wallet...", "wallet")
        }
        if (status) {
            if (viewModel.checkWalletForBalance(mSubscription.estimateAmount, id)) {
                val id = viewModel.makeTransactionFromWallet(mSubscription.estimateAmount, id, mSubscription.id)
                validatingTransactionBeforeOrder(id)
            } else {
                withContext(Dispatchers.Main) {
                    delay(1000)
                    hideSuccessDialog()
                    showErrorSnackBar(
                        "Insufficient balance in Wallet. Pick another payment method",
                        true
                    )
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@SubscriptionHistoryActivity, "Transaction cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun validatingTransactionBeforeOrder(id: String) = withContext(Dispatchers.Main) {
        if ( id == "failed") {
            hideSuccessDialog()
            showErrorSnackBar("Server Error! If money is debited please contact customer support", false)
            return@withContext
        } else {
            withContext(Dispatchers.Main) {
                hideSuccessDialog()
                showSuccessDialog("", "Updating Subscription...", "dates")
                startTransaction()
            }
        }
    }

    private suspend fun startTransaction() = withContext (Dispatchers.IO) {
        val renewedDate = TimeUtil().getCustomDateFromDifference(mSubscription.endDate, 30)
        if(viewModel.renewSubscription(mSubscription.id, mSubscription.monthYear, renewedDate)) {
            delay(1500)
            withContext(Dispatchers.Main) {
                hideSuccessDialog()
                viewModel.getSubscriptions(Constants.SUB_ACTIVE)
                showSuccessDialog("", "Subscription Updated Successfully...")
                delay(1500)
                hideSuccessDialog()
            }
        } else {
            withContext(Dispatchers.Main) {
                hideSuccessDialog()
                showErrorSnackBar("Server Error! Subscription can't be created. If money is debited please contact customer support", false, "long")
                return@withContext
            }
        }
    }
}