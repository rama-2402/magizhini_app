package com.voidapp.magizhiniorganics.magizhiniorganics.ui.business

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Career

class BusinessViewModel(
    val dbRepository: DatabaseRepository,
    val fbRepository: FirestoreRepository
): ViewModel() {

    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate


//    fun getCareersDocLink() = viewModelScope.launch {
//        val career: MutableList<Career>? = fbRepository.getCareersDocLink()
//        withContext(Dispatchers.Main) {
//            _uiUpdate.value = UiUpdate.OpenUrl(career)
//        }
//    }

    fun setEmptyUI() {
        _uiUpdate.value = UiUpdate.Empty
    }

    sealed class UiUpdate {
        data class UpdatedNewPartner(val isSuccessful: Boolean): UiUpdate()
        data class OpenUrl(val career: MutableList<Career>?): UiUpdate()

        object Empty: UiUpdate()
    }
}