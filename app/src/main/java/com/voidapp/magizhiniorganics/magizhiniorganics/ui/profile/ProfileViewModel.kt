package com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProfileViewModel (
    val dbRepository: DatabaseRepository,
    val fbRepository: FirestoreRepository
        ) : ViewModel() {

    var tempFile: File? = null
    var userID: String? = null
    var phoneNumber: String? = null
    var DobLong: Long? = null
    var referralCode: String? = null
    var profilePicUri: Uri? = null

    val purchaseHistory: MutableList<String> = mutableListOf()
    val subscriptions:  MutableList<String> = mutableListOf()

    var userProfile: UserProfileEntity? = null

    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate
    private val _uiEvent: MutableLiveData<UIEvent> = MutableLiveData()
    val uiEvent: LiveData<UIEvent> = _uiEvent

    fun setEmptyUiEvent() {
        _uiEvent.value = UIEvent.EmptyUIEvent
    }

    fun setEmptyStatus() {
        _uiUpdate.value = UiUpdate.Empty
    }

    fun getUserProfile() = viewModelScope.launch (Dispatchers.IO) {
        dbRepository.getProfileData()?.let { profile ->
            withContext(Dispatchers.Main) {
                if (profile.name != "") {
                    if (profile.referralId != "") {
                        referralCode = profile.referralId
                        _uiUpdate.value = UiUpdate.ReferralStatus(false, null)
                    }
                    userProfile = profile
                    _uiUpdate.value = UiUpdate.PopulateProfileData(profile, null)
                } else {
                    _uiUpdate.value = UiUpdate.ReferralStatus(true, null)
                }
            }
        } ?: withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.PopulateProfileData(null, null)
        }
    }

    fun uploadProfile(profile: UserProfile) = viewModelScope.launch {
        profilePicUri?.let {
            userProfile?.let {
                _uiUpdate.value = UiUpdate.UpdateLoadStatusDialog("Updating Your Profile... ", "upload")
            } ?:let { _uiUpdate.value = UiUpdate.UpdateLoadStatusDialog("Creating Your Profile... ", "upload") }
        } ?:let {
            userProfile?.let {
                _uiUpdate.value = UiUpdate.ShowLoadStatusDialog("Updating Your Profile... ", "upload")
            } ?:let { _uiUpdate.value = UiUpdate.ShowLoadStatusDialog("Creating Your Profile... ", "upload") }
        }

        val status = fbRepository.uploadProfile(profile)
        delay(1800)
        if (status) {
            userProfile?.let {
                _uiUpdate.value =
                    UiUpdate.UpdateLoadStatusDialog("Profile Updated successfully... ", "success")
            } ?:let {
                _uiUpdate.value =
                    UiUpdate.UpdateLoadStatusDialog("Profile Created successfully... ", "success")
            }
            delay(1800)
            _uiUpdate.value = UiUpdate.ProfileUploadStatus(true, null)
        } else {
            userProfile?.let {
                _uiUpdate.value = UiUpdate.UpdateLoadStatusDialog( "Server Error! Failed to update profile. Try later", "fail")
            } ?: let {
                _uiUpdate.value = UiUpdate.UpdateLoadStatusDialog( "Server Error! Failed to create profile. Try later", "fail")
            }
            delay(1000)
            _uiUpdate.value = UiUpdate.ProfileUploadStatus(false, null)
        }
    }

    fun uploadProfilePic(path: String, uri: Uri, extension: String) = viewModelScope.launch {
        _uiUpdate.value = UiUpdate.ShowLoadStatusDialog("Uploading Profile Picture...", "upload")
        val status = fbRepository.uploadImage(path, uri, extension)
        if (
            status == "failed"
        ) {
            _uiUpdate.value = UiUpdate.ProfilePicUrl(null, "Server Error! Failed to upload Profile Picture")
        } else {
            tempFile?.delete()
            tempFile = null
            _uiUpdate.value = UiUpdate.ProfilePicUrl(status, "Server Error! Failed to upload Profile Picture")
        }
    }

    fun createNewUserWallet(id: String) = viewModelScope.launch(Dispatchers.IO) {
        Wallet(
            id = id,
            amount = 0f,
            0L,
            0L,
            listOf()
        ).also {
            fbRepository.createWallet(it)
        }

        UserProfile(
            id = userID!!,
            phNumber = phoneNumber!!
        ).let {
            fbRepository.uploadProfile(it)
        }
    }

    fun applyReferralNumber(code: String) = viewModelScope.launch {
        _uiEvent.value = UIEvent.ProgressBar(true)
        if(fbRepository.checkForReferral(code)) {
            val referralStatus = userID?.let { fbRepository.applyReferralNumber(it, code) } ?: false
            delay(500)
            referralCode = code
            _uiEvent.value = UIEvent.ProgressBar(false)
            _uiUpdate.value = UiUpdate.ReferralStatus(referralStatus, "Server Error! Failed to apply referral")
        } else {
            _uiEvent.value = UIEvent.ProgressBar(false)
            _uiUpdate.value = UiUpdate.ReferralStatus(false, "There is no profile associated with the number. Please enter a valid mobile of the Referrer Account")
        }

    }

    fun getAllActiveOrders() = dbRepository.getAllActiveOrders()
    fun getAllActiveSubscriptions() = dbRepository.getAllActiveSubscriptions()


    sealed class UiUpdate {
        //load satus dialog
        data class ShowLoadStatusDialog(val message: String?, val data: String?): UiUpdate()
        data class UpdateLoadStatusDialog(val message: String?, val data: String?): UiUpdate()
        object DismissLoadStatusDialog: UiUpdate()

        //profile data
        data class PopulateProfileData(val profile: UserProfileEntity?, val message: String?): UiUpdate()
        data class ProfilePicUrl(val imageUrl: String?, val message: String?): UiUpdate()
        data class ProfileUploadStatus(val status: Boolean, val message: String?): UiUpdate()

        //referral
        data class ReferralStatus(val status: Boolean, val message: String?): UiUpdate()

        object Empty: UiUpdate()
    }
}