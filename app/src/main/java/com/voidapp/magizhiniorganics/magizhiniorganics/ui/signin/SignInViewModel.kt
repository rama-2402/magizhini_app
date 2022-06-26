package com.voidapp.magizhiniorganics.magizhiniorganics.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirebaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.USERS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SignInViewModel(
    private val fbRepository: FirestoreRepository,
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    var phoneAuthID: String = ""

    private val _loginStatus = MutableStateFlow("")
    val loginStatus = _loginStatus.asStateFlow()

    fun getCurrentUserId(): String? = fbRepository.getCurrentUserId()

    fun setEmptyStatus() {
        _loginStatus.value = ""
    }

    suspend fun checkUserProfileDetails(): String = fbRepository.checkUserProfileDetails()

    suspend fun createNewCustomerProfile() = firebaseRepository.createNewCustomerProfile()

    fun checkForPreviousProfiles(phoneNumber: String) = viewModelScope.launch(Dispatchers.IO) {
        FirebaseFirestore.getInstance()
            .collection(USERS)
            .document(getCurrentUserId()!!)
            .get().await().toObject(UserProfile::class.java)?.let {
                if (it.phNumber == phoneNumber) {
                    withContext(Dispatchers.Main) {
                        _loginStatus.value = "old"
                        return@withContext
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _loginStatus.value = "mismatch"
                        return@withContext
                    }
                }
            } ?: let {
            val docs = FirebaseFirestore.getInstance()
                .collection(USERS)
                .whereEqualTo("phNumber", phoneNumber)
                .get().await()

            if (docs.isEmpty) {
                withContext(Dispatchers.Main) {
                    _loginStatus.value = "new"
                    return@withContext
                }
            }

            if (docs.documents.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    _loginStatus.value = "new"
                    return@withContext
                }
            }

            for (doc in docs.documents) {
                phoneAuthID = doc.id
                if (getCurrentUserId()!! == doc.id) {
                    withContext(Dispatchers.Main) {
                        _loginStatus.value = "old"
                        return@withContext
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _loginStatus.value = "port"
                        return@withContext
                    }
                }
            }
        }
    }
}
