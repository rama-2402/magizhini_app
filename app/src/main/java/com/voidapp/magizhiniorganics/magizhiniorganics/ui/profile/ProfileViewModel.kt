package com.voidapp.magizhiniorganics.magizhiniorganics.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirebaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CustomerProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel (
    val dbRepository: DatabaseRepository,
    val fsRepository: FirestoreRepository,
    val fbRepository: FirebaseRepository
        ) : ViewModel() {

    private var _isNewUser: MutableLiveData<Boolean> = MutableLiveData()
    val isNewUser: LiveData<Boolean> = _isNewUser
    private var _userProfile: MutableLiveData<UserProfileEntity> = MutableLiveData()
    val userProfile: LiveData<UserProfileEntity> = _userProfile
    private var _profileUploadStatus: MutableLiveData<Boolean> = MutableLiveData()
    val profileUploadStatus: LiveData<Boolean> = _profileUploadStatus
    private var _profileImageUploadStatus: MutableLiveData<String> = MutableLiveData()
    val profileImageUploadStatus: LiveData<String> = _profileImageUploadStatus

    fun getUserProfile() = viewModelScope.launch (Dispatchers.IO) {
        val profile = dbRepository.getProfileData()!!
        withContext(Dispatchers.Main) {
         _userProfile.value = profile
        }
    }

    fun uploadProfile(profile: UserProfile) = viewModelScope.launch(Dispatchers.IO) {
        if (fsRepository.uploadProfile(profile)) {
            CustomerProfile(
                profile.id,
                profile.name,
                profile.profilePicUrl
            ).also {
                if (fbRepository.uploadUserProfile(it)) {
                    withContext(Dispatchers.Main) {
                        _profileUploadStatus.value = true
                    }
                } else {
                        _profileUploadStatus.value = false
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                _profileUploadStatus.value = false
            }
        }
    }

    fun uploadProfilePic(path: String, uri: Uri, extension: String) = viewModelScope.launch(Dispatchers.IO) {
        val status = fsRepository.uploadImage(path, uri, extension)
        if (
            status == "failed"
        ) {
            withContext(Dispatchers.Main) {
                _profileImageUploadStatus.value = "failed"
            }
        } else {
            withContext(Dispatchers.Main) {
                _profileImageUploadStatus.value = status
            }
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
            fsRepository.createWallet(it)
        }
    }

}