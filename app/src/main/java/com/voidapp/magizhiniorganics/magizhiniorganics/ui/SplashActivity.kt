package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.INT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LOGIN_STATUS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_ID
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.SharedPref
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import kotlin.math.abs

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity(), KodeinAware {

    private lateinit var binding: ActivitySplashBinding
    override val kodein: Kodein by kodein()

    private val fbRepository: FirestoreRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
        binding.progressCircular.animate()

        val sRef = this.getSharedPreferences(Constants.USERS, Context.MODE_PRIVATE)
        val isNewUser = sRef.getBoolean(LOGIN_STATUS, true)
        val isNewDay = sRef.getString(Constants.DATE, Constants.DATE)
        val month = sRef.getInt("month", TimeUtil().getMonthNumber())
        val navigation = intent.getStringExtra("navigate")

        binding.tvStatus.setOnClickListener {
            checkNetwork(isNewDay!!, isNewUser, month, navigation)
        }

        checkNetwork(isNewDay!!, isNewUser, month, navigation)
    }

    private fun checkNetwork(isNewDay: String, isNewUser: Boolean, month: Int, navigation: String?) {
        if (!NetworkHelper.isOnline(this)) {
            showRetry()
            showToast(this, "Please check your Internet connection")
        } else {
            hideRetry()
            backgroundCheck(isNewDay, isNewUser, month, navigation)
        }
    }

    private fun backgroundCheck(isNewDay: String, isNewUser: Boolean, month: Int, navigation: String?) {
        if (isNewUser) {
            lifecycleScope.launch {
                delay(1000)
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
                        updateDatabaseWorkRequest(false, isNewDay, navigation)
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
        if (isNewDay != TimeUtil().getCurrentDate()) {
            if (wipe) {
                SharedPref(this).putData("month", INT, TimeUtil().getMonthNumber())
                startWork("wipe", navigation)
            }else {
                startWork("", navigation)
            }
        } else {
            navigateToHomeScreen(true, navigation)
        }
    }

    private fun startWork(wipe: String, navigation: String?) {
        val userID = SharedPref(this).getData(USER_ID, STRING, "")
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateDataService>()
                .setInputData(
                    workDataOf(
                        "wipe" to wipe,
                        "id" to userID
                    )
                )
                .build()

        WorkManager.getInstance(this).enqueue(workRequest)
        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(workRequest.id)
            .observe(this) {
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
                    WorkInfo.State.SUCCEEDED -> {
                        navigateToHomeScreen(true, navigation)
                    }
                    else -> {
//                        navigateToHomeScreen()
                    }
                }
            }
    }

    private fun navigateToHomeScreen(newDayCheck: Boolean, navigation: String?) = lifecycleScope.launch {
        if (fbRepository.updateNotifications(SharedPref(this@SplashActivity).getData(USER_ID, STRING, "").toString())) {
            binding.progressCircular.remove()
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
            binding.progressCircular.remove()
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