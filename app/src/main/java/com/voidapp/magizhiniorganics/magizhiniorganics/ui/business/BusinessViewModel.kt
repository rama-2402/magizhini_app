package com.voidapp.magizhiniorganics.magizhiniorganics.ui.business

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Career
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.NewPartner
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BusinessViewModel(
    val dbRepository: DatabaseRepository,
    val fbRepository: FirestoreRepository
): ViewModel() {

    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate

    fun sendNewPartnerRequest(newPartnersMap: HashMap<String, String>) = viewModelScope.launch(Dispatchers.IO){
        NewPartner(
            partnerName = newPartnersMap["name"].toString(),
            business = newPartnersMap["business"].toString(),
            phone = newPartnersMap["phone"].toString(),
            mail = newPartnersMap["mail"].toString(),
            social = newPartnersMap["social"].toString(),
            description = newPartnersMap["description"].toString()
        ).let {
            when(fbRepository.sendNewPartnerRequest(it)) {
                is NetworkResult.Success -> {
                    withContext(Dispatchers.Main) {
                        _uiUpdate.value = UiUpdate.UpdatedNewPartner( true)
                    }
                }
                is NetworkResult.Failed -> {
                    withContext(Dispatchers.Main) {
                        _uiUpdate.value = UiUpdate.UpdatedNewPartner( true)
                    }
                }
                else -> Unit
            }
        }
    }

    fun getCareersDocLink() = viewModelScope.launch {
        val career: MutableList<Career>? = fbRepository.getCareersDocLink()
        withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.OpenUrl(career)
        }
    }

    fun setEmptyUI() {
        _uiUpdate.value = UiUpdate.Empty
    }

    sealed class UiUpdate {
        data class UpdatedNewPartner(val isSuccessful: Boolean): UiUpdate()
        data class OpenUrl(val career: MutableList<Career>?): UiUpdate()

        object Empty: UiUpdate()
    }
}