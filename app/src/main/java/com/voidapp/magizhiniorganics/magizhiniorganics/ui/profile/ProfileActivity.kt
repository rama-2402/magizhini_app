package com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.google.android.gms.dynamic.IFragmentWrapper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityProfileBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddReferralBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.services.GetOrderHistoryService
import com.voidapp.magizhiniorganics.magizhiniorganics.services.UpdateDataService
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import kotlinx.coroutines.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.IOException
import java.util.*

class ProfileActivity : BaseActivity(), View.OnClickListener, KodeinAware {

    //TODO UPLOAD FIRESTORE AND DAO FAVORITES, DEFAULT VARIANTS DETAILS IN PROFILE

    lateinit var binding: ActivityProfileBinding
    override val kodein by kodein()

    private lateinit var viewModel: ProfileViewModel
    private val factory: ProfileViewModelFactory by instance()
//
//    private val repository: FirestoreRepository by instance()
//    private val databaseRepository: DatabaseRepository by instance()
//    private val firebaseRepository: FirebaseRepository by instance()

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

        mCurrentUserId = SharedPref(this).getData(Constants.USER_ID, Constants.STRING, "") as String

        mPhoneNumber = intent.getStringExtra(Constants.PHONE_NUMBER).toString()

        isNewUser = intent.getBooleanExtra(Constants.STATUS, false)

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
            binding.ivProfilePic.show()
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
        viewModel.userProfile.observe(this, { userData ->
            mProfile = userData.toUserProfile()
            setUserDetailsFromDao(userData)
        })
        viewModel.profileUploadStatus.observe(this, { status ->
            if (status) {
                hideProgressDialog()
                successDialogAndTransition()
            } else {
                hideProgressDialog()
                showErrorSnackBar("Server Error! Profile Creation failed. Try later", true)
            }
        })
        viewModel.profileImageUploadStatus.observe(this, { status ->
            if (status == "failed") {
                hideProgressDialog()
                showErrorSnackBar("Server Error! Image Update failed. Try later", true)
            } else {
                mProfile.profilePicUrl = status
                viewModel.uploadProfile(mProfile)
            }
        })
        if (!isNewUser){
            viewModel.getAllActiveOrders().observe(this, {
                mProfile.purchaseHistory.clear()
                mProfile.purchaseHistory.addAll(it)
            })
            viewModel.getAllActiveSubscriptions().observe(this, {
                mProfile.subscriptions.clear()
                mProfile.subscriptions.addAll(it)
            })
        }
    }

    private fun activityInit() {
        binding.ivProfilePic.clipToOutline = true
        binding.tvPhoneNumber.text = mPhoneNumber
        if (!isNewUser) {
            viewModel.getUserProfile()
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
        if (isNewUser) {
            viewModel.createNewUserWallet(mCurrentUserId!!)
        }
        generateAddressObject()
        with(mProfile) {
            id = mCurrentUserId!!
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
            tvReferral.gone()
            //hinding the referral code area for existing user
        }
        GlideLoader().loadUserPicture(this, mProfilePicUrl, binding.ivProfilePic)
    }

    //validating the data entered before uploading
    private fun profileDataValidation() {
        generateProfileModel()
        if (binding.etAddressOne.text.isNullOrEmpty()) {
                binding.etlAddressOne.error = "* required"
                return
        }
        if (binding.etAddressTwo.text.isNullOrEmpty()) {
             binding.etlAddressTwo.error = "* required"
             return
        }

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
                dob == " DD / MM / YYYY " -> {
                    binding.tvDob.error = "* required"
                    return@with
                }
                mProfilePicUri == null -> {
                    mProfile.profilePicUrl = mProfilePicUrl
                    showProgressDialog()
                    //uploading only the changed data. since uri is empty profile pic is not changed
                    viewModel.uploadProfile(mProfile)
                }
                mProfilePicUri !== null -> {
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
    fun onDobSelected(date: String) {
        binding.tvDob.text = date
        mProfile.dob = date
    }

    fun exitProfileWithoutChange() {
        hideExitSheet()
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
            showSuccessDialog()
        } else {
            showSuccessDialog("Profile Updated", "" )
        }

        //a delay of 2 seconds for the success dialog to fully display the animation
        lifecycleScope.launch {
            delay(2000)
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
        SharedPref(this).putData(Constants.LOGIN_STATUS, Constants.BOOLEAN, false)   //stating it is not new user
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
                    mProfile.referrerNumber = code
                    dialogBsAddReferral.dismiss()
                    showToast(this@ProfileActivity, "Referral added", Constants.SHORT)
                }
            }

        dialogBsAddReferral.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            //If permission is granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                GlideLoader().showImageChooser(this)
            } else {
                //Displaying another toast if permission is not granted
                showErrorSnackBar("Storage Permission Denied!", true)
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.PICK_IMAGE_REQUEST_CODE) {
                if (data != null) {
                    try {
                        // The uri of selected image from phone storage.
                        mProfilePicUri = data.data!!

                        GlideLoader().loadUserPicture(
                            this,
                            mProfilePicUri!!,
                            binding.ivProfilePic
                        )

                        binding.ivProfilePic.scaleType = ImageView.ScaleType.CENTER_CROP
//                        Firestore().uploadImage(this, Constants.PROFILE_PIC_PATH, mProfilePicUri!!)

                    } catch (e: IOException) {
                        e.printStackTrace()
                        showErrorSnackBar("Image selection failed!", true)
                    }
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // A log is printed when user close or cancel the image selection.
            Log.e("Request Cancelled", "Image selection cancelled")
        }
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

    override fun onClick(v: View?) {
        if (v != null) {
            when (v) {
                binding.ivProfilePic -> {
                    PermissionsUtil().checkStoragePermission(this)
                }
                binding.btnSaveProfile -> {
                    profileDataValidation()
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