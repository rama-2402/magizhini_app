package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySplashBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.CleanDatabaseService
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateDataService
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateDeliveryService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.INT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LOGIN_STATUS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.QUARTER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_ID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity(), KodeinAware {

    private lateinit var binding: ActivitySplashBinding
    override val kodein: Kodein by kodein()

    private val fbRepository: FirestoreRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)

        val sRef = this.getSharedPreferences(Constants.USERS, Context.MODE_PRIVATE)
        val isNewUser = sRef.getBoolean(LOGIN_STATUS, true)
        val isNewDay = sRef.getString(Constants.DATE, Constants.DATE)
        val month = sRef.getInt("month", TimeUtil().getMonthNumber())
        val navigation = intent.getStringExtra("navigate")

        NetworkManagerUtil(this).observe(this) {
            it?.let {
                if (it) {
                    binding.tvStatus.remove()
                    lifecycleScope.launch {
                        binding.apply {
                            ivLogo.startAnimation(
                                AnimationUtils.loadAnimation(
                                    this@SplashActivity,
                                    R.anim.fade_in
                                )
                            )
                            ivLogo.visible()
                            delay(100)
                            ivBagOne.startAnimation(
                                AnimationUtils.loadAnimation(
                                    this@SplashActivity,
                                    R.anim.slide_in_left
                                )
                            )
                            ivBagOne.visible()
                            delay(64)
                            ivBagThree.startAnimation(
                                AnimationUtils.loadAnimation(
                                    this@SplashActivity,
                                    R.anim.slide_in_right
                                )
                            )
                            ivBagThree.visible()
                            delay(90)
                            ivBagTwo.startAnimation(
                                AnimationUtils.loadAnimation(
                                    this@SplashActivity,
                                    R.anim.slide_in_left
                                )
                            )
                            ivBagTwo.visible()
                            delay(34)
                            ivBagFour.startAnimation(
                                AnimationUtils.loadAnimation(
                                    this@SplashActivity,
                                    R.anim.slide_in_right
                                )
                            )
                            ivBagFour.visible()
                        }
                    }
                    binding.progressCircular.visible()
                    binding.progressCircular.animate()
                    backgroundCheck(isNewDay!!, isNewUser, month, navigation)
                } else {
                    binding.ivLogo.fadInAnimation()
                    binding.ivLogo.visible()
                    showToast(this, "Please check your Internet connection")
                }
            } ?: let {
                binding.ivLogo.fadInAnimation()
                binding.ivLogo.visible()
                showToast(this, "Please check your Internet connection")
            }
        }
        binding.tvStatus.visible()
    }

    private fun backgroundCheck(
        isNewDay: String,
        isNewUser: Boolean,
        month: Int,
        navigation: String?
    ) {
        if (isNewUser) {
            lifecycleScope.launch {
//                delay(3000)
                delay(1000)
//                hideAnimation()
//                delay(800)
                Intent(this@SplashActivity, OnBoardingActivity::class.java).also {
                    startActivity(it)
                    finish()
                }
            }
        } else {
            val updateDeliveryWorkRequest: WorkRequest =
                OneTimeWorkRequestBuilder<UpdateDeliveryService>()
                    .build()

            WorkManager.getInstance(this).enqueue(updateDeliveryWorkRequest)
            lifecycleScope.launch {
                when {

                    abs(month - TimeUtil().getMonthNumber()) == 1 -> {
                        showToast(
                            this@SplashActivity,
                            "Updating the product catalog. Please Wait..."
                        )
//                        val cleanDatabaseWorkRequest: WorkRequest =
//                            OneTimeWorkRequestBuilder<CleanDatabaseService>().build()
//
//                        WorkManager.getInstance(this@SplashActivity)
//                            .enqueue(cleanDatabaseWorkRequest)
//                        updateDatabaseWorkRequest(false, isNewDay, navigation)
                        updateDatabaseWorkRequest(true, isNewDay, navigation)
                    }
                    abs(month - TimeUtil().getMonthNumber()) > 1 -> {
                        showToast(
                            this@SplashActivity,
                            "Updating the product catalog. Please Wait..."
                        )
                        updateDatabaseWorkRequest(true, isNewDay, navigation)
                    }
                    else -> updateDatabaseWorkRequest(false, isNewDay, navigation)
                }
            }
        }
    }

    private fun updateDatabaseWorkRequest(wipe: Boolean, isNewDay: String, navigation: String?) {
        if (
            isNewDay != TimeUtil().getCurrentDate() ||
            TimeUtil().getQuarterOfTheDay() != SharedPref(this).getData(QUARTER, STRING, "1")
                .toString().toInt()
        ) {
//            SharedPref(this).putData(QUARTER, STRING, TimeUtil().getQuarterOfTheDay().toString())
            if (wipe) {
                SharedPref(this).putData("month", INT, TimeUtil().getMonthNumber())
                startWork("wipe", navigation)
            } else {
                startWork("", navigation)
            }
        } else {
//            startWork("", navigation)
            navigateToHomeScreen(false, navigation)
        }
    }

    private fun startWork(wipe: String, navigation: String?) {
        val userID = SharedPref(this).getData(USER_ID, STRING, "").toString()

        if (userID != "" && userID != "null") {
            val workRequest: WorkRequest =
                if (wipe == "wipe") {
                    OneTimeWorkRequestBuilder<UpdateDataService>()
                        .setInputData(
                            workDataOf(
                                "wipe" to wipe,
                                "id" to userID
                            )
                        )
                        .build()
                } else {
                    OneTimeWorkRequestBuilder<UpdateDataService>()
                        .setInputData(
                            workDataOf(
                                "id" to userID
                            )
                        )
                        .build()
//                    PeriodicWorkRequestBuilder<UpdateDataService>(12, TimeUnit.HOURS)
//                        .setInputData(
//                            workDataOf(
//                                "id" to userID
//                            )
//                        )
//                        .build()
                }
            WorkManager.getInstance(this).enqueue(workRequest)
            navigateToHomeScreen(true, navigation)
        } else {
            val request = OneTimeWorkRequestBuilder<UpdateDataService>()
                .setInputData(
                    workDataOf(
                        "id" to ""
                    )
                )
                .build()

            WorkManager.getInstance(this).enqueue(request)

            navigateToHomeScreen(true, navigation)
        }

//        WorkManager.getInstance(this)
//            .getWorkInfoByIdLiveData(workRequest.id)
//            .observe(this) {
//                when (it.state) {
//                    WorkInfo.State.CANCELLED -> {
////                        showRetry()
//                        showToast(
//                            this,
//                            "Failed to update product catalog. Try restarting the app again",
//                            LONG
//                        )
//                    }
//                    WorkInfo.State.FAILED -> {
////                        showRetry()
//                        showToast(
//                            this,
//                            "Failed to update product catalog. Try restarting the app again",
//                            LONG
//                        )
//                    }
//                    WorkInfo.State.SUCCEEDED -> {
////                        val periodicWorker: WorkRequest =
////                            if (wipe == "") {
////                                PeriodicWorkRequestBuilder<UpdateDataService>(12, TimeUnit.HOURS)
////                                    .setInitialDelay(TimeUtil().getHoursBeforeMidNight(), TimeUnit.HOURS)
////                                    .setInputData(
////                                        workDataOf(
////                                            "wipe" to wipe,
////                                            "id" to userID
////                                        )
////                                    )
////                                    .build()
////                            } else {
////                                PeriodicWorkRequestBuilder<UpdateDataService>(12, TimeUnit.HOURS)
////                                    .setInputData(
////                                        workDataOf(
////                                            "id" to userID
////                                        )
////                                    )
////                                    .build()
////                            }
////                        WorkManager.getInstance(this).enqueue(periodicWorker)
//                        navigateToHomeScreen(true, navigation)
//                    }
//                    else -> {
////                        navigateToHomeScreen()
//                    }
//                }
//            }
    }

    private fun navigateToHomeScreen(newDayCheck: Boolean, navigation: String?) =
        lifecycleScope.launch {
            if (fbRepository.updateNotifications(
                    SharedPref(this@SplashActivity).getData(
                        USER_ID,
                        STRING,
                        ""
                    ).toString()
                )
            ) {
//            binding.progressCircular.remove()
//                delay(3000)
                delay(1000)
//                hideAnimation()
//                delay(800)
                Intent(this@SplashActivity, HomeActivity::class.java).also {
//                if (
//                    SharedPref(this@SplashActivity).getData(DOB, STRING, "").toString() == "${TimeUtil().getCurrentDateNumber()}/${TimeUtil().getMonthNumber()}"
//                ) {
//                    it.putExtra("day", newDayCheck)
//                } else {
                    navigation?.let { nav ->
                        it.putExtra("navigate", nav)
                    }
                    it.putExtra("day", newDayCheck)
//                }
                    startActivity(it)
                    finish()
                    finishAffinity()
                }
            } else {
//                delay(3000)
                delay(1000)
//                hideAnimation()
//                delay(800)
//            binding.progressCircular.remove()
                Intent(this@SplashActivity, HomeActivity::class.java).also {
//                if (
//                    SharedPref(this@SplashActivity).getData(DOB, STRING, "").toString() == "${TimeUtil().getCurrentDateNumber()}/${TimeUtil().getMonthNumber()}"
//                ) {
//                    it.putExtra("day", newDayCheck)
//                } else {
                    navigation?.let { nav ->
                        it.putExtra("navigate", nav)
                    }
                    it.putExtra("day", newDayCheck)
//                }
                    startActivity(it)
                    finish()
                    finishAffinity()
                }
            }
        }
}