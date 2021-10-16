package com.voidapp.magizhiniorganics.magizhiniorganics.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySignInBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.CountdownTimer
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.NetworkHelper
import kotlinx.coroutines.*
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.concurrent.TimeUnit

class SignInActivity : BaseActivity(), View.OnClickListener, KodeinAware {

    lateinit var binding: ActivitySignInBinding

    override val kodein by kodein()
    val repository: FirestoreRepository by instance()

    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private var mCallBacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null
    private var mVerificationId: String? = null
    private lateinit var mFirebaseAuth: FirebaseAuth

    private var mPhoneNumber: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)

        // Verifying the mobile number and sending the OTP
        mCallBacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
               repository.signInWithPhoneAuthCredential(
                    this@SignInActivity,
                    phoneAuthCredential)
//                _status.value = repository.signIn(phoneAuthCredential) as Boolean

            }

            override fun onVerificationFailed(error: FirebaseException) {
                onFirestoreFailure("Something went wrong. Try Later")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                mVerificationId = verificationId
                forceResendingToken = token

                binding.llPreOTP.gone()
                binding.llPostOTP.show()
                showToast(this@SignInActivity, "OTP Sent", Constants.LONG)
            }
        }

        viewInit()

    }

    //Initializing
    private fun viewInit() {
        binding.llPostOTP.gone()
        binding.llPreOTP.show()
        if(isOnline()) {
            mFirebaseAuth = FirebaseAuth.getInstance()
            clickListeners()
        } else {
            binding.btnSendOTP.disable()
        }
    }

    private fun clickListeners() {
        binding.btnSendOTP.setOnClickListener(this)
        binding.btnResend.setOnClickListener(this)
        binding.btnVerify.setOnClickListener(this)
        binding.llPreOTP.setOnClickListener(this)
        binding.llPostOTP.setOnClickListener(this)
    }

    //validating the phone number before seding the OTP request
    private fun phoneNumberValidation() {
        val phNumber = binding.etPhoneNumberPreOTP.text.toString().trim()

        if(phNumber.isEmpty() || phNumber.length < 10) {
            showErrorSnackBar("Enter a valid phone number", true)
            return
        } else {

            UIUtil.hideKeyboard(this)

            val prefix = "+91"
            mPhoneNumber = "$prefix$phNumber"

            binding.llPreOTP.gone()
            binding.llPostOTP.show()
            binding.cvTimer.show()
            binding.btnResend.disable()
            binding.btnResend.setBackgroundColor(Color.LTGRAY)

            CountdownTimer().initTimer(this)
            binding.cvTimer.startTimer()

            startPhoneNumberVerification(mPhoneNumber)
        }
    }

    //setting the otp timeout and sending the OTP
    private fun startPhoneNumberVerification(phone: String) = CoroutineScope(Dispatchers.IO).launch {

        withContext(Dispatchers.Main){
            Toast.makeText(this@SignInActivity, "Please wait for Auto-Verification", Toast.LENGTH_LONG).show()
        }

        try {
            val options = PhoneAuthOptions.newBuilder(mFirebaseAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.NANOSECONDS)
                .setActivity(this@SignInActivity)
                .setCallbacks(mCallBacks!!)
                .build()

            withContext(Dispatchers.Main) {
                UIUtil.hideKeyboard(this@SignInActivity)
            }
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            // Failed
            onFirestoreFailure("Something went wrong! Try Later")
        }

    }

    private fun resendVerificationCode(
        phone: String,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        binding.btnResend.disable()
        binding.btnResend.setBackgroundColor(Color.LTGRAY)

        binding.cvTimer.show()
        CountdownTimer().initTimer(this)
        binding.cvTimer.startTimer()

        startPhoneNumberVerification(mPhoneNumber)
    }

    //validating otp to verify
    private fun otpValidation() {

        val otp = binding.etPhoneNumberPostOTP.text.toString()

        if (otp.isEmpty()) {
            showErrorSnackBar("Enter a valid OTP", true)
        } else {
            UIUtil.hideKeyboard(this)
            verifyPhoneNumberWithCode(mVerificationId, otp)
        }

    }

    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {

        val credential = PhoneAuthProvider.getCredential(verificationId.toString(), code)
        repository.signInWithPhoneAuthCredential(this ,credential)
//        _status.value = repository.signIn(credential) as Boolean

    }

    fun loggedIn() {
        binding.cvTimer.stopTimer()
        Intent(this, ProfileActivity::class.java).also {
            it.putExtra(Constants.PHONE_NUMBER, mFirebaseAuth.currentUser!!.phoneNumber)
            it.putExtra(Constants.USER_ID, mFirebaseAuth.currentUser!!.uid)
            it.putExtra(Constants.STATUS, "onBoard")
            startActivity(it)
            finish()
        }
    }

    //called by firestore when signin authentication is failed
    fun onFirestoreFailure(error: String) {
        showErrorSnackBar(error, true)
    }

    //Called by countdown timer class on OTP Timeout after 60 seconds
    fun onOtpTimeOut() {
        binding.btnResend.enable()
        binding.btnResend.setBackgroundColor(Color.WHITE)
        binding.cvTimer.hide()
    }

    //checking the network connection before proceeding
    private fun isOnline():Boolean {
        return if (NetworkHelper.isNetworkConnected(baseContext)) {
            true
        } else {
            showErrorSnackBar("Please check network connection", true)
            false
        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v) {
                binding.btnSendOTP -> {
                    phoneNumberValidation()
                }
                binding.btnResend -> {
                    resendVerificationCode(mPhoneNumber, forceResendingToken)
                }
                binding.btnVerify -> {
                    otpValidation()
                }
                binding.llPreOTP -> UIUtil.hideKeyboard(this)
                binding.llPostOTP -> UIUtil.hideKeyboard(this)
            }
        }
    }
}