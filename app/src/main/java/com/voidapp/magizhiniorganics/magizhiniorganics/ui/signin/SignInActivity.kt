package com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.AnimationUtils
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.utils.FadeViewHelper.Companion.DEFAULT_ANIMATION_DURATION
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySignInBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.GetOrderHistoryService
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateDataService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BOOLEAN
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LOGIN_STATUS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PHONE_NUMBER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import ticker.views.com.ticker.widgets.circular.timer.callbacks.CircularViewCallback
import ticker.views.com.ticker.widgets.circular.timer.view.CircularView
import java.util.concurrent.TimeUnit


class SignInActivity : BaseActivity(), KodeinAware, View.OnClickListener {

    override val kodein by kodein()

    private lateinit var binding: ActivitySignInBinding
    private lateinit var viewModel: SignInViewModel
    private val factory: SignInViewModelFactory by instance()

    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var mCallBacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null
    private var mVerificationId: String? = null

    private var mPhoneNumber: String = ""
    private var mCurrentUserID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)
        viewModel = ViewModelProvider(this, factory)[SignInViewModel::class.java]
        binding.viewmodel = viewModel


        lifecycleScope.launch {
            binding.apply {
                delay(400)
                ivLogo.fadInAnimation()
                clBody.startAnimation(AnimationUtils.loadAnimation(this@SignInActivity, R.anim.slide_up))
                ivLogo.visible()
                clBody.visible()
                delay(100)
                    ivBagOne.startAnimation(AnimationUtils.loadAnimation(this@SignInActivity, R.anim.slide_in_left))
                ivBagOne.visible()
                delay(64)
                    ivBagThree.startAnimation(AnimationUtils.loadAnimation(this@SignInActivity, R.anim.slide_in_right))
                ivBagThree.visible()
                delay(90)
                    ivBagTwo.startAnimation(AnimationUtils.loadAnimation(this@SignInActivity, R.anim.slide_in_left))
                ivBagTwo.visible()
                delay(34)
                    ivBagFour.startAnimation(AnimationUtils.loadAnimation(this@SignInActivity, R.anim.slide_in_right))
                ivBagFour.visible()
                    playFruitsFallingAnimation(ivApple)
                    delay(100)
                    playFruitsFallingAnimation(ivMilk)
                    delay(50)
                    playFruitsFallingAnimation(ivBroccoli)
                    delay(80)
                    playFruitsFallingAnimation(ivTomato)
                    delay(100)
                    playFruitsFallingAnimation(ivPotato)
                    delay(40)
                    playFruitsFallingAnimation(ivBanana)
                    delay(90)
                    playFruitsFallingAnimation(ivPear)
                    delay(50)
                    playFruitsFallingAnimation(ivBeet)
                    delay(80)
                    playFruitsFallingAnimation(ivChili)
                    delay(120)
                    playFruitsFallingAnimation(ivLemon)
                    delay(30)
                    playFruitsFallingAnimation(ivCarrot)
                delay(382)
                    playFruitsFallingAnimation(ivAppleOne)
                    delay(100)
                    playFruitsFallingAnimation(ivMilkOne)
                    delay(50)
                    playFruitsFallingAnimation(ivBroccoliOne)
                    delay(80)
                    playFruitsFallingAnimation(ivTomatoOne)
                    delay(20)
                    playFruitsFallingAnimation(ivPotatoOne)
                    delay(40)
                    playFruitsFallingAnimation(ivBananaOne)
                    delay(90)
                    playFruitsFallingAnimation(ivPearOne)
                    delay(50)
                    playFruitsFallingAnimation(ivBeetOne)
                    delay(80)
                    playFruitsFallingAnimation(ivChiliOne)
                    delay(120)
                    playFruitsFallingAnimation(ivLemonOne)
                    delay(30)
                    playFruitsFallingAnimation(ivCarrotOne)
            }
        }


        layoutVisibility("pre")

        // Verifying the mobile number and sending the OTP
