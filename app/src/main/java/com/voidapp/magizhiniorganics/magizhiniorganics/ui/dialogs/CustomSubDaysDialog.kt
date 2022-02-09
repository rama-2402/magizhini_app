package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogCustomSubDaysBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.FRIDAY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.MONDAY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SATURDAY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SUNDAY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.THURSDAY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.TUESDAY
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.WEDNESDAY


class CustomSubDaysDialog (
    private val context: Context,
    private val days: ArrayList<String>,
    private val activity: Activity
){

    private val bottomSheetDialog: BottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog)

    init {

        val view: DialogCustomSubDaysBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_custom_sub_days,
                null,
                false)

        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.setContentView(view.root)


//        val view = LayoutInflater.from(context).inflate(R.layout.dialog_order_details, null)
//        bottomSheetDialog.setContentView(view)

        with(view) {
            for (day in days) {
                when (day) {
                    MONDAY -> cpMonday.isChecked = true
                    TUESDAY -> cpTuesday.isChecked = true
                    WEDNESDAY -> cpWednesday.isChecked = true
                    THURSDAY -> cpThursday.isChecked = true
                    FRIDAY -> cpFriday.isChecked = true
                    SATURDAY -> cpSaturday.isChecked = true
                    else -> cpSunday.isChecked = true
                }
            }

            btnSave.setOnClickListener {
                val selectedDays = arrayListOf<String>()
                if (cpMonday.isChecked) {
                    selectedDays.add(MONDAY)
                }
                if (cpTuesday.isChecked) {
                    selectedDays.add(TUESDAY)
                }
                if (cpWednesday.isChecked) {
                    selectedDays.add(WEDNESDAY)
                }
                if (cpThursday.isChecked) {
                    selectedDays.add(THURSDAY)
                }
                if (cpFriday.isChecked) {
                    selectedDays.add(FRIDAY)
                }
                if (cpSaturday.isChecked) {
                    selectedDays.add(SATURDAY)
                }
                if (cpSunday.isChecked) {
                    selectedDays.add(SUNDAY)
                }
                when (activity) {
                    is SubscriptionProductActivity -> {
                        dismiss()
                        activity.selectedCustomSubDates(selectedDays)
                    }
                }
            }

            btnCancel.setOnClickListener {
                val selectedDays = arrayListOf<String>()
                if (cpMonday.isChecked) {
                    selectedDays.add(MONDAY)
                }
                if (cpTuesday.isChecked) {
                    selectedDays.add(TUESDAY)
                }
                if (cpWednesday.isChecked) {
                    selectedDays.add(WEDNESDAY)
                }
                if (cpThursday.isChecked) {
                    selectedDays.add(THURSDAY)
                }
                if (cpFriday.isChecked) {
                    selectedDays.add(FRIDAY)
                }
                if (cpSaturday.isChecked) {
                    selectedDays.add(SATURDAY)
                }
                if (cpSunday.isChecked) {
                    selectedDays.add(SUNDAY)
                }
                when (activity) {
                    is SubscriptionProductActivity -> {
                        dismiss()
                        activity.selectedCustomSubDates(selectedDays)
                    }
                }
            }
        }
    }

    fun show() = bottomSheetDialog.show()

    fun dismiss() = bottomSheetDialog.dismiss()
}