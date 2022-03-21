package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogCalendarFilterBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.AddressDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.dialog_listener.CalendarFilerDialogClickListener
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.purchaseHistory.PurchaseHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import java.util.*

class CalendarFilterDialog: DialogFragment(){

    companion object {
        private const val MAX_YEAR = 2050

        fun newInstance(
            month: String = "January",
            year: Int = 2022
        ): CalendarFilterDialog {
            val args = Bundle()
            args.putString("month", month)
            args.putInt("year", year)
            val fragment = CalendarFilterDialog()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: DialogCalendarFilterBinding? = null
    private val binding get() = _binding!!

    private var onItemClickListener: CalendarFilerDialogClickListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCalendarFilterBinding.inflate(requireActivity().layoutInflater)
        isCancelable = false

        val month = arguments?.getString("month") ?: "January"
        val year = arguments?.getInt("year") ?: 2022

        binding.pickerMonth.run {
            minValue = 0
            maxValue = 11
            value = getMonthPosition(month)
            displayedValues = arrayOf("Jan","Feb","Mar","Apr","May","June","July",
                "Aug","Sep","Oct","Nov","Dec")

        }

        binding.pickerYear.run {
            minValue = 2022
            maxValue = MAX_YEAR
            value = year
        }

        binding.tvOk.setOnClickListener {
            onItemClickListener?.selectedFilter(getMonthFromPosition(binding.pickerMonth.value), binding.pickerYear.value.toString())
        }

        binding.tvCancel.setOnClickListener {
            onItemClickListener?.cancelDialog()
        }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.CustomAlertDialog)
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {

        super.onAttach(context)

        onItemClickListener = if (context is CalendarFilerDialogClickListener) {
            context
        } else {
            throw RuntimeException (
                context.toString() + "CalendarFilerClickListener needed"
            )
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

    private fun getMonthFromPosition(monthNumber: Int): String {
        return when(monthNumber) {
            0 -> "January"
            1 -> "February"
            2 -> "March"
            3 -> "April"
            4 -> "May"
            5 -> "June"
            6 -> "July"
            7 -> "August"
            8 -> "September"
            9 -> "October"
            10 -> "November"
            11 -> "December"
            else -> "January"
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

    override fun onDetach() {
        _binding = null
        super.onDetach()
    }

}
