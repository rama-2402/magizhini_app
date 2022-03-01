package com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Query
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityProfileBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddReferralBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CUSTOMER_SUPPORT
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import kotlinx.coroutines.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.IOException
import java.util.*

class ProfileActivity : BaseActivity(), View.OnClickListener, KodeinAware {

    lateinit var binding: ActivityProfileBinding
    override val kodein by kodein()

    private lateinit var viewModel: ProfileViewModel
    private val factory: ProfileViewModelFactory by instance()

    private var mProfile: UserProfile = UserProfile()

    private var mProfilePicUri: Uri? = null
    private var mProfilePicUrl: String = ""

    private var mCurrentUserId: String? = ""
    private var mPhoneNumber: String = ""
    private var isNewUser: Boolean = false

    private var mLatitude: String = ""
    private var mLongitude: String = ""
    private var mAddress:  String = ""

    private lateinit var dialogBsAddReferral: BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isNewUser = intent.getBooleanExtra(Constants.STATUS, false)
        mCurrentUserId = intent.getStringExtra(Constants.USER_ID).toString()
        mPhoneNumber = intent.getStringExtra(Constants.PHONE_NUMBER).toString()

        //setting the theme and view binding
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        viewModel = ViewModelProvider(this, factory).get(ProfileViewModel::class.java)
        binding.viewmodel = viewModel

        with(binding) {
            ivHeader.startAnimation(AnimationUtils.loadAnimation(this@ProfileActivity, R.anim.slide_in_top_bounce))
            tvProfile.startAnimation(AnimationUtils.loadAnimation(this@ProfileActivity, R.anim.slide_in_top_bounce))
            llProfileName.startAnimation(AnimationUtils.loadAnimation(this@ProfileActivity, R.anim.slide_in_top_bounce))
            svProfileBody.startAnimation(AnimationUtils.loadAnimation(this@ProfileActivity, R.anim.slide_up))
        }

        lifecycleScope.launch {
            delay(1010)
            binding.ivProfilePic.visible()
        }

        activityInit()
        observers()
        clickListeners()

