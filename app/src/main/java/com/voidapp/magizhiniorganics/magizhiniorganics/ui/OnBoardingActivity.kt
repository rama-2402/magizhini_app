package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.viewPagerAdapter.ViewPagerAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.viewPagerAdapter.ViewPagerListener
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityOnBoardingBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.GetDataIntentService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Time
import kotlinx.coroutines.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class OnBoardingActivity: BaseActivity() {

    private lateinit var binding: ActivityOnBoardingBinding

    private var titleList = mutableListOf<String>()
    private var bodyOneList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sRef = this.getSharedPreferences(Constants.USERS, Context.MODE_PRIVATE)
        val isNewUser = sRef.getBoolean(Constants.LOGIN_STATUS, true)
        val isNewDay = sRef.getString(Constants.DATE, Constants.DATE)

        if (isNewUser) {
            setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
            binding = DataBindingUtil.setContentView(
                this@OnBoardingActivity,
                R.layout.activity_on_boarding
            )
            postToList()
            activityInit()
        } else {
//            if (isNewDay != Time().getCurrentDateAndTime()) {
                Intent(this, GetDataIntentService::class.java).also {
                    startService(it)
                }
//            }
            Intent(this@OnBoardingActivity, HomeActivity::class.java).also {
                startActivity(it)
            }
        }
    }

    private fun activityInit() {

        binding.viewPager.adapter = ViewPagerAdapter(
            titleList,
            bodyOneList,
            object : ViewPagerListener {
                override fun signIn() {
                    Intent(this@OnBoardingActivity, SignInActivity::class.java).also {
                        startActivity(it)
                        finish()
                        finishAffinity()
                    }
                }
            }
        )

        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        binding.ciPageIndicator.setViewPager(binding.viewPager)

        binding.viewPager
    }

    private fun addToList(
        title: String,
        bodyOne: String = ""
        ) {
        titleList.add(title)
        bodyOneList.add(bodyOne)
    }

    private fun postToList() {
        addToList("Pure Organic", "Get Fresh, Organic Vegatables and groceries daily at your doorstep")
        addToList("High Quality", "High Quality personally handpicked items just for you")
        addToList("Affordable Price", "Farm Fresh groceries for your Good Health at Prices lower than market price")
    }
}