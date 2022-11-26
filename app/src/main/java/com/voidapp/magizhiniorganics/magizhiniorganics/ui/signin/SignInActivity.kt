package com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivitySignInBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.GetOrderHistoryService
import com.voidapp.magizhiniorganics.magizhiniorganics.services.PortPhoneAuthToGmailAuth
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateDataService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.checkout.InvoiceActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile.ProfileActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BOOLEAN
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LOGIN_STATUS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.MAIL_ID
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PHONE_NUMBER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_ID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.android.retainedSubKodein
import org.kodein.di.generic.instance
import java.util.concurrent.TimeUnit

class SignInActivity : BaseActivity(), KodeinAware {

    override val kodein by kodein()

    private lateinit var binding: ActivitySignInBinding
    private lateinit var viewModel: SignInViewModel
    private val factory: SignInViewModelFactory by instance()

    private lateinit var auth: FirebaseAuth
    private lateinit var signInClient: GoogleSignInClient

    private var mMailID: String = ""
    private var mPhoneNumber: String = ""
    private var mCurrentUserID: String = ""

    private val SIGNIN_REQ = 1
    private var showOneTapUI = true
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    private var signInOption: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_in)
        viewModel = ViewModelProvider(this, factory)[SignInViewModel::class.java]

        signInOption = intent.getStringExtra("goto")

        lifecycleScope.launch {
            binding.apply {
                delay(400)
                ivLogo.fadInAnimation()
                clBody.startAnimation(
                    AnimationUtils.loadAnimation(
                        this@SignInActivity,
                        R.anim.slide_up
                    )
                )
                ivLogo.visible()
                clBody.visible()
                delay(100)
                ivBagOne.startAnimation(
                    AnimationUtils.loadAnimation(
                        this@SignInActivity,
                        R.anim.slide_in_left
                    )
                )
                ivBagOne.visible()
                delay(64)
                ivBagThree.startAnimation(
                    AnimationUtils.loadAnimation(
                        this@SignInActivity,
                        R.anim.slide_in_right
                    )
                )
                ivBagThree.visible()
                delay(90)
                ivBagTwo.startAnimation(
                    AnimationUtils.loadAnimation(
                        this@SignInActivity,
                        R.anim.slide_in_left
                    )
                )
                ivBagTwo.visible()
                delay(34)
                ivBagFour.startAnimation(
                    AnimationUtils.loadAnimation(
                        this@SignInActivity,
                        R.anim.slide_in_right
                    )
                )
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

        auth = FirebaseAuth.getInstance()

        val signInRequest = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(resources.getString(R.string.google_token_id))
            .requestEmail()
            .build()

        signInClient = GoogleSignIn.getClient(this, signInRequest)

//        googleSignInVerification()
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
                when (status) {
                    "" -> Unit
                    "new" -> newUserVerification()
                    "old" -> newUserVerification()
                    "mismatch" -> {
                        hideProgressDialog()
                        this@SignInActivity.hideKeyboard()
                        showErrorSnackBar("This Login ID is associated with another Number", true)
                        auth.signOut()
                        GoogleSignIn.getClient(
                            this@SignInActivity,
                            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        )
                            .signOut()
                    }
                    "port" -> {
                        val workRequest: WorkRequest =
                            OneTimeWorkRequestBuilder<PortPhoneAuthToGmailAuth>()
                                .setInputData(
                                    workDataOf(
                                        "userID" to viewModel.getCurrentUserId()!!,
                                        "phoneID" to viewModel.phoneAuthID
                                    )
                                )
                                .build()

                        WorkManager.getInstance(this@SignInActivity).enqueue(workRequest)

                        WorkManager.getInstance(this@SignInActivity)
                            .getWorkInfoByIdLiveData(workRequest.id)
                            .observe(this@SignInActivity) {
                                when (it.state) {
                                    WorkInfo.State.SUCCEEDED -> {
                                        lifecycleScope.launch {
                                            newUserVerification()
                                        }
                                    }
                                    else -> {}
                                }
                            }
                    }
                    "imageFailed" -> {
                        hideProgressDialog()
                        showErrorSnackBar(
                            "Server Error! Profile Picture upload Failed. Try again later",
                            true
                        )
                    }
                    "profileFailed" -> {
                        hideProgressDialog()
                        showErrorSnackBar(
                            "Server Error! Profile creation failed. Try again Later",
                            true
                        )
                    }
                    "success" -> {
                        hideProgressDialog()
//                        stopTimer()
                        showErrorSnackBar("Profile Created Successfully", false)
                        navigateToHomePage()
                    }
                    else -> {
                        newUserVerification()
                    }
                }
                viewModel.setEmptyStatus()
            }
        }
    }

    private fun clickListeners() {
        binding.llPreOTP.setOnClickListener {
            this.hideKeyboard()
        }
        binding.tvTerms.setOnClickListener {
            lifecycleScope.launch {
                delay(200)
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.addCategory(Intent.CATEGORY_BROWSABLE)
                    intent.data = Uri.parse("https://rama-2402.github.io/privacy-policy/")
                    startActivity(Intent.createChooser(intent, "Open link with"))
                } catch (e: Exception) {
                    println("The current phone does not have a browser installed")
                }
            }
        }
        binding.btnGoogleLogIn.setOnClickListener {
            if (binding.etPhoneNumber.text.toString().isNullOrEmpty()) {
                showToast(this, "Enter Phone Number to continue")
                return@setOnClickListener
            }
            if (isOnline()) {
                mPhoneNumber = "+91${binding.etPhoneNumber.text.toString().trim()}"

//        oneTapClient.beginSignIn(signInRequest)
//            .addOnSuccessListener(this) { result ->
//                try {
//                    startIntentSenderForResult(
//                        result.pendingIntent.intentSender, SIGNIN_REQ, null, 0,0,0,null
//                    )
//                } catch (e: Exception) {
//
//                }
//            }

                val signInIntent = signInClient.signInIntent
                startActivityForResult(signInIntent, 100)
//                activityForResult.launch(signInIntent)
            }
        }
        binding.btnSkip.setOnClickListener {
           if (signInOption == "order") {
                showToast(this, "Skipping SignIn")
                navigateToCheckout()
           }
            when (signInOption) {
                "newID" -> finish()
                "wallet" -> finish()
                "food" -> finish()
                else -> {
                    showProgressDialog(true)
                    showToast(this, "Getting latest offers and syncing store...")
                    startGetAllDataService("home")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { firebaseAuthWithGoogle(it) }
            } catch (e: Exception) {
                showToast(this, e.message.toString())
            }
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        when (requestCode) {
//            SIGNIN_REQ -> {
//                try {
//                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
//                    val idToken = credential.googleIdToken
//                    val username = credential.id
//                    val password = credential.password
//                    when {
//                        idToken != null -> {
//                            firebaseAuthWithGoogle(idToken)
//                            // Got an ID token from Google. Use it to authenticate
//                            // with your backend.
//                            Log.d(TAG, "Got ID token.")
//                        }
//                        password != null -> {
//                            // Got a saved username and password. Use them to authenticate
//                            // with your backend.
//                            showToast(this, "non null password")
//                            Log.d(TAG, "Got password.")
//                        }
//                        else -> {
//                            // Shouldn't happen.
//                            showToast(this, "No ID token or password!")
//                            Log.d(TAG, "No ID token or password!")
//                        }
//                    }
//                } catch (e: ApiException) {
//                    showToast(this, e.message.toString())
//                    // ...
//                }
//            }
//            else ->  {
//                showToast(this, "req code else block")
//            }
//        }
//        }

    private fun googleSignInVerification() {
        auth = FirebaseAuth.getInstance()
//        oneTapClient = Identity.getSignInClient(this)
//        signInRequest = BeginSignInRequest.builder()
////            .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
////                .setSupported(true)
////                .build())
//            .setGoogleIdTokenRequestOptions(
//                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
//                    .setSupported(true)
//                    .setServerClientId(resources.getString(R.string.google_token_id))
//                    .setFilterByAuthorizedAccounts(true)
//                    .build()
//            ).setAutoSelectEnabled(true).build()

        val signInRequest = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(resources.getString(R.string.google_token_id))
            .requestEmail()
            .build()

        signInClient = GoogleSignIn.getClient(this, signInRequest)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    mMailID = task.result.user?.email ?: ""
                    mCurrentUserID = task.result.user?.uid ?: ""
                    showProgressDialog(false)
                    viewModel.checkForPreviousProfiles(
                        "+91${
                            binding.etPhoneNumber.text.toString().trim()
                        }"
                    )
                } else {
                    Log.e(TAG, "firebaseAuthWithGoogle: ${task.exception}")
                }
            }
    }

    private val activityForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