//        mCallBacks =

        initFlow()
        clickListeners()
    }

    private fun playFruitsFallingAnimation(view: View) {
        lifecycleScope.launch {
            view.visible()
            while (true) {
                val screenHeight: Int = resources.displayMetrics.heightPixels
                val positionAnimator = ValueAnimator.ofFloat(-100f, screenHeight.toFloat())
                positionAnimator.addUpdateListener {
                    val value = it.animatedValue as Float
                    view.translationY = value
                }
                val rotationAnimator = ObjectAnimator.ofFloat(view, "rotation", 0f, 180f)
                val animatorSet = AnimatorSet()
                animatorSet.play(positionAnimator).with(rotationAnimator)
                animatorSet.duration = 2969
                animatorSet.start()
                delay(2542)
            }
        }
    }


    private fun initFlow() {
        lifecycleScope.launch {
            viewModel.loginStatus.collect { status ->
                when(status) {
                    "" -> {}
                    "failed" -> {
                        hideProgressDialog()
                        showErrorSnackBar("Login Failed! Recheck OTP or Try again later", true)
                    }
                    "imageFailed" -> {
                        hideProgressDialog()
                        showErrorSnackBar("Server Error! Profile Picture upload Failed. Try again later", true)
                    }
                    "profileFailed" -> {
                        hideProgressDialog()
                        showErrorSnackBar("Server Error! Profile creation failed. Try again Later", true)
                    }
                    "success" -> {
                        hideProgressDialog()
                        stopTimer()
                        showErrorSnackBar("Profile Created Successfully", false)
                        navigateToHomePage()
                    }
                    else -> {
                        stopTimer()
                        newUserVerification()
                    }
                }
            }
        }
    }

    private fun clickListeners() {
        binding.btnSendOTP.setOnClickListener(this)
        binding.btnResend.setOnClickListener(this)
        binding.btnVerify.setOnClickListener(this)
        binding.llPreOTP.setOnClickListener{
            this.hideKeyboard()
        }
        binding.llPostOTP.setOnClickListener{
            this.hideKeyboard()
        }
        binding.tvTerms.setOnClickListener {
            lifecycleScope.launch {
                delay(200)
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.data = Uri.parse("https://rama-2402.github.io/privacy-policy/")
                    startActivity(Intent.createChooser(intent, "Open link with"))
                } catch (e: Exception) {
                    println("The current phone does not have a browser installed")
                }
            }
        }
    }

    //validating the phone number before seding the OTP request
    private fun phoneNumberValidation() {
        val phNumber = binding.etPhoneNumberPreOTP.text.toString().trim()
        this.hideKeyboard()

        if(phNumber.isEmpty() || phNumber.length < 10) {
            showErrorSnackBar("Enter a valid phone number", true)
            return
        } else {
            val prefix = "+91"
            mPhoneNumber = "$prefix$phNumber"

            layoutVisibility("post")
            startTimer()
            startPhoneNumberVerification(mPhoneNumber)
        }
    }

    private fun startTimer() {
        val builderWithTimer = CircularView.OptionsBuilder()
            .shouldDisplayText(true)
            .setCounterInSeconds(60)
            .setCircularViewCallback(object : CircularViewCallback {
                override fun onTimerFinish() {
                    // Will be called if times up of countdown timer
                    onOtpTimeOut()
                }

                override fun onTimerCancelled() {

                }
            })
        binding.cvTimer.setOptions(builderWithTimer)
        binding.cvTimer.startTimer()
    }

    private fun stopTimer() = binding.cvTimer.stopTimer()

    //setting the otp timeout and sending the OTP
    private fun startPhoneNumberVerification(phone: String) = lifecycleScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.Main){
            showToast(this@SignInActivity,"Please wait for Auto-Verification", LONG)
        }
        try {
            val mFirebaseAuth = FirebaseAuth.getInstance()
            val options = PhoneAuthOptions.newBuilder(mFirebaseAuth)
                .setPhoneNumber(phone)
                .setTimeout(120L, TimeUnit.NANOSECONDS)
                .setActivity(this@SignInActivity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                        try {
                            showProgressDialog()
                            viewModel.signIn(phoneAuthCredential)
                        } catch (e: Exception) {
                            showErrorSnackBar("Server error! Please Try Later", true)
                        }
                    }

                    override fun onVerificationFailed(error: FirebaseException) {
//                        showErrorSnackBar("Server error! Please Try Later", true)
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        mVerificationId = verificationId
                        forceResendingToken = token
                        layoutVisibility("post")
                        showToast(this@SignInActivity, "OTP Sent", LONG)
                    }
                })
                .build()

            withContext(Dispatchers.Main) {
                this@SignInActivity.hideKeyboard()
            }
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            // Failed
            showErrorSnackBar("Server error! Please Try Later", true)
        }
    }


    private fun resendVerificationCode() {
        binding.btnResend.remove()
//        binding.btnResend.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.green_light, null))
//        binding.btnResend.setTextColor(resources.getColor(R.color.green_base))
        startTimer()
        startPhoneNumberVerification(mPhoneNumber)
    }

    //validating otp to verify
    private fun otpValidation() {
        val otp = binding.etPhoneNumberPostOTP.text.toString()
        if (otp.isEmpty()) {
            this.hideKeyboard()
            showErrorSnackBar("Enter a valid OTP", true)
        } else {
            this.hideKeyboard()
            showProgressDialog()
            verifyPhoneNumberWithCode(mVerificationId, otp)
        }
    }

    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId.toString(), code)
        viewModel.signIn(credential)
    }

    private suspend fun newUserVerification() {
        binding.cvTimer.fadOutAnimation()
        when (val status = viewModel.checkUserProfileDetails()) {
            "" -> {
                mCurrentUserID = viewModel.getCurrentUserId()!!
                SharedPref(this).putData(USER_ID, STRING, mCurrentUserID)
                SharedPref(this).putData(PHONE_NUMBER, STRING, mPhoneNumber)
                viewModel.createNewCustomerProfile()
                startGetProfileDataService()
                startGetAllDataService("profile")
            }
            "failed" -> {
                hideProgressDialog()
                showErrorSnackBar("Server Error! Failed to connect to server. Try again Later", true)
            }
            else -> {
                showToast(this, "Syncing your profile... Please wait... ")
                mCurrentUserID = status
                SharedPref(this).putData(USER_ID, STRING, mCurrentUserID)
                SharedPref(this).putData(PHONE_NUMBER, STRING, mPhoneNumber)
                startGetProfileDataService()
                startGetAllDataService("home")
            }
        }
    }

    private fun navigateToProfilePage() {
        Intent(this@SignInActivity, ProfileActivity::class.java).also {
            it.putExtra(PHONE_NUMBER, mPhoneNumber)
            it.putExtra(USER_ID, mCurrentUserID)
//            it.putExtra(STATUS, true)
            startActivity(it)
            finish()
        }
    }

    private fun navigateToHomePage() {
        SharedPref(this).putData(LOGIN_STATUS, BOOLEAN, false)
        Intent(this, HomeActivity::class.java).also {
            startActivity(it)
        }
    }

    private fun startGetProfileDataService() {
        val currentMonthYear = "${TimeUtil().getMonth()}${TimeUtil().getYear()}"
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<GetOrderHistoryService>()
                .setInputData(
                    workDataOf(
                        "id" to mCurrentUserID,
                        "filter" to currentMonthYear
                    )
                )
                .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun startGetAllDataService(navigateTo: String) {
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateDataService>()
                .setInputData(
                    workDataOf(
                        "wipe" to "",
                        "id" to mCurrentUserID
                    )
                )
                .build()

        WorkManager.getInstance(this).enqueue(workRequest)
        WorkManager.getInstance(this)
            .getWorkInfoByIdLiveData(workRequest.id)
            .observe(this) {
                when (it.state) {
                    WorkInfo.State.CANCELLED -> {
                        hideProgressDialog()
                        showToast(
                            this,
                            "Failed to update product catalog. Clear app data and try again",
                            LONG
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        hideProgressDialog()
                        showToast(
                            this,
                            "Failed to update product catalog. Clear app data and try again",
                            LONG
                        )
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        hideProgressDialog()
                        if (navigateTo == "home") {
                            navigateToHomePage()
                        } else {
                            navigateToProfilePage()
                        }
                    }
                    else -> Unit
                }
            }
    }


    private fun layoutVisibility(visibility: String) {
        with(binding) {
            when (visibility) {
                "pre" -> {
                    tvHeader.setTextAnimation("ENTER YOUR MOBILE NUMBER")
                    llPreOTP.visible()
                    llPostOTP.remove()
                    cvTimer.hide()
                    ivLogo.disable()
                }
                "post" -> {
                    tvHeader.setTextAnimation("ENTER OTP")
                    llPreOTP.remove()
                    llPostOTP.visible()
                    cvTimer.visible()
                    btnResend.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.green_light, null))
                    btnResend.setTextColor(resources.getColor(R.color.green_base, null))
                    btnResend.disable()
                    ivLogo.disable()
                }
            }
        }
    }

    //Called by countdown timer class on OTP Timeout after 60 seconds
    fun onOtpTimeOut() {
        binding.btnResend.enable()
        binding.btnResend.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.green_base, null))
        binding.btnResend.setTextColor(Color.WHITE)
        binding.cvTimer.hide()
    }

    //checking the network connection before proceeding
    private fun isOnline(): Boolean {
        return if (NetworkHelper.isNetworkConnected(baseContext)) {
            true
        } else {
            showErrorSnackBar("Please check network connection", true)
            false
        }
    }

    override fun onClick(v: View?) {
        if (v != null && isOnline()) {
            when (v) {
                binding.btnSendOTP -> phoneNumberValidation()
                binding.btnResend -> resendVerificationCode()
                binding.btnVerify -> otpValidation()
            }
        }
    }
}