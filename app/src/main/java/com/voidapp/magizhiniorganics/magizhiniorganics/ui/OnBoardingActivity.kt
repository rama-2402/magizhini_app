package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.viewPagerAdapter.ViewPagerAdapter
import com.voidapp.magizhiniorganics.magizhiniorganics.adapter.viewpager.viewPagerAdapter.ViewPagerListener
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityOnBoardingBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin.SignInActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadInAnimation
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.fadOutAnimation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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

            val window: Window = this.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.WHITE

            binding.ivBackground.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in_top_bounce))

            lifecycleScope.launch {
                delay(900)
                binding.apply {
                    imageView.visible()
                    ivBottomCircle.visible()
//                    ivGradientCircle.visible()
                    btnNext.fadInAnimation()
                    viewPager.visible()
                }
                postToList()
                activityInit()
                listeners()
            }
    }

    private fun listeners() {
        binding.btnNext.setOnClickListener {
            lifecycleScope.launch {
                binding.apply {
                    btnNext.fadOutAnimation()
                    imageView.hide()
                    ivBottomCircle.hide()
                    ivGradientCircle.hide()
                    viewPager.hide()
                    ciPageIndicator.hide()
                    ivBackground.startAnimation(AnimationUtils.loadAnimation(this@OnBoardingActivity, R.anim.slide_out_top))
                    delay(810)
                    ivBackground.hide()
                }
                Intent(this@OnBoardingActivity, SignInActivity::class.java).also {
                    startActivity(it)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    finishAffinity()
                }
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                if (position == 2) {
                    binding.nextScreen.fadOutAnimation()
                    binding.nextScreen.remove()
                    binding.tvSignIn.fadInAnimation()
                }
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
//                Log.e("Selected_Page", position.toString())
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
            }
        })
//
//        binding.tvSignIn.setOnClickListener {
//            Intent(this@OnBoardingActivity, SignInActivity::class.java).also {
//                startActivity(it)
//                finish()
//                finishAffinity()
//            }
//        }
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
                    binding.nextScreen.fadOutAnimation()
                    binding.nextScreen.remove()
                    binding.tvSignIn.fadInAnimation()
//                    binding.viewPager.currentItem = position + 1
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
        addToList("Pure Farm Fresh", "Get a wide variety of Farm Fresh Products in a single click")
        addToList("Highest Quality", "High Quality Premium grade products personally handpicked just for you")
        addToList("Affordable Price", "Get fresh Vegetables and groceries delivered daily at your doorstep at lower than market price")
    }
}