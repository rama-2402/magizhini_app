package com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory

import android.app.usage.UsageEvents
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aminography.primecalendar.PrimeCalendar
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.drawable.DrawableUtils
import com.google.firebase.database.collection.LLRBNode
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.SubscriptionHistoryAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.SubscriptionEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySubscriptionHistoryBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomExitConfirmationBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogCalendarBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.DatePickerLib
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Utils.toDateNumber
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Utils.toMonthNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.Constant
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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

//    fun cancellationDates(dates: List<PrimeCalendar>) {
//        showSuccessDialog(body = "validating cancellation dates...")
//        for (date in dates) {
//            if (date.timeInMillis < mSubscription.startDate || date.timeInMillis > mSubscription.endDate) {
//                showErrorSnackBar("Pick valid days between subscription period", true)
//            } else {
//                viewModel.addCancellationDates(mSubscription, dates)
//            }
//        }
//    }

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

    fun showCalendarDialog(sub: SubscriptionEntity) {
        var unSubscribe: Boolean = true
        var cancelDate: Long = 0L

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

        val min = Calendar.getInstance()
        min.add(Calendar.DAY_OF_MONTH, getDate(sub.startDate))
        view.calendarView.setMinimumDate(min)

        val max = Calendar.getInstance()
        max.add(Calendar.DAY_OF_MONTH, getDate(sub.endDate))
        view.calendarView.setMaximumDate(max)

        val cancelledEvents = ArrayList<EventDay>()
        for (date in sub.cancelledDates) {
            val cancelledCalendar = Calendar.getInstance()      //this instance is to update the cancelled date
            val cal = Calendar.getInstance()        //this instance is to get the number of days ahead the subscription start date is from the current date
            cal.timeInMillis = sub.startDate        //we are assigning the subscription start date
            cal.add(Calendar.DATE, getDifference(sub.startDate, date))      //adding the date to calendar by incrementing the cancelled date from the current date
            val extra = getDate(sub.startDate) + getDifference(sub.startDate, cal.timeInMillis)     //here we are getting the int value of the cancelled date from current date by getting the int of the sub start date + the int of the cancelled date from the sub start date
            cancelledCalendar.add(Calendar.DAY_OF_MONTH, extra + 2)
            val event = EventDay(
                cancelledCalendar, R.drawable.ic_delivery_cancelled
            )
        cancelledEvents.add(event)
        }
        view.calendarView.setEvents(cancelledEvents as List<EventDay>)

        view.calendarView.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                if (
                    eventDay.calendar.timeInMillis > System.currentTimeMillis() &&
                    eventDay.calendar.timeInMillis > sub.startDate &&
                    eventDay.calendar.timeInMillis < sub.endDate &&
                    !sub.cancelledDates.contains(eventDay.calendar.timeInMillis) &&
                    sub.status == Constants.SUB_ACTIVE
                ) {
                    cancelDate = eventDay.calendar.timeInMillis
                    unSubscribe = false
                    view.tvDueText.text = "Cancel Delivery on: "
                    view.tvDueAmount.text = Time().getCustomDate(dateLong = cancelDate)
                    view.tvUnsubscribe.text = "CONFIRM"
                } else {
                    unSubscribe = true
                    view.tvDueText.text = "Monthly Due: "
                    view.tvDueAmount.text = "Rs: ${sub.estimateAmount}"
                    view.tvUnsubscribe.text = "UNSUBSCRIBE"
                }
            }
        })

        view.unsubscribe.setOnClickListener {
            if (unSubscribe && sub.status == Constants.SUB_ACTIVE) {
                calendarDialog.dismiss()
                showExitSheet(this, "Cancel Subscription")
            } else {
                if (viewModel.cancelDate(sub, cancelDate)) {
                    sub.cancelledDates.add(cancelDate)
                    val cancelledCalendar = Calendar.getInstance()      //this instance is to update the cancelled date
                    val cal = Calendar.getInstance()        //this instance is to get the number of days ahead the subscription start date is from the current date
                    cal.timeInMillis = sub.startDate        //we are assigning the subscription start date
                    cal.add(Calendar.DATE, getDifference(sub.startDate, cancelDate))      //adding the date to calendar by incrementing the cancelled date from the current date
                    val extra = getDate(sub.startDate) + getDifference(sub.startDate, cal.timeInMillis)     //here we are getting the int value of the cancelled date from current date by getting the int of the sub start date + the int of the cancelled date from the sub start date
                    cancelledCalendar.add(Calendar.DAY_OF_MONTH, extra + 2)
                    val event = EventDay(
                        cancelledCalendar, R.drawable.ic_delivery_cancelled
                    )
                    cancelledEvents.add(event)
                    view.calendarView.setEvents(cancelledEvents as List<EventDay>)
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

    private fun getDifference(start: Long, end: Long): Int {
        val diff = (end - start)/ (24 * 60 * 60 * 1000)
        return diff.toInt()
    }

    private fun getDate(startDate: Long): Int {
        var diff = (startDate - System.currentTimeMillis())/ (24 * 60 * 60 * 1000)
        diff = if (diff < 0) {
            diff - 1
        } else {
            diff
        }
        return diff.toInt()
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