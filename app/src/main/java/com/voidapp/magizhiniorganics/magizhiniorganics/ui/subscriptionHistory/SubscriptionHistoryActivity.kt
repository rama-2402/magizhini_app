package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.SubscriptionHistoryAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySubscriptionHistoryBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogCalendarBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.util.*

class SubscriptionHistoryActivity : BaseActivity(), KodeinAware {
    override val kodein: Kodein by kodein()
    private lateinit var binding: ActivitySubscriptionHistoryBinding
    private lateinit var viewModel: SubscriptionHistoryViewModel
    private val factory: SubscriptionViewModelFactory by instance()

    private lateinit var subAdapter: SubscriptionHistoryAdapter

    private var mSubscription: SubscriptionEntity = SubscriptionEntity()
    private var mAllSubscriptions: MutableList<SubscriptionEntity> = mutableListOf()

    private val TAG: String = "qqqq"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_subscription_history)
        viewModel = ViewModelProvider(this, factory).get(SubscriptionHistoryViewModel::class.java)
        binding.viewmodel = viewModel

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
                }
            }
        })
    }

    private fun initLiveDate() {
        viewModel.getSubscriptions()
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

        view.calendarView.setUseThreeLetterAbbreviation(true)

        for (date in sub.cancelledDates) {
            val event = Event(resources.getColor(R.color.matteRed, theme), date, "Cancelled")
            cancelDates.add(Time().getCustomDate(dateLong = date))      //we are creating list of date strings to check
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

        view.calendarView.setListener(object : CompactCalendarView.CompactCalendarViewListener {
            override fun onDayClick(dateClicked: Date?) {
                val instanceToGetLongDate = Calendar.getInstance()
                instanceToGetLongDate.time = dateClicked
                    if (
                        instanceToGetLongDate.timeInMillis > System.currentTimeMillis() &&
                        instanceToGetLongDate.timeInMillis < sub.endDate &&
                        !cancelDates.contains(Time().getCustomDate(dateLong = instanceToGetLongDate.timeInMillis)) &&
                        sub.status == Constants.SUB_ACTIVE
                    ) {
                        cancelDate = instanceToGetLongDate.timeInMillis
                        unSubscribe = false
                        view.tvDueText.text = "Cancel Delivery on "
                        view.tvDueAmount.text = "${Time().getCustomDate(dateLong = cancelDate)}?"
                        view.tvUnsubscribe.text = "CONFIRM"
                    } else {
                        unSubscribe = true
                        view.tvDueText.text = "Monthly Due: "
                        view.tvDueAmount.text = "Rs: ${sub.estimateAmount}"
                        view.tvUnsubscribe.text = "UNSUBSCRIBE"
                    }
            }

            override fun onMonthScroll(firstDayOfNewMonth: Date?) {
                val month = SimpleDateFormat("MMMM - yyyy")
                view.tvMonth.text = month.format(firstDayOfNewMonth)
            }
        })

        view.unsubscribe.setOnClickListener {
            if (unSubscribe && sub.status == Constants.SUB_ACTIVE) {
                calendarDialog.dismiss()
                showExitSheet(this, "Cancel Subscription")
            } else {
                if (viewModel.cancelDate(sub, cancelDate)) {
                    val event = Event(resources.getColor(R.color.matteRed, theme), cancelDate, "Delivered")
                    cancelDates.add(Time().getCustomDate(dateLong = cancelDate))      //we are creating list of date strings to check
                    view.calendarView.addEvent(event)
                    unSubscribe = true
                    view.tvDueText.text = "Monthly Due: "
                    view.tvDueAmount.text = "Rs: ${sub.estimateAmount}"
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

    private fun showShimmer() {
        with(binding) {
            flShimmerPlaceholder.show()
            rvSubscriptionHistory.gone()
        }
    }

    private fun hideShimmer() {
        with(binding) {
            flShimmerPlaceholder.gone()
            rvSubscriptionHistory.show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}