package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.RadioGroup
import androidx.activity.contextaware.withContextAvailable
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.github.sundeepk.compactcalendarview.domain.Event
import com.google.android.datatransport.cct.internal.LogEvent
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityFoodOrderBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SINGLE_DAY_LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.setTextAnimation
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
import kotlin.math.log
import java.sql.Time as Time

class FoodOrderActivity :
    BaseActivity(),
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

    private fun checkTimeLimit()= lifecycleScope.launch(Dispatchers.IO) {
        while(loop) {
//         var min: Long = 0
        var difference: Long = 0
        try {
            val simpleDateFormat =
                SimpleDateFormat("hh:mm aa") // for 12-hour system, hh should be used instead of HH
            // There is no minute different between the two, only 8 hours difference. We are not considering Date, So minute will always remain 0
            val date1: Date = simpleDateFormat.parse(simpleDateFormat.format(System.currentTimeMillis()))
            val date2: Date = simpleDateFormat.parse("08:30 AM")

            difference = (date2.time - date1.time) / 1000
            val hours: Long = difference % (24 * 3600) / 3600 // Calculating Hours
            val minute: Long =
                difference % 3600 / 60 // Calculating minutes if there is any minutes difference
             withContext(Dispatchers.Main) {
                 if ((minute + hours * 60) < 0) {
                     binding.tvTimeLimit.text = "Order Intake closed for today. You can still place order for tomorrow"
                     loop = false
                 } else {
                     binding.tvTimeLimit.text = "Place order in the next $hours Hour $minute Minutes to get delivery starting from today"
                 }
             }
//            min =
//                minute + (hours * 60) // This will be our final minutes. Multiplying by 60 as 1 hour contains 60 mins
        } catch (e: Exception) {
            e.printStackTrace()
        }
            delay(1000)
        }
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
                    if (viewModel.currentSubOption == "month") {
                        return
                    }
                    val instanceToGetLongDate = Calendar.getInstance()
                    instanceToGetLongDate.time = dateClicked!!

                    val limitTime = if (loop) {
                        System.currentTimeMillis() - SINGLE_DAY_LONG
                    } else {
                        System.currentTimeMillis()
                    }

                    if (viewModel.currentSubOption == "custom") {
                        if (instanceToGetLongDate.timeInMillis > limitTime) {
                            if (TimeUtil().getDayName(instanceToGetLongDate.timeInMillis) == "Sunday") {
                                showToast(
                                    this@FoodOrderActivity,
                                    "Delivery Not Available on Sundays"
                                )
                                return
                            }
                            if (viewModel.selectedEventDates.contains(instanceToGetLongDate.timeInMillis)) {
                                removeEvent(instanceToGetLongDate.timeInMillis)
                            } else {
                                addEvent(instanceToGetLongDate.timeInMillis)
                            }
                        } else {
                            showToast(this@FoodOrderActivity, "Please pick a valid date")
                        }
                    } else {
                        if (TimeUtil().getDayName(instanceToGetLongDate.timeInMillis) == "Sunday") {
                            showToast(this@FoodOrderActivity, "Delivery Not Available on Sundays")
                            return
                        }
                        if (viewModel.selectedEventDates.isNotEmpty()) {
                            removeEvent(viewModel.selectedEventDates[0])
                        }
                        viewModel.selectedEventDates.clear()
                        if (
                            instanceToGetLongDate.timeInMillis > limitTime
                        ) {
                            addEvent(instanceToGetLongDate.timeInMillis)
                        } else {
                            showToast(this@FoodOrderActivity, "Please pick a valid date")
                        }
                    }

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
                        0 -> {
                            populateMonthEvents()
                            "month"
                        }
                        1 -> "single"
                        else -> "custom"
                    }
                    setPrice()
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
        }
    }

    private fun populateMonthEvents() {
        var currentDate: Long = System.currentTimeMillis()
        var loop = true
        var prevDay = TimeUtil().getDateNumber(currentDate)
        if (loop) {
            currentDate -= SINGLE_DAY_LONG
        }
        while (loop) {
            currentDate += SINGLE_DAY_LONG
            if (prevDay > TimeUtil().getDateNumber(currentDate)) {
                return
            }
            prevDay = TimeUtil().getDateNumber(currentDate)
            if (
                TimeUtil().getDayName(currentDate) != "Sunday"
            ) {
                addEvent(currentDate)
            }
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
                is FoodSubscriptionViewModel.UiUpdate.PopulateUserProfile -> {
                    hideProgressDialog()
                    populateProfileData(event.userProfile)
                }
                is FoodSubscriptionViewModel.UiUpdate.Empty -> return@observe
                else -> viewModel.setEmptyStatus()
            }
            viewModel.setEmptyStatus()
        }
    }

    private fun removeEvent(time: Long) {
        Event(
            resources.getColor(
                R.color.matteRed,
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
                R.color.matteRed,
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
            totalPrice += 5
        }

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
}