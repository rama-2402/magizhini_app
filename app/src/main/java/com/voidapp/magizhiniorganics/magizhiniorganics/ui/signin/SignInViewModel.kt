package com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirebaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignInViewModel(
    private val fbRepository: FirestoreRepository,
    private val firebaseRepository: FirebaseRepository
): ViewModel() {

    private val _loginStatus = MutableStateFlow("")
    val loginStatus = _loginStatus.asStateFlow()

    fun getPhoneNumber(): String? = fbRepository.getPhoneNumber()
    fun getCurrentUserId(): String? = fbRepository.getCurrentUserId()

    fun signIn(phoneAuthCredential: PhoneAuthCredential) = viewModelScope.launch (Dispatchers.IO) {
        if (
            fbRepository.signInWithPhoneAuthCredential(phoneAuthCredential)
        ) {
//            withContext(Dispatchers.Main) {
                _loginStatus.value = "complete"
//            }
        } else {
//            withContext(Dispatchers.Main) {
                _loginStatus.value = "failed"
//            }
        }
    }

    suspend fun checkUserProfileDetails(): String = fbRepository.checkUserProfileDetails()

    suspend fun createNewCustomerProfile() = firebaseRepository.createNewCustomerProfile()

}