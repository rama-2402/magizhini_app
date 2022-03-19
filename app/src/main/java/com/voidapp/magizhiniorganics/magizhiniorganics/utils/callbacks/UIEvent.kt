package com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks

import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants

sealed class UIEvent {
    data class Toast(val message: String, val duration: String = Constants.SHORT): UIEvent()
    data class SnackBar(val message: String, val isError: Boolean) : UIEvent()
    data class ProgressBar(val visibility: Boolean): UIEvent()
    object EmptyUIEvent: UIEvent()
}