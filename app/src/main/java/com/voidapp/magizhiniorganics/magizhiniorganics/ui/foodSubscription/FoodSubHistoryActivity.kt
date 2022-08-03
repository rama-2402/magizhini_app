package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityFoodSubHistoryBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_ID
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.SimpleDateFormat
import java.util.*

class FoodSubHistoryActivity :
    BaseActivity(),
    KodeinAware {
    override val kodein: Kodein by kodein()

    private lateinit var binding: ActivityFoodSubHistoryBinding
    private val factory: FoodSubscriptionViewModelFactory by instance()
    private lateinit var viewModel: FoodSubscriptionViewModel

    private var date: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_food_sub_history)
        viewModel = ViewModelProvider(this, factory)[FoodSubscriptionViewModel::class.java]

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
                is FoodSubscriptionViewModel.UiUpdate.UpdateFoodDeliveryStatus -> {
                    hideProgressDialog()
                    if (event.status == null || event.status == "none") {
                        binding.tvFoodStatus.text =
                            "Food preparation details not available at the moment"
                    } else {
                        populateDeliveryStatus(event.status)
                    }
                }
                is FoodSubscriptionViewModel.UiUpdate.Empty -> return@observe
                else -> viewModel.setEmptyStatus()
            }
            viewModel.setEmptyStatus()
        }
    }

    private fun populateDeliveryStatus(status: String) {
        binding.apply {
//            tvFoodStatus.text = when (status) {
//                "preparing" -> "All ingredients are quality checked and sent for cooking"
//                "cooking" -> "Your Food is being cooked right now"
//                "ready" -> "Your Food is packed and out for delivery"
//                else -> {
//                    if (viewModel.ammaSpecialOrders.isNotEmpty()) {
//                        when(viewModel.ammaSpecialOrders[0].status) {
//                             "success" -> "Your Food has been delivered successfully"
//                            "failed" -> "Sorry, We have failed to deliver your food. Please contact customer support for more information"
//                            "cancel" -> "You have cancelled the delivery for the day."
//                        }
//                    }
//
//                }
//            }
        }
    }

}

