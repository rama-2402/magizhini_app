package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySplashBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateDataService
import com.voidapp.magizhiniorganics.magizhiniorganics.services.updateDeliveryService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
        binding.progressCircular.animate()

        val sRef = this.getSharedPreferences(Constants.USERS, Context.MODE_PRIVATE)
        val isNewUser = sRef.getBoolean(Constants.LOGIN_STATUS, true)
        val isNewDay = sRef.getString(Constants.DATE, Constants.DATE)

        if (isNewUser) {
            lifecycleScope.launch {
                delay(1500)
                Intent(this@SplashActivity, OnBoardingActivity::class.java).also {
                    startActivity(it)
                    finish()
                }
            }
        } else {
            val updateDeliveryWorkRequest: WorkRequest =
                OneTimeWorkRequestBuilder<updateDeliveryService>()
                    .build()

            WorkManager.getInstance(this).enqueue(updateDeliveryWorkRequest)

            if (isNewDay != Time().getCurrentDate()) {
                val workRequest: WorkRequest =
                    OneTimeWorkRequestBuilder<UpdateDataService>()
                        .build()

                WorkManager.getInstance(this).enqueue(workRequest)
            }

            lifecycleScope.launch {
                delay(2500)
                binding.progressCircular.visibility = View.INVISIBLE
                Intent(this@SplashActivity, HomeActivity::class.java).also {
                    startActivity(it)
//                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }

        }
    }
}