//                try {
//                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
//                    val idToken = credential.googleIdToken
//                    when {
//                        idToken != null -> {
//                            firebaseAuthWithGoogle(idToken)
//                            // Got an ID token from Google. Use it to authenticate
//                            // with Firebase.
//                            Log.d(TAG, "Got ID token.")
//                        }
//                        else -> {
//                            // Shouldn't happen.
//                            Log.d(TAG, "No ID token!")
//                        }
//                    }
//                } catch (e: ApiException) {
//                    // ...
//                }

                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    account.idToken?.let { firebaseAuthWithGoogle(it) }
                    Log.e("qw", "token ${account.idToken}")
                } catch (e: Exception) {
                    Log.e("qw", "activity result${e.message}: ")
                }
            }
        }

    private suspend fun newUserVerification() {
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
                showErrorSnackBar(
                    "Server Error! Failed to connect to server. Try again Later",
                    true
                )
            }
            else -> {
                showToast(this, "Syncing your profile... Please wait... ")
                mCurrentUserID = viewModel.getCurrentUserId()!!
                SharedPref(this).putData(USER_ID, STRING, mCurrentUserID)
                SharedPref(this).putData(PHONE_NUMBER, STRING, mPhoneNumber)
                startGetProfileDataService()
                when(signInOption) {
                    "wallet" -> finish()
                    "newID" -> finish()
                    "food" -> finish()
                    else -> startGetAllDataService("home")
                }
            }
        }
    }

    private fun navigateToProfilePage(navigateTo: String?) {
        hideProgressDialog()
        Intent(this@SignInActivity, ProfileActivity::class.java).also {
            it.putExtra(PHONE_NUMBER, mPhoneNumber)
            it.putExtra(USER_ID, mCurrentUserID)
            it.putExtra(MAIL_ID, mMailID)
            it.putExtra("goto", navigateTo)
            startActivity(it)
            finish()
        }
    }

    private fun navigateToHomePage() {
        hideProgressDialog()
        if (signInOption == "order") {
            navigateToCheckout()
        } else {
            SharedPref(this).putData(LOGIN_STATUS, BOOLEAN, false)
            Intent(this, HomeActivity::class.java).also {
                startActivity(it)
                finish()
            }
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

                        val periodicWorkRequest: WorkRequest =
                    PeriodicWorkRequestBuilder<UpdateDataService>(8, TimeUnit.HOURS)
                            .setInitialDelay(TimeUtil().getHoursBeforeMidNight(), TimeUnit.HOURS)
                            .setInputData(
                                workDataOf(
                                    "wipe" to "",
                                    "id" to mCurrentUserID
                                )
                            )
                            .build()
                        WorkManager.getInstance(this).enqueue(periodicWorkRequest)

                        when(navigateTo) {
                            "home" -> navigateToHomePage()
                            "profile" -> navigateToProfilePage("order")
                            else -> navigateToCheckout()
                        }
                    }
                    WorkInfo.State.BLOCKED -> Log.e("qw", "blocked")
                    WorkInfo.State.ENQUEUED -> Log.e("qw", "enqueue")
                    WorkInfo.State.RUNNING -> Log.e("qw", "running")
                    else -> Unit
                }
            }
    }

    private fun navigateToCheckout() {
        Intent(this, InvoiceActivity::class.java).also {
            startActivity(it)
            finish()
        }
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
}