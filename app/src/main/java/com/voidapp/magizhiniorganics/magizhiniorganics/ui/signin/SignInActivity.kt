package com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySignInBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.GetOrderHistoryService
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateDataService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PHONE_NUMBER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_ID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import ticker.views.com.ticker.widgets.circular.timer.callbacks.CircularViewCallback
import ticker.views.com.ticker.widgets.circular.timer.view.CircularView
import java.util.concurrent.TimeUnit

class SignInActivity : BaseActivity(), View.OnClickListener, KodeinAware {

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


        layoutVisibility("pre")

        // Verifying the mobile number and sending the OTP
        mCallBacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                showProgressDialog()
                viewModel.signIn(phoneAuthCredential)
            }

            override fun onVerificationFailed(error: FirebaseException) {
                showErrorSnackBar("Server error! Please Try Later", true)
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
        }

        initFlow()
        clickListeners()
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
                        showErrorSnackBar("Profile Created Successfully", false)
                        navigateToHomePage()
                    }
                    else -> newUserVerification()
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
                .setCallbacks(mCallBacks!!)
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
        binding.btnResend.disable()
        binding.btnResend.setBackgroundColor(Color.LTGRAY)
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
        stopTimer()
        viewModel.signIn(credential)
    }

    private suspend fun newUserVerification() {
        when (val status = viewModel.checkUserProfileDetails()) {
            "" -> {
                mCurrentUserID = viewModel.getCurrentUserId()!!
                viewModel.createNewCustomerProfile()
                startGetProfileDataService()
                startGetAllDataService()
                delay(2000)
                hideProgressDialog()
                navigateToProfilePage()
            }
            "failed" -> {
                hideProgressDialog()
                showErrorSnackBar("Server Error! Failed to connect to server. Try again Later", true)
            }
            else -> {
                mCurrentUserID = status
                SharedPref(this).putData(USER_ID, STRING, mCurrentUserID)
                SharedPref(this).putData(PHONE_NUMBER, STRING, mPhoneNumber)
                startGetProfileDataService()
                startGetAllDataService()
                delay(2000)
                hideProgressDialog()
                navigateToHomePage()
            }
        }
    }

    private fun navigateToProfilePage() {
        Intent(this@SignInActivity, ProfileActivity::class.java).also {
            it.putExtra(Constants.PHONE_NUMBER, mPhoneNumber)
            it.putExtra(Constants.USER_ID, mCurrentUserID)
            it.putExtra(Constants.STATUS, true)
            startActivity(it)
            finish()
        }
    }

    private fun navigateToHomePage() {
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

    private fun startGetAllDataService() {
        val workRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateDataService>()
                .setInputData(
                    workDataOf(
                        "wipe" to ""
                    )
                )
                .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }


    private fun layoutVisibility(visibility: String) {
        with(binding) {
            when (visibility) {
                "pre" -> {
                    llPreOTP.visible()
                    llPostOTP.remove()
                    cvTimer.hide()
                    ivLogo.disable()
                }
                "post" -> {
                    llPreOTP.remove()
                    llPostOTP.visible()
                    cvTimer.visible()
                    btnResend.setBackgroundColor(Color.LTGRAY)
                    btnResend.disable()
                    ivLogo.disable()
                }
            }
        }
    }

    //Called by countdown timer class on OTP Timeout after 60 seconds
    fun onOtpTimeOut() {
        binding.btnResend.enable()
        binding.btnResend.setBackgroundColor(Color.WHITE)
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