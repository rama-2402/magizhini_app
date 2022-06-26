package com.voidapp.magizhiniorganics.magizhiniorganics.ui.cwm.allCWM

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CWMViewModel(
    private val dbRepository: DatabaseRepository,
    private val fbRepository: FirestoreRepository
): ViewModel() {

    private val _status: MutableStateFlow<NetworkResult> = MutableStateFlow<NetworkResult>(
        NetworkResult.Empty)
    val status: StateFlow<NetworkResult> = _status

    fun setEmptyStatus() {
        _status.value = NetworkResult.Empty
    }

    fun getAllCWMDishes() = viewModelScope.launch {
        _status.value = NetworkResult.Loading("")
        _status.value = fbRepository.getAllCWMDishes()
    }

    fun getHowToVideo(where: String) = viewModelScope.launch {
        _status.value = NetworkResult.Loading("")
        val url = fbRepository.getHowToVideo(where)
        _status.value = NetworkResult.Success("how", url)
    }
}