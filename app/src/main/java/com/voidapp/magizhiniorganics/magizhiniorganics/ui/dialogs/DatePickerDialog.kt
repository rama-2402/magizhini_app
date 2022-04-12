package com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogDataPickerBinding
import java.util.*

class DatePickerDialog: DialogFragment() {

    private var _binding: DialogDataPickerBinding? = null
    private val binding get() = _binding!!

//    private var calendar: Calendar? = null
//    private var year: Int = 0
//    private var month: Int = 0
//    private var day: Int = 0

    companion object {
        fun newInstance(startDateLong: Long?, endDateLong: Long?): DatePickerDialog {
            val args = Bundle()
            startDateLong?.let {
                args.putLong("start", it)
            }
            endDateLong?.let {
                args.putLong("end", it)
            }
            val fragment = DatePickerDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(STYLE_NORMAL, R.style.CustomAlertDialog)
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogDataPickerBinding.inflate(LayoutInflater.from(context))
        isCancelable = true

        val startDate = arguments?.getLong("start")
        val endDate = arguments?.getLong("end")

//        calendar = Calendar.getInstance()
//        year = calendar?.get(Calendar.YEAR)!!
//        month = calendar?.get(Calendar.MONTH)!!
//        day = calendar?.get(Calendar.DAY_OF_MONTH)!!

        populateDatePicker(startDate, endDate)

        return binding.root
    }

    private fun populateDatePicker(startDate: Long?, endDate: Long?) {
        val today = Calendar.getInstance()
        binding.apply {
            datePicker.init(today.get(Calendar.YEAR), today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)

            ) { view, year, month, day ->
                val month = month + 1
                val msg = "You Selected: $day/$month/$year"
                tvSelect.text = msg
//                Toast.makeText(, msg, Toast.LENGTH_SHORT).show()
            }
//            endDate?.let {
//                datePicker.maxDate = it
//            }
//            startDate?.let {
//                datePicker.minDate = it
//            }
//            calendar?.set(datePicker.year, datePicker.month, datePicker.dayOfMonth)

            tvSelect.setOnClickListener {
//                Log.e("qw", "populateDatePicker: ${TimeUtil().getCustomDate(dateLong = calendar!!.timeInMillis)}", )
            }
        }
    }
}