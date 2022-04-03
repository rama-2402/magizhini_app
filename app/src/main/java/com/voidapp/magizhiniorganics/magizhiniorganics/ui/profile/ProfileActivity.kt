package com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.R
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.ActivityProfileBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.databinding.DialogBottomAddReferralBinding
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.BaseActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.dialogs.LoadStatusDialog
import com.voidapp.magizhiniorganics.magizhiniorganics.ui.home.HomeActivity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BOOLEAN
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LOGIN_STATUS
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.LONG
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PHONE_NUMBER
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.PROFILE_PIC_PATH
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.STRING
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USER_ID
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


class ProfileActivity :
    BaseActivity(),
    KodeinAware
{

    lateinit var binding: ActivityProfileBinding
    override val kodein by kodein()

    private lateinit var viewModel: ProfileViewModel
    private val factory: ProfileViewModelFactory by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //setting the theme and view binding
        setTheme(R.style.Theme_MagizhiniOrganics_NoActionBar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        viewModel = ViewModelProvider(this, factory).get(ProfileViewModel::class.java)

        with(binding) {
//            ivHeader.startAnimation(AnimationUtils.loadAnimation(this@ProfileActivity, R.anim.slide_in_top_bounce))
//            tvProfile.startAnimation(AnimationUtils.loadAnimation(this@ProfileActivity, R.anim.slide_in_top_bounce))
//            llProfileName.startAnimation(AnimationUtils.loadAnimation(this@ProfileActivity, R.anim.slide_in_top_bounce))
            svProfileBody.startAnimation(AnimationUtils.loadAnimation(this@ProfileActivity, R.anim.slide_up))
        }

        viewModel.phoneNumber = intent.getStringExtra(PHONE_NUMBER).toString()
        viewModel.userID = intent.getStringExtra(USER_ID).toString()

        lifecycleScope.launch {
            delay(1010)
            binding.ivProfilePic.visible()
        }

        activityInit()
        observers()
        clickListeners()

        if (savedInstanceState != null) {
            viewModel.phoneNumber = savedInstanceState.getString(PHONE_NUMBER)
        }
    }

    private fun observers() {

        viewModel.uiEvent.observe(this) { event ->
            when(event) {
                is UIEvent.Toast -> showToast(this, event.message, event.duration)
                is UIEvent.SnackBar -> showErrorSnackBar(event.message, event.isError)
                is UIEvent.ProgressBar -> {
                    if (event.visibility) {
                        showProgressDialog()
                    } else {
                        hideProgressDialog()
                    }
                }
                is UIEvent.EmptyUIEvent -> return@observe
                else -> Unit
            }
            viewModel.setEmptyUiEvent()
        }

        viewModel.uiUpdate.observe(this) { event ->
            when(event) {
                is ProfileViewModel.UiUpdate.PopulateProfileData -> {
                    event.profile?.let {    //profile data will be populated if user data exists
                        populateProfileDetails(it)
                    } ?:let {   //new wallet will be created since this is a new user
                        viewModel.userID?.let {
                            viewModel.createNewUserWallet(it)
                        }
                    }
                }
                is ProfileViewModel.UiUpdate.ReferralStatus -> {
                    event.message?.let {
                        binding.tvReferral.remove()
                        if (event.status) {
                            showErrorSnackBar(
                                "Referral added Successfully. Your referral bonus will be added to your Wallet.",
                                false,
                                LONG
                            )
                        } else {
                            binding.tvReferral.visible()
                            showErrorSnackBar(
                                event.message.toString(),
                                true,
                                LONG
                            )
                        }
                    }
               }
                is ProfileViewModel.UiUpdate.ProfilePicUrl-> {
                    event.imageUrl?.let {
                        //uploaded pic url
                        generateProfileModel(it)
                    } ?: let {
                        dismissLoadStatusDialog()
                        showErrorSnackBar(event.message!!, true)
                    }
                }
                is ProfileViewModel.UiUpdate.ProfileUploadStatus -> {
                    dismissLoadStatusDialog()
                    if (event.status) {
                        SharedPref(this).putData(LOGIN_STATUS, Constants.BOOLEAN, false)   //stating it is not new user
                        newUserTransitionFromProfile()
                    } else {
                        showErrorSnackBar(event.message!!, true)
                    }
                }
                is ProfileViewModel.UiUpdate.ShowLoadStatusDialog -> showLoadStatusDialog("", event.message!!, event.data!!)
                is ProfileViewModel.UiUpdate.UpdateLoadStatusDialog -> updateLoadStatusDialogText(event.data!!, event.message!!)
                is ProfileViewModel.UiUpdate.Empty -> return@observe
                else -> Unit
            }
            viewModel.setEmptyStatus()
        }
    }

    private fun showLoadStatusDialog(title: String, body: String, content: String) {
        LoadStatusDialog.newInstance(title, body, content).show(supportFragmentManager,
            Constants.LOAD_DIALOG
        )
    }

    private fun dismissLoadStatusDialog() {
        (supportFragmentManager.findFragmentByTag(Constants.LOAD_DIALOG) as? DialogFragment)?.dismiss()
    }

    private fun updateLoadStatusDialogText(data: String, message: String) {
        LoadStatusDialog.statusContent = message
        LoadStatusDialog.statusText.value = data
    }

    private fun populateProfileDetails(userProfile: UserProfileEntity) {
        binding.apply {
            etProfileName.setText(userProfile.name)
            tvDob.text = userProfile.dob
            tvPhoneNumber.text = userProfile.phNumber
            etAlternateNumber.setText(userProfile.alternatePhNumber)
            etEmailId.setText(userProfile.mailId)
            etAddressOne.setText(userProfile.address[0].addressLineOne)
            etAddressTwo.setText(userProfile.address[0].addressLineTwo)
            spArea.setSelection(userProfile.address[0].LocationCodePosition)
            tvGps.text = userProfile.address[0].gpsAddress
//            mLatitude = userProfile.address[0].gpsLatitude
//            mLongitude = userProfile.address[0].gpsLongitude
//            mAddress = userProfile.address[0].gpsAddress
//            if (
//                userProfile.referralId != "" ||
//                SharedPref(this@ProfileActivity).getData(LOGIN_STATUS, BOOLEAN, false)
//            ) {
                tvReferral.remove()
//            }
            //hiding the referral code area for existing user
            if (userProfile.profilePicUrl != "") {
                ivProfilePic.loadImg(userProfile.profilePicUrl) {
                    supportStartPostponedEnterTransition()
                }
            }
        }

        viewModel.getAllActiveOrders().observe(this) {
            it?.let {
                viewModel.purchaseHistory.clear()
                viewModel.purchaseHistory.addAll(it)
            }
        }
        viewModel.getAllActiveSubscriptions().observe(this) {
            it?.let {
                viewModel.subscriptions.clear()
                viewModel.subscriptions.addAll(it)
            }
        }
    }

    private fun activityInit() {
        binding.ivProfilePic.clipToOutline = true
        viewModel.getUserProfile()
    }

    private fun clickListeners() {
        with(binding) {
            ivProfilePic.setOnClickListener {
                if (PermissionsUtil.hasStoragePermission(this@ProfileActivity)) {
                    getAction.launch(pickImageIntent)
                } else {
                    showExitSheet(this@ProfileActivity, "The App Needs Storage Permission to access profile picture from Gallery. \n\n Please provide ALLOW in the following Storage Permissions", "permission")
                }
            }
            btnSaveProfile.setOnClickListener {
                if (!NetworkHelper.isOnline(this@ProfileActivity)) {
                    showErrorSnackBar("Please check your Internet Connection", true)
                    return@setOnClickListener
                }
                profileDataValidation()
            }
            llDob.setOnClickListener{
                viewModel.DobLong?.let {
                    val selectedDateMap = HashMap<String, Long>()
                    selectedDateMap["date"] = it
                    selectedDateMap["month"] = it
                    selectedDateMap["year"] = it
                    DatePickerLib.showCalendar(this@ProfileActivity, this@ProfileActivity, null, System.currentTimeMillis(), selectedDateMap)
                } ?:let {
                    DatePickerLib.showCalendar(this@ProfileActivity, this@ProfileActivity, null, System.currentTimeMillis(), null)
                }
            }
            tvReferral.setOnClickListener{
                showExitSheet(this@ProfileActivity, "Magizhini Referral Program Offers Customers Referral Bonus Rewards for each successful referrals. Please enter your Referrer's Registered Mobile number with Magizhini to avail Referral Bonus! Click Proceed to Continue", "referral")
            }
            llGps.setOnClickListener{
                //implements maps and gps location picker if needed
            }
        }
    }

    //generating the profile model for uploading to firestore
    //and assigning it to mProfile variable for later use
    private fun generateProfileModel(profilePicUrl: String) {
        viewModel.userProfile?.let {
            viewModel.profilePicUri?.let {
                updateUserProfile(profilePicUrl)
            } ?: updateUserProfile(it.profilePicUrl)
        } ?: createNewUserModel(profilePicUrl)
    }

    private fun createNewUserModel(profilePicUrl: String) {
        val address = arrayListOf<Address>(generateAddressObject())
        UserProfile(
            id = viewModel.userID!!,
            name = binding.etProfileName.text.toString().trim(),
            phNumber = binding.tvPhoneNumber.text.toString().trim(),
            alternatePhNumber = binding.etAlternateNumber.text.toString().trim(),
            dob = binding.tvDob.text.toString(),
            address = address,
            mailId = binding.etEmailId.text.toString().trim(),
            profilePicUrl = profilePicUrl,
            referrerNumber = viewModel.referralCode ?: ""
        ).let { profile ->
            profile.extras = if (profile.referrerNumber == "") {
                 arrayListOf("no")
            } else {
                arrayListOf("yes")
            }
            viewModel.uploadProfile(profile)
        }
    }

    private fun updateUserProfile(profilePicUrl: String) {
        viewModel.userProfile?.let { profile ->
            profile.id = viewModel.userID!!
            profile.name = binding.etProfileName.text.toString().trim()
            profile.phNumber = binding.tvPhoneNumber.text.toString().trim()
            profile.alternatePhNumber = binding.etAlternateNumber.text.toString().trim()
            profile.dob = binding.tvDob.text.toString()
            profile.address[0] = generateAddressObject()
            profile.mailId = binding.etEmailId.text.toString().trim()
            profile.profilePicUrl = profilePicUrl
            profile.purchaseHistory.addAll(viewModel.purchaseHistory)
            profile.subscriptions.addAll(viewModel.subscriptions)
            viewModel.uploadProfile(profile = profile.toUserProfile())
        }
    }

    private fun generateAddressObject(): Address {
        return Address (
            userId = binding.etProfileName.text.toString().trim(),
            addressLineOne = binding.etAddressOne.text.toString().trim(),
            addressLineTwo = binding.etAddressTwo.text.toString().trim(),
            city = binding.spCity.selectedItem.toString(),
            LocationCode = binding.spArea.selectedItem.toString(),
            LocationCodePosition = binding.spArea.selectedItemPosition,
//            gpsLatitude = mLatitude,
//            gpsLongitude = mLongitude,
//            gpsAddress = mAddress
        )
    }

    //validating the data entered before uploading
    private fun profileDataValidation() {
        binding.apply {
            when {
                etProfileName.text.isNullOrEmpty() -> {
                    etProfileName.error = "*required"
                    return@apply
                }
                etEmailId.text.isNullOrEmpty() -> {
                    etEmailId.error = "*required"
                    return@apply
                }
                !Patterns.EMAIL_ADDRESS.matcher(etEmailId.text.toString().trim()).matches() -> {
                    binding.etlEmailId.error = "*Enter a valid Email ID"
                    return@apply
                }
                etAddressOne.text.isNullOrEmpty() -> {
                    etlAddressOne.error = "* required"
                    return@apply
                }
                etAddressTwo.text.isNullOrEmpty() -> {
                    etlAddressTwo.error = "* required"
                    return@apply
                }
                tvDob.text.toString() == " DD / MM / YYYY " -> {
                    showErrorSnackBar("Date of Birth required", true)
                    return@apply
                }
                else -> {
                    viewModel.profilePicUri?.let {
                        compressImageToNewFile(this@ProfileActivity, it)?.let { file ->
                            viewModel.tempFile = file
                            viewModel.uploadProfilePic(
                                PROFILE_PIC_PATH,
                                file.toUri(),
                                ".jpg"
                            )
                        }
                    } ?: generateProfileModel("")
                }
            }
        }
    }

    fun selectedCalendarDate(date: Long) {
        // uploading dob should be in 00/00/0000 format
        binding.tvDob.text = TimeUtil().getCustomDate(dateLong = date)
        viewModel.DobLong = date
        SharedPref(this).putData(Constants.DOB, Constants.STRING, TimeUtil().getCustomDate(dateLong = date).substring(0,5))
    }

    fun openReferral() {
        showReferralBs()
    }

    private fun showReferralBs() {
        //BS to add referral number
        val dialogBsAddReferral = BottomSheetDialog(this, R.style.BottomSheetDialog)

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
                dialogBsAddReferral.dismiss()
                viewModel.applyReferralNumber(code)
            }
        }

        dialogBsAddReferral.show()
    }

    fun exitProfileWithoutChange() {
        //on back pressed if it is new user the app is closed if not it means the user is already logged in so it will move to home activity
        viewModel.userProfile?.let {
            transitionFromProfile()
        } ?:let {
            finish()
            finishAffinity()
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

    override fun onResume() {
        viewModel.userID = SharedPref(this).getData(USER_ID, STRING, "").toString()
        viewModel.phoneNumber = SharedPref(this).getData(PHONE_NUMBER, STRING, "").toString()
        binding.tvPhoneNumber.text = viewModel.phoneNumber
        super.onResume()
    }

    override fun onBackPressed() {
        viewModel.userProfile?.let {
            showExitSheet(this, "Exit Profile Update","close")
        } ?: showExitSheet(this, "Cancel Registration", "close")
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putString(PHONE_NUMBER, viewModel.phoneNumber.toString())
        super.onSaveInstanceState(outState, outPersistentState)
    }

    fun proceedToRequestPermission() = PermissionsUtil.requestStoragePermissions(this)

    fun proceedToRequestManualPermission() = this.openAppSettingsIntent()

    private val getAction = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.profilePicUri = result.data?.data
        viewModel.profilePicUri?.let { uri -> binding.ivProfilePic.loadImg(uri) {
            supportStartPostponedEnterTransition()
        }
        }
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

}