        if (savedInstanceState != null) {
            mProfilePicUri = savedInstanceState.getString(Constants.PROFILE_PIC_URI)?.toUri()
            mProfilePicUri?.let { GlideLoader().loadUserPicture(this, it, binding.ivProfilePic) }
        }
    }

    private fun observers() {
        viewModel.userProfile.observe(this) { userData ->
            mProfile = userData.toUserProfile()
            viewModel.getAllActiveOrders().observe(this) {
                mProfile.purchaseHistory.clear()
                mProfile.purchaseHistory.addAll(it)
            }
            viewModel.getAllActiveSubscriptions().observe(this) {
                mProfile.subscriptions.clear()
                mProfile.subscriptions.addAll(it)
            }
            setUserDetailsFromDao(userData)
        }
        viewModel.referralStatus.observe(this) {
            hideProgressDialog()
            if (it) {
                dialogBsAddReferral.dismiss()
                binding.tvReferral.remove()
                showErrorSnackBar(
                    "Referral added Successfully. Your referral bonus will be added to your Wallet.",
                    false,
                    LONG
                )
            } else {
                mProfile.referrerNumber = ""
                showToast(this, "No account with the given number. Please check again")
            }
        }
        viewModel.profileUploadStatus.observe(this) { status ->
            if (status) {
                hideProgressDialog()
                successDialogAndTransition()
            } else {
                hideProgressDialog()
                showErrorSnackBar("Server Error! Profile Creation failed. Try later", true)
            }
        }
        viewModel.profileImageUploadStatus.observe(this) { status ->
            if (status == "failed") {
                hideProgressDialog()
                showErrorSnackBar("Server Error! Image Update failed. Try later", true)
            } else {
                mProfile.profilePicUrl = status
                viewModel.uploadProfile(mProfile)
            }
        }
    }

    private fun activityInit() {
        binding.ivProfilePic.clipToOutline = true
        lifecycleScope.launch {
            if (isNewUser) {
                mProfile.id = mCurrentUserId.toString()
                binding.tvPhoneNumber.text = mPhoneNumber
                if (viewModel.checkForReferral(mCurrentUserId!!)) {
                    binding.tvReferral.remove()
                } else {
                    viewModel.createNewUserWallet(mCurrentUserId!!)
                }
            } else {
                viewModel.getUserProfile()
            }
        }
    }

    private fun clickListeners() {
        with(binding) {
            ivProfilePic.setOnClickListener(this@ProfileActivity)
            btnSaveProfile.setOnClickListener(this@ProfileActivity)
            llDob.setOnClickListener(this@ProfileActivity)
            tvReferral.setOnClickListener(this@ProfileActivity)
            llGps.setOnClickListener(this@ProfileActivity)
        }
    }

    //generating the profile model for uploading to firestore
    //and assigning it to mProfile variable for later use
    private fun generateProfileModel(): UserProfile {
        generateAddressObject()
        with(mProfile) {
            name = binding.etProfileName.text.toString().trim()
            phNumber = binding.tvPhoneNumber.text.toString().trim()
            alternatePhNumber = binding.etAlternateNumber.text.toString().trim()
            dob = binding.tvDob.text.toString()
            mailId = binding.etEmailId.text.toString().trim()
        }
        return mProfile
    }

    private fun generateAddressObject() {
        if (mProfile.address.isNotEmpty()) {
            with(mProfile.address[0]) {
                userId = binding.etProfileName.text.toString().trim()
                addressLineOne = binding.etAddressOne.text.toString().trim()
                addressLineTwo = binding.etAddressTwo.text.toString().trim()
                city = binding.spCity.selectedItem.toString()
                LocationCode = binding.spArea.selectedItem.toString()
                LocationCodePosition = binding.spArea.selectedItemPosition
                gpsLatitude = mLatitude
                gpsLongitude = mLongitude
                gpsAddress = mAddress
            }
        } else {
            Address (
                userId = binding.etProfileName.text.toString().trim(),
                addressLineOne = binding.etAddressOne.text.toString().trim(),
                addressLineTwo = binding.etAddressTwo.text.toString().trim(),
                city = binding.spCity.selectedItem.toString(),
                LocationCode = binding.spArea.selectedItem.toString(),
                LocationCodePosition = binding.spArea.selectedItemPosition,
                gpsLatitude = mLatitude,
                gpsLongitude = mLongitude,
                gpsAddress = mAddress
            ).also {
                mProfile.address.add(it)
            }
        }
    }

    //setting the user details that is received from DAO
    private fun setUserDetailsFromDao(userProfile: UserProfileEntity) {
        with(binding) {
            etProfileName.setText(userProfile.name)
            tvDob.text = userProfile.dob
            tvPhoneNumber.text = userProfile.phNumber
            etAlternateNumber.setText(userProfile.alternatePhNumber)
            etEmailId.setText(userProfile.mailId)
            etAddressOne.setText(userProfile.address[0].addressLineOne)
            etAddressTwo.setText(userProfile.address[0].addressLineTwo)
            spArea.setSelection(userProfile.address[0].LocationCodePosition)
            tvGps.text = userProfile.address[0].gpsAddress
            mProfilePicUrl = userProfile.profilePicUrl
            mLatitude = userProfile.address[0].gpsLatitude
            mLongitude = userProfile.address[0].gpsLongitude
            mAddress = userProfile.address[0].gpsAddress
            tvReferral.remove()
            //hinding the referral code area for existing user
        }
        GlideLoader().loadUserPicture(this, mProfilePicUrl, binding.ivProfilePic)
    }

    //validating the data entered before uploading
    private fun profileDataValidation() {
        if (binding.etAddressOne.text.isNullOrEmpty()) {
                binding.etlAddressOne.error = "* required"
                return
        }
        if (binding.etAddressTwo.text.isNullOrEmpty()) {
             binding.etlAddressTwo.error = "* required"
             return
        }
        generateProfileModel()
        //address lines, name, dob, are mandatory
        //if mProfilePicUri is null that means the user didnot select any pic from storage so
        //if it is for the first time url will be empty and that will be assigned to mProfile class
        //if there is already a picture but edited rest of the profile page then the already present pic's url will be taken back
        with(mProfile) {
            when {
                name.isEmpty() -> {
                    binding.etlProfileName.error = "* required"
                    return@with
                }
                mailId.isEmpty() -> {
                    binding.etlEmailId.error = "* required"
                    return@with
                }
                !Patterns.EMAIL_ADDRESS.matcher(mailId).matches() -> {
                    binding.etlEmailId.error = "* Enter a valid Email ID"
                    return@with
                }
                dob == " DD / MM / YYYY " -> {
                    binding.tvDob.error = "* required"
                    this@ProfileActivity.hideKeyboard()
                    showToast(this@ProfileActivity, "Please select Date of Birth")
                    return@with
                }
                mProfilePicUri == null -> {
                    mProfile.profilePicUrl = mProfilePicUrl
                    showProgressDialog()
                    //uploading only the changed data. since uri is empty profile pic is not changed
                    viewModel.uploadProfile(mProfile)
                }
                mProfilePicUri != null -> {
                    showProgressDialog()
                    //uploading the profile pic first and thereby then chaining to upload the rest of the data
                    viewModel.uploadProfilePic (
                        Constants.PROFILE_PIC_PATH,
                        mProfilePicUri!!,
                        GlideLoader().imageExtension(this@ProfileActivity,  mProfilePicUri!!)!!
                    )
                }
            }
        }
    }

    //assigning the data of birth date
    fun onDobSelected(date: Long) {
        binding.tvDob.text = TimeUtil().getCustomDate(dateLong = date)
        SharedPref(this).putData(Constants.DOB, Constants.STRING, TimeUtil().getCustomDate(dateLong = date).substring(0,5))
        mProfile.dob = date.toString()
    }

    fun exitProfileWithoutChange() {
        //on back pressed if it is new user the app is closed if not it means the user is already logged in so it will move to home activity
        if(isNewUser) {
            finish()
        } else {
            transitionFromProfile()
        }
    }

    private fun successDialogAndTransition() {
        //on save profile btn press if new user the success dialog will be shown else profile updated text will be shown with sucess dialog
        if (isNewUser) {
            SharedPref(this).putData(Constants.USER_ID, Constants.STRING, mCurrentUserId!!)
            SharedPref(this).putData(Constants.PHONE_NUMBER, Constants.STRING, mPhoneNumber)
            SharedPref(this).putData(Constants.LOGIN_STATUS, Constants.BOOLEAN, false)   //stating it is not new user
            showSuccessDialog()
        } else {
            showSuccessDialog("Profile Updated", "" )
        }

        //a delay of 2 seconds for the success dialog to fully display the animation
        lifecycleScope.launch {
            delay(2000)
            hideSuccessDialog()
            newUserTransitionFromProfile()
        }

    }

    private fun transitionFromProfile() {
        //movement of profile to home activity for old user
            Intent(this, HomeActivity::class.java).also {
                startActivity(it)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                finish()
            }
    }

    private fun newUserTransitionFromProfile() {
        Intent(this, HomeActivity::class.java).also {
            startActivity(it)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }

    private fun showReferralBs() {
        //BS to add referral number
        dialogBsAddReferral = BottomSheetDialog(this, R.style.BottomSheetDialog)

        val view: DialogBottomAddReferralBinding = DataBindingUtil.inflate(LayoutInflater.from(applicationContext),R.layout.dialog_bottom_add_referral,null,false)
        dialogBsAddReferral.setCancelable(true)
        dialogBsAddReferral.setContentView(view.root)
        dialogBsAddReferral.dismissWithAnimation = true

        //verifying if the referral number is empty and assigning it to the userProfile object
        view.btnApply.setOnClickListener {
            val code = view.etReferralNumber.text.toString().trim()
            if (code.isEmpty()) {
                view.etlReferralNumber.error = "* Enter a valid code"
            return@setOnClickListener
            } else {
                showProgressDialog()
                mProfile.referrerNumber = code
                viewModel.applyReferralNumber(mCurrentUserId!!, code)
            }
        }

        dialogBsAddReferral.show()
    }

    override fun onBackPressed() {
        if (isNewUser) {
            showExitSheet(this, "Cancel Registration")
        } else {
            showExitSheet(this, "Exit Profile Update")
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putString(Constants.PROFILE_PIC_URI, mProfilePicUri.toString())
        super.onSaveInstanceState(outState, outPersistentState)
    }

    fun proceedToRequestPermission() = PermissionsUtil.requestStoragePermissions(this)

    fun proceedToRequestManualPermission() = this.openAppSettingsIntent()

    private val getAction = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        mProfilePicUri = result.data?.data
        mProfilePicUri?.let { uri -> GlideLoader().loadUserPicture(this, uri, binding.ivProfilePic) }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.STORAGE_PERMISSION_CODE) {
            if(
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                showToast(this, "Storage Permission Granted")
                getAction.launch(pickImageIntent)
            } else {
                showToast(this, "Storage Permission Denied")
                showExitSheet(this, "Some or All of the Storage Permission Denied. Please click PROCEED to go to App settings to Allow Permission Manually \n\n PROCEED >> [Settings] >> [Permission] >> Permission Name Containing [Storage or Media or Photos]", "setting")
            }
        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v) {
                binding.ivProfilePic -> {
                    if (PermissionsUtil.hasStoragePermission(this)) {
                        getAction.launch(pickImageIntent)
                    } else {
                        showExitSheet(this, "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
                    }
                }
                binding.btnSaveProfile -> {
                    profileDataValidation()
//                    viewModel.deleteprof()
                }
                binding.llDob -> {
                    DatePickerLib().pickDob(this)
                }
                binding.tvReferral -> {
                    showReferralBs()
                }
                binding.llGps -> {
//                    if(!PermissionsUtil().isGpsEnabled(this)) {
//                        startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
//                    } else {
//                        generateProfileModel()
//                        Intent(this, MapsActivity::class.java).also {
//                            startActivity(it)
//                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
//                        }
//                    }
                }
            }
        }
    }
}