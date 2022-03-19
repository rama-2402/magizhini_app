package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductActivity
import java.util.*


object DatePickerLib {

    fun showCalendar(context: Context, activity: Activity, minDate: Long?, maxDate: Long?, selectedDate: HashMap<String, Long>?) {
        val calendar = Calendar.getInstance()
        val dialog = DatePickerDialog(context, { _, year, month, day_of_month ->
            calendar[Calendar.YEAR] = year
            calendar[Calendar.MONTH] = month
            calendar[Calendar.DAY_OF_MONTH] = day_of_month
            selectedCalendarDate(activity, calendar.timeInMillis)
        }, calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DAY_OF_MONTH])
        //this part is to preselect the already selected startsub date
        selectedDate?.let {
            dialog.updateDate(
                TimeUtil().getYear(dateLong = selectedDate["year"].toString().toLong()).toInt(),
                TimeUtil().getMonthNumber(dateLong = selectedDate["month"].toString().toLong()),
                TimeUtil().getDateNumber(dateLong = selectedDate["date"].toString().toLong()).toInt()
            )
        }
        minDate?.let {
            dialog.datePicker.minDate = it
        }
        maxDate?.let {
            dialog.datePicker.maxDate = it
        }
        dialog.show()
    }

    private fun selectedCalendarDate(activity: Activity, date: Long) {
        when(activity) {
            is SubscriptionProductActivity -> {
                activity.selectedCalendarDate(date)
            }
            is ProfileActivity -> {
                activity.selectedCalendarDate(date)
            }
        }
    }


//    fun pickDob(activity: ProfileActivity) {
//        //val themeFactory = object: DarkThemeFactory() {}
//
//            val callback = SingleDayPickCallback { date ->
//                activity.onDobSelected(date.timeInMillis)
//            }
//                val today = CivilCalendar()
//                val datePicker = PrimeDatePicker.dialogWith(today)
//                    .pickSingleDay(callback)
//                    //.applyTheme(themeFactory)
//                    .build()
//                datePicker.show(activity.supportFragmentManager, "magizhiniOrganics")
//        }

//    fun startSubscriptionDate(activity: SubscriptionProductActivity) {
//        //val themeFactory = object: DarkThemeFactory() {}
//
//            val callback = SingleDayPickCallback { date ->
//                activity.filterDate(date.timeInMillis)
//            }
//            val min = CalendarFactory.newInstance(CalendarType.CIVIL)
//            min.add(Calendar.DAY_OF_MONTH, +1)
//                val today = CivilCalendar()
//                val datePicker = PrimeDatePicker.dialogWith(today)
//                    .pickSingleDay(callback)
//                    .minPossibleDate(min)
//                    //.applyTheme(themeFactory)
//                    .build()
//                datePicker.show(activity.supportFragmentManager, "magizhiniOrganics")
//        }

//    fun pickMultipleDates(activity: SubscriptionHistoryActivity, minDate: Int, maxDate: Int) {
//        val callback = MultipleDaysPickCallback { dates ->
//            activity.cancellationDates(dates)
//        }
//        val today = CalendarFactory.newInstance(CalendarType.CIVIL)
////
//        val min = CalendarFactory.newInstance(CalendarType.CIVIL)
//        min.add(Calendar.DAY_OF_MONTH, +minDate)
//        val max = CalendarFactory.newInstance(CalendarType.CIVIL)
//        max.add(Calendar.DAY_OF_MONTH, +maxDate)
//
//        Log.e("qqqq", "pickMultipleDates: $minDate $maxDate", )
//
//        val datePicker = PrimeDatePicker
//            .dialogWith(today)
//            .pickMultipleDays(callback)
//            .minPossibleDate(min)
//            .maxPossibleDate(max)
////                .applyTheme(themeFactory)
//            .build()
//
//       datePicker.show(activity.supportFragmentManager, "magizhiniOrganics")
//    }


//
//    fun purchase(activity: Activity) {
//
//              if (activity is PurchaseActivity) {
//
//                val callback = SingleDayPickCallback { date ->
//                    activity.setDate(date.shortDateString)
//                }
//
//                val today = CivilCalendar()
//                val datePicker = PrimeDatePicker.dialogWith(today)
//                    .pickSingleDay(callback)
//                    //.applyTheme(themeFactory)
//                    .build()
//                datePicker.show(activity.supportFragmentManager, "Void_Tracker")
//            }
//    }
//
//    fun purchaseHistoryFilter(activity: PurchaseHistoryActivity) {
//        val callback = SingleDayPickCallback { date ->
//            activity.dateFilter(date.shortDateString)
//        }
//
//        val today = CivilCalendar()
//        val datePicker = PrimeDatePicker.dialogWith(today)
//            .pickSingleDay(callback)
//            //.applyTheme(themeFactory)
//            .build()
//        datePicker.show(activity.supportFragmentManager, "Void_Tracker")
//    }
//
//    fun multipleDays(activity: PurchaseHistoryActivity) {
//
//        val callback = MultipleDaysPickCallback { it ->
//            val dates = it.joinToString {it.shortDateString}
//        }
//        val today = CivilCalendar()
//        val datePicker = PrimeDatePicker
//            .dialogWith(today)
//            .pickMultipleDays(callback)
////                .applyTheme(themeFactory)
//            .build()
//                datePicker.show(activity.supportFragmentManager, "Void_Tracker")
//    }

//    fun dataRange(activity: PurchaseHistoryActivity) {
//        val callback = RangeDaysPickCallback { start, end ->
//            val fromDate = start.shortDateString
//            val toDate = end.shortDateString
//            activity.applyDateRangeFilter(start.shortDateString, end.shortDateString)
//        }
//        val today = CivilCalendar()
//        val datePicker = PrimeDatePicker
//            .dialogWith(today)
//            .pickRangeDays(callback)
////                .applyTheme(themeFactory)
//            .build()
//
//        datePicker.show(activity.supportFragmentManager, "Void_Tracker")
//    }
}
