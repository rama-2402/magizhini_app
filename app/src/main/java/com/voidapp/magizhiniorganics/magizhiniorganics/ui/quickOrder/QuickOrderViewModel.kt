package com.voidapp.magizhiniorganics.magizhiniorganics.ui.quickOrder

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Operation
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.UserProfileDao
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Address
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class QuickOrderViewModel(
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository
): ViewModel() {

    val orderListUri: MutableList<Uri> = mutableListOf()
    var userProfile: UserProfileEntity? = null
    var addressContainer: Address? = null

    var mCheckedAddressPosition: Int = 0
    var addressPosition: Int = 0

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(
        NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    fun setEmptyStatus() {
        _status.value = NetworkResult.Empty
    }

    fun addNewImageUri(data: Uri) {
        orderListUri.add(data)
    }

    fun getAddress() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val profile = dbRepository.getProfileData()
            profile?.let {
                userProfile = it
                _status.value = NetworkResult.Success("address", it.address)
            }
        } catch (e: IOException) {
            _status.value = NetworkResult.Failed("address", "Failed to fetch address. Please try again later")
        }
    }

    fun deleteAddress(position: Int) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val localUpdate = async {
                userProfile?.let {
                    it.address.removeAt(position)
                    dbRepository.upsertProfile(it)
                }
            }
            val cloudUpdate = async {
                userProfile?.let {
                    updateAddress(it.id, it.address,"update")
                }
            }

            localUpdate.await()
            cloudUpdate.await()
            _status.value = NetworkResult.Success("addressUpdate", "Address Deleted")
        } catch (e: IOException) {
            _status.value = NetworkResult.Success("addressUpdate", "Failed to delete address. Try later")
            e.message?.let { fbRepository.logCrash("checkout: update address to profile from db", it) }
        }
    }

    private suspend fun updateAddress(id: String, address: ArrayList<Address>, status: String) {
        if(status == "add") {
            fbRepository.addAddress(id, address[0])
        } else {
            fbRepository.updateAddress(id, address)
        }
    }

    fun addAddress(newAddress: Address) = viewModelScope.launch (Dispatchers.IO) {
        try {
            val list = arrayListOf<Address>(newAddress)

            val localUpdate = async {
                userProfile?.let {
                    it.address.add(newAddress)
                    dbRepository.upsertProfile(it)
                }
            }

            val cloudUpdate = async {
                    userProfile?.let {
                        updateAddress(it.id, list,"add")
                    }
                }

            localUpdate.await()
            cloudUpdate.await()
            _status.value = NetworkResult.Success("addressUpdate", "Address added")
        } catch (e: IOException) {
            _status.value = NetworkResult.Success("addressUpdate", "Failed to add address. Try later")
            e.message?.let { fbRepository.logCrash("checkout: add address to profile from db", it) }
        }
    }

    fun updateAddress(address: Address) = viewModelScope.launch (Dispatchers.IO){
        try {
            userProfile?.let { profile ->
                profile.address[addressPosition].apply {
                    userId = address.userId
                    addressLineOne = address.addressLineOne
                    addressLineTwo = address.addressLineTwo
                    LocationCode = address.LocationCode
                    LocationCodePosition = address.LocationCodePosition
                }.run {
                    val localUpdate = async {
                        dbRepository.upsertProfile(profile)
                    }
                    val cloudUpdate = async {
                        updateAddress(profile.id, profile.address, "update")
                    }

                    localUpdate.await()
                    cloudUpdate.await()
                    _status.value = NetworkResult.Success("addressUpdate", "Address updated")
                }
            }
        } catch (e: IOException) {
            _status.value = NetworkResult.Success("addressUpdate", "Failed to update address. Try later")
            e.message?.let { fbRepository.logCrash("checkout: update address to profile from db", it) }
        }
    }

    fun sendGetEstimateRequest() = viewModelScope.launch {
        _status.value = NetworkResult.Loading("")

    }

    fun sendOrderPlaceRequest() = viewModelScope.launch {
        _status.value = NetworkResult.Loading("")

    }
}