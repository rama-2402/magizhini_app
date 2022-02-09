package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogCalendarFilterBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity

class CalendarFilterDialog(
    private val context: Context,
    private val activity: Activity,
    private val month: String,
    private val year: String,
){

    private val bottomSheetDialog: BottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog)

    init {

        val view: DialogCalendarFilterBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.dialog_calendar_filter,
                null,
                false)

        bottomSheetDialog.setCancelable(true)
        bottomSheetDialog.setContentView(view.root)

        with(view) {
            spMonth.setSelection(getMonthPosition(month))
            spYear.setSelection(getYearPosition(year))
            btnFilter.setOnClickListener {
                when(activity) {
                    is PurchaseHistoryActivity -> {
                        dismiss()
                        activity.filterOrders(spMonth.selectedItem.toString(), spYear.selectedItem.toString())
                    }
                    is WalletActivity -> {
                        dismiss()
                        activity.filterTransactions(spMonth.selectedItem.toString(), spYear.selectedItem.toString())
                    }
                }
            }

        }
    }

    private fun getMonthPosition(month: String): Int {
        return when(month) {
            "January" -> 0
            "February" -> 1
            "March" -> 2
            "April" -> 3
            "May" -> 4
            "June" -> 5
            "July" -> 6
            "August" -> 7
            "September" -> 8
            "October" -> 9
            "November" -> 10
            "December" -> 11
            else -> 0
        }
    }

    private fun getYearPosition(year: String): Int {
        return when(year) {
            "2021" -> 0
            "2022" -> 1
            "2023" -> 2
            "2024" -> 3
            "2025" -> 4
            "2026" -> 5
            "2027" -> 6
            "2028" -> 7
            "2029" -> 8
            "2030" -> 9
            "2031" -> 10
            "2032" -> 11
            "2033" -> 12
            "2034" -> 13
            "2035" -> 14
            "2036" -> 15
            "2037" -> 16
            "2038" -> 17
            "2039" -> 18
            "2040" -> 19
            else -> 0
        }
    }

    fun show() = bottomSheetDialog.show()

    fun dismiss() = bottomSheetDialog.dismiss()
}