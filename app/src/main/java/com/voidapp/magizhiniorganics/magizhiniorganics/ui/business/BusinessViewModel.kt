package com.voidapp.magizhiniorganics.magizhiniorganics.ui.business

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BusinessViewModel(
    val dbRepository: DatabaseRepository,
    val fbRepository: FirestoreRepository
): ViewModel() {

    inner class NewPartner (
        var id: String = "",
        val partnerName: String = "",
        val business: String = "",
        val phone: String = "",
        val mail: String = "",
        val social: String? = "",
        val description: String = ""
    )

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

    sealed class UiUpdate {
        data class UpdatedNewPartner(val isSuccessful: Boolean): UiUpdate()
    }
}