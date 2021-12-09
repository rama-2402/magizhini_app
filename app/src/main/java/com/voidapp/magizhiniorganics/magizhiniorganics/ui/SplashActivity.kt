package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySplashBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.CleanDatabaseService
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateDataService
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateDeliveryService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs
import androidx.work.WorkManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
        binding.progressCircular.animate()

        val sRef = this.getSharedPreferences(Constants.USERS, Context.MODE_PRIVATE)
        val isNewUser = sRef.getString(Constants.USER_ID, "")
        val isNewDay = sRef.getString(Constants.DATE, Constants.DATE)
        val month = sRef.getInt("month", TimeUtil().getMonthNumber())

        binding.tvStatus.setOnClickListener {
            checkNetwork(isNewDay!!, isNewUser!!, month)
        }

        checkNetwork(isNewDay!!, isNewUser!!, month)
    }

    private fun checkNetwork(isNewDay: String, isNewUser: String, month: Int) {
        if (!NetworkHelper.isOnline(this)) {
            showRetry()
            showToast(this, "Please check your Internet connection")
        } else {
            hideRetry()
            backgroundCheck(isNewDay, isNewUser, month)
        }
    }

    private fun backgroundCheck(isNewDay: String, isNewUser: String, month: Int) {
        if (isNewUser == "") {
            lifecycleScope.launch {
                delay(1500)
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
                        val cleanDatabaseWorkRequest: WorkRequest =
                            OneTimeWorkRequestBuilder<CleanDatabaseService>().build()

                        WorkManager.getInstance(this@SplashActivity).enqueue(cleanDatabaseWorkRequest)
                        updateDatabaseWorkRequest(false, isNewDay)
                    }
                    abs(month - TimeUtil().getMonthNumber()) > 1 -> {
                        showToast(
                            this@SplashActivity,
                            "Updating the product catalog. Please Wait..."
                        )
                        updateDatabaseWorkRequest(true, isNewDay)
                    }
                    else -> updateDatabaseWorkRequest(false, isNewDay)
                }
            }
        }
    }

    private fun updateDatabaseWorkRequest(wipe: Boolean, isNewDay: String) {
//        if (isNewDay == TimeUtil().getCurrentDate()) {
//            if (wipe) {
//                startWork("wipe")
//            }else {
                startWork("")
//            }
//        } else {
//            navigateToHomeScreen()
//        }
    }

    private fun startWork(wipe: String) {
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateDataService>()
                .setInputData(
                    workDataOf(
                        "wipe" to wipe
                    )
                )
                .build()

        WorkManager.getInstance(this).enqueue(workRequest)
        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(workRequest.id)
            .observe(this, {
                when (it.state) {
                    WorkInfo.State.CANCELLED -> {
                        showRetry()
                        showToast(
                            this,
                            "Failed to update product catalog. Try restarting the app again",
                            LONG
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        showRetry()
                        showToast(
                            this,
                            "Failed to update product catalog. Try restarting the app again",
                            LONG
                        )
                    }
                    else -> {
                        navigateToHomeScreen()
                    }
                }
            })
    }

    private fun navigateToHomeScreen() = lifecycleScope.launch {
        delay(1000)
        binding.progressCircular.remove()
        Intent(this@SplashActivity, HomeActivity::class.java).also {
            startActivity(it)
            finish()
            finishAffinity()
        }
    }

    private fun showRetry() {
        binding.apply {
            progressCircular.remove()
            tvStatus.visible()
        }
    }
    private fun hideRetry() {
        binding.apply {
            progressCircular.visible()
            tvStatus.remove()
        }
    }
}