package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.aminography.primecalendar.PrimeCalendar
import com.aminography.primecalendar.civil.CivilCalendar
import com.aminography.primecalendar.common.CalendarFactory
import com.aminography.primecalendar.common.CalendarType
import com.aminography.primecalendar.common.operators.date
import com.aminography.primecalendar.common.operators.dayOfYear
import com.aminography.primedatepicker.picker.PrimeDatePicker
import com.aminography.primedatepicker.picker.callback.MultipleDaysPickCallback
import com.aminography.primedatepicker.picker.callback.RangeDaysPickCallback
import com.aminography.primedatepicker.picker.callback.SingleDayPickCallback
import com.google.type.DateTime
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptionHistory.SubscriptionHistoryActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.subscriptions.SubscriptionProductViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.wallet.WalletActivity
import java.util.*


class DatePickerLib {


    fun pickDob(activity: ProfileActivity) {
        //val themeFactory = object: DarkThemeFactory() {}

            val callback = SingleDayPickCallback { date ->
                activity.onDobSelected(date.longDateString)
            }
                val today = CivilCalendar()
                val datePicker = PrimeDatePicker.dialogWith(today)
                    .pickSingleDay(callback)
                    //.applyTheme(themeFactory)
                    .build()
                datePicker.show(activity.supportFragmentManager, "magizhiniOrganics")
        }
    fun pickSingleDate(activity: WalletActivity) {
        //val themeFactory = object: DarkThemeFactory() {}

            val callback = SingleDayPickCallback { date ->
                activity.filterDate(date.timeInMillis)
            }
                val today = CivilCalendar()
                val datePicker = PrimeDatePicker.dialogWith(today)
                    .pickSingleDay(callback)
                    //.applyTheme(themeFactory)
                    .build()
                datePicker.show(activity.supportFragmentManager, "magizhiniOrganics")
        }
    fun startSubscriptionDate(activity: SubscriptionProductActivity) {
        //val themeFactory = object: DarkThemeFactory() {}

            val callback = SingleDayPickCallback { date ->
                activity.filterDate(date.timeInMillis)
            }
                val today = CivilCalendar()
                val datePicker = PrimeDatePicker.dialogWith(today)
                    .pickSingleDay(callback)
                    //.applyTheme(themeFactory)
                    .build()
                datePicker.show(activity.supportFragmentManager, "magizhiniOrganics")
        }
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
