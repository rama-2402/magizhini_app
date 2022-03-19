package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.viewpager2.widget.ViewPager2
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.viewPagerAdapter.ViewPagerAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.viewPagerAdapter.ViewPagerListener
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityOnBoardingBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin.SignInActivity

class OnBoardingActivity: BaseActivity() {

    private lateinit var binding: ActivityOnBoardingBinding

    private var titleList = mutableListOf<String>()
    private var bodyOneList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            binding = DataBindingUtil.setContentView(
                this@OnBoardingActivity,
                R.layout.activity_on_boarding
            )
            postToList()
            activityInit()

    }

    private fun activityInit() {
        binding.viewPager.adapter = ViewPagerAdapter(
            this,
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

                override fun nextPage(position: Int) {
                    binding.viewPager.currentItem = position + 1
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
        addToList("Pure Organic", "Get a wide variety of Farm Fresh Organic Products in a single click")
        addToList("Highest Quality", "High Quality Premium grade products personally handpicked just for you")
        addToList("Affordable Price", "Get fresh Vegetables and groceries delivered daily at your doorstep at lower than market price")
    }
}