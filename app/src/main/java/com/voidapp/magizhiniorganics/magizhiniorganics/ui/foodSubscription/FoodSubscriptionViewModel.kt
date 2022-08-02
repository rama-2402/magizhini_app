package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.FoodSubscriptionUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.PushNotificationUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

class FoodSubscriptionViewModel(
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository,
    private val foodSubscriptionUseCase: FoodSubscriptionUseCase
): ViewModel() {
    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate
    private val _uiEvent: MutableLiveData<UIEvent> = MutableLiveData()
    val uiEvent: LiveData<UIEvent> = _uiEvent

    var userProfile: UserProfileEntity? = null
    var wallet: Wallet? = null

    val ammaSpecials: MutableList<AmmaSpecial> = mutableListOf()
    var lunchMap: HashMap<String, Double> = hashMapOf()

    val selectedEventDates: MutableList<Long> = mutableListOf()
    var currentSubOption: String = "month"
    var currentCountOption : Int = 0
    var totalPrice: Double = 0.0

    fun setEmptyUiEvent() {
        _uiEvent.value = UIEvent.EmptyUIEvent
    }

    fun setEmptyStatus() {
        _uiUpdate.value = UiUpdate.Empty
    }

    fun getAmmaSpecials() = viewModelScope.launch {
//        val specials = foodSubscriptionUseCase.getAllAmmaSpecials()
//        val banners = foodSubscriptionUseCase.getAllBanners()
        val specials = generateSampleSpecials()
//        _uiUpdate.value = UiUpdaate.PopulateAmmaSpecials(specials, banners)
        ammaSpecials.clear()
        specials?.let { specialsList ->
            ammaSpecials.addAll(specialsList)
            specialsList.forEach {
                lunchMap[it.foodDay] = if (it.discountedPrice == 0.0) {
                    it.price
                } else {
                    it.discountedPrice
                }
            }
        }
        _uiUpdate.value = UiUpdate.PopulateAmmaSpecials(specials, null)
    }

    private fun generateSampleSpecials(): MutableList<AmmaSpecial> {
        val mon = AmmaSpecial(
            "",
            thumbnailUrl = "https://firebasestorage.googleapis.com/v0/b/magizhiniorganics-56636.appspot.com/o/products%2FTomato%20%2F%20%E0%AE%A4%E0%AE%95%E0%AF%8D%E0%AE%95%E0%AE%BE%E0%AE%B3%E0%AE%BF.jpg?alt=media&token=5171b4cf-0a61-47f8-99e4-2ff91abe828c",
            foodName = "Beet root saatham and appalam and murukku",
            foodDay = "Monday",
            foodTime = "Lunch",
            price = 100.50,
            discountedPrice = 80.00,
            description = "Something something and something for that and this products purchase",
            ingredients = arrayListOf("podi", "powder","water/thanni","sombu/ nee vanthu oombu","enga da pora unakaaga thaan ithu panren/and somethig like that")
        )

        val tue = AmmaSpecial(
            "",
            thumbnailUrl = "https://firebasestorage.googleapis.com/v0/b/magizhiniorganics-56636.appspot.com/o/products%2FTomato%20%2F%20%E0%AE%A4%E0%AE%95%E0%AF%8D%E0%AE%95%E0%AE%BE%E0%AE%B3%E0%AE%BF.jpg?alt=media&token=5171b4cf-0a61-47f8-99e4-2ff91abe828c",
            foodName = "random saatham and rice for eating and dinning and something more like that for eating",
            foodDay = "Tuesday",
            foodTime = "Dinner",
            price = 100.50,
            description = "Something something and something for that and this products purchase",
            ingredients = arrayListOf("podi", "powder","water/thanni","sombu/ nee vanthu oombu","enga da pora unakaaga thaan ithu panren/and somethig like that")
        )
        val wed = AmmaSpecial(
            "",
            thumbnailUrl = "https://firebasestorage.googleapis.com/v0/b/magizhiniorganics-56636.appspot.com/o/products%2FTomato%20%2F%20%E0%AE%A4%E0%AE%95%E0%AF%8D%E0%AE%95%E0%AE%BE%E0%AE%B3%E0%AE%BF.jpg?alt=media&token=5171b4cf-0a61-47f8-99e4-2ff91abe828c",
            foodName = "random saatham and rice for eating and dinning and something more like that for eating",
            foodDay = "Wednesday",
            foodTime = "Dinner",
            price = 200.0,
            description = "Something something and something for that and this products purchase",
            ingredients = arrayListOf("podi", "powder","water/thanni","sombu/ nee vanthu oombu","enga da pora unakaaga thaan ithu panren/and somethig like that")
        )
        val thu = AmmaSpecial(
            "",
            thumbnailUrl = "https://firebasestorage.googleapis.com/v0/b/magizhiniorganics-56636.appspot.com/o/products%2FTomato%20%2F%20%E0%AE%A4%E0%AE%95%E0%AF%8D%E0%AE%95%E0%AE%BE%E0%AE%B3%E0%AE%BF.jpg?alt=media&token=5171b4cf-0a61-47f8-99e4-2ff91abe828c",
            foodName = "random saatham and rice for eating and dinning and something more like that for eating",
            foodDay = "Thursday",
            foodTime = "Dinner",
            price = 100.50,
            discountedPrice = 50.0,
            description = "Something something and something for that and this products purchase",
            ingredients = arrayListOf("podi", "powder","water/thanni","sombu/ nee vanthu oombu","enga da pora unakaaga thaan ithu panren/and somethig like that")
        )
        val fri = AmmaSpecial(
            "",
            thumbnailUrl = "https://firebasestorage.googleapis.com/v0/b/magizhiniorganics-56636.appspot.com/o/products%2FTomato%20%2F%20%E0%AE%A4%E0%AE%95%E0%AF%8D%E0%AE%95%E0%AE%BE%E0%AE%B3%E0%AE%BF.jpg?alt=media&token=5171b4cf-0a61-47f8-99e4-2ff91abe828c",
            foodName = "random saatham and rice for eating and dinning and something more like that for eating",
            foodDay = "Friday",
            foodTime = "Dinner",
            price = 20.50,
            description = "Something something and something for that and this products purchase",
            ingredients = arrayListOf("podi", "powder","water/thanni","sombu/ nee vanthu oombu","enga da pora unakaaga thaan ithu panren/and somethig like that")
        )
        val sat = AmmaSpecial(
            "",
            thumbnailUrl = "https://firebasestorage.googleapis.com/v0/b/magizhiniorganics-56636.appspot.com/o/products%2FTomato%20%2F%20%E0%AE%A4%E0%AE%95%E0%AF%8D%E0%AE%95%E0%AE%BE%E0%AE%B3%E0%AE%BF.jpg?alt=media&token=5171b4cf-0a61-47f8-99e4-2ff91abe828c",
            foodName = "random saatham and rice for eating and dinning and something more like that for eating",
            foodDay = "Saturday",
            foodTime = "Dinner",
            price = 500.50,
            discountedPrice = 90.00,
            description = "Something something and something for that and this products purchase",
            ingredients = arrayListOf("podi", "powder","water/thanni","sombu/ nee vanthu oombu","enga da pora unakaaga thaan ithu panren/and somethig like that")
        )

        return mutableListOf(mon, tue, wed, thu, fri, sat)
    }

    //order activity
    fun getProfile() = viewModelScope.launch(Dispatchers.IO) {
        dbRepository.getProfileData()?.let { profile ->
            withContext(Dispatchers.Main) {
                userProfile = profile
                _uiUpdate.value = UiUpdate.PopulateUserProfile(profile)
            }
        }
    }

    fun placeOrderOnlinePayment(orderDetailsMap: HashMap<String, Any>) = viewModelScope.launch {
        userProfile?.let { profile ->
            AmmaSpecialOrder(
                id = "",
                customerID = profile.id,
                orderDate = TimeUtil().getCurrentDate(),
                price = totalPrice,
                userName = profile.name,
                addressOne = orderDetailsMap["one"].toString(),
                addressTwo = orderDetailsMap["two"].toString(),
                city = orderDetailsMap["city"].toString(),
                code = orderDetailsMap["code"].toString(),
                phoneNumber = orderDetailsMap["phoneNumber"].toString(),
                mailID = orderDetailsMap["mailID"].toString(),
                leafNeeded = orderDetailsMap["leaf"].toString().toBoolean(),
                orderType = currentSubOption,
                orderCount = currentCountOption,
                deliveryDates = selectedEventDates as ArrayList<Long>
            ).let {
                _uiUpdate.value = UiUpdate.CreateStatusDialog(null, null)
                foodSubscriptionUseCase.
                    placeFoodSubscriptionOnlinePayment(
                        it,
                        orderDetailsMap["transactionID"]!!.toString()
                    ).onEach {  result ->
                        withContext(Dispatchers.Main) {
                            when(result) {
                                is NetworkResult.Success -> {
                                    when(result.message) {
                                        "placing" -> _uiUpdate.value = UiUpdate.PlacingOrder("Validating Transaction...", "validating")
                                        "placed" -> {
                                            _uiUpdate.value = UiUpdate.PlacedOrder(
                                                "Subscription Placed Successfully!",
                                                "success"
                                            )
                                            delay(1800)
                                            _uiUpdate.value = UiUpdate.DismissStatusDialog(true)
                                            sendPushNotification(it.customerID)
                                        }
                                    }
                                }
                                is NetworkResult.Failed -> {
                                    _uiUpdate.value = UiUpdate.UpdateStatusDialog(result.message, "fail")
                                    delay(1800)
                                    _uiUpdate.value = UiUpdate.DismissStatusDialog(false)
                                }
                                else -> Unit
                            }
                        }
                }.launchIn(this)
            }
        }?: run {
            _uiEvent.value = UIEvent.ProgressBar(false)
            _uiEvent.value = UIEvent.SnackBar(
                "Couldn't fetch your profile. Please logout and log back in again to continue",
                true
            )
        }
    }

    suspend fun fetchWallet(): Wallet? {
        userProfile?.let {
            when(val result = fbRepository.getWallet(it.id)) {
                is NetworkResult.Success -> {
                    wallet = result.data as Wallet
                }
                is NetworkResult.Failed -> {
                    _uiEvent.value = UIEvent.ProgressBar(false)
                    _uiEvent.value = UIEvent.SnackBar(result.data.toString(), true)
                }
                else -> Unit
            }
        } ?: let {
            _uiEvent.value = UIEvent.ProgressBar(false)
            _uiEvent.value = UIEvent.SnackBar("Failed to fetch Wallet. Try other payment", true)
        }
        return wallet
    }

    fun placeOrderWalletPayment(orderDetailsMap: HashMap<String, Any>) = viewModelScope.launch {
        userProfile?.let { profile ->
            AmmaSpecialOrder(
                id = "",
                customerID = profile.id,
                orderDate = TimeUtil().getCurrentDate(),
                price = totalPrice,
                userName = profile.name,
                addressOne = orderDetailsMap["one"].toString(),
                addressTwo = orderDetailsMap["two"].toString(),
                city = orderDetailsMap["city"].toString(),
                code = orderDetailsMap["code"].toString(),
                phoneNumber = orderDetailsMap["phoneNumber"].toString(),
                mailID = orderDetailsMap["mailID"].toString(),
                leafNeeded = orderDetailsMap["leaf"].toString().toBoolean(),
                orderType = currentSubOption,
                orderCount = currentCountOption,
                deliveryDates = selectedEventDates as ArrayList<Long>
            ).let {
                _uiUpdate.value = UiUpdate.CreateStatusDialog(null, null)
                foodSubscriptionUseCase.
                    placeFoodSubscriptionWithWallet(
                        it
                    ).onEach { result ->
                        withContext(Dispatchers.Main) {
                            when(result) {
                                is NetworkResult.Success -> {
                                    when(result.message) {
                                        "transaction" -> _uiUpdate.value = UiUpdate.ValidatingTransaction("Making payment from wallet... ", "transaction")
                                        "validating" -> _uiUpdate.value = UiUpdate.PlacingOrder("Validating Transaction...", "validating")
                                        "placed" -> {
                                            _uiUpdate.value = UiUpdate.PlacedOrder(
                                                "Subscription Placed Successfully!",
                                                "success"
                                            )
                                            delay(1800)
                                            _uiUpdate.value = UiUpdate.DismissStatusDialog(true)
                                            sendPushNotification(it.customerID)
                                        }
                                    }
                                }
                                is NetworkResult.Failed -> {
                                    _uiUpdate.value = UiUpdate.UpdateStatusDialog(result.message, "fail")
                                    delay(1000)
                                    _uiUpdate.value = UiUpdate.DismissStatusDialog(false)
                                }
                                else -> Unit
                            }
                        }
                    }.launchIn(this)
            }
        }?: run {
            _uiEvent.value = UIEvent.ProgressBar(false)
            _uiEvent.value = UIEvent.SnackBar(
                "Couldn't fetch your profile. Please logout and log back in again to continue",
                true
            )
        }
    }

    private suspend fun sendPushNotification(customerID: String) {
        PushNotificationUseCase(fbRepository).sendPushNotification(
            customerID,
            "Amma's Special Subscription Created!",
            "Your Amma's Special Subscription has been created successfully. You can check the progress of your purchase in Subscription History Page.",
            "amma"
            )
    }

    sealed class UiUpdate {
        data class PopulateAmmaSpecials(val ammaSpecials: List<AmmaSpecial>?, val banners: List<Banner>?): UiUpdate()
        data class PopulateUserProfile(val userProfile: UserProfileEntity): UiUpdate()

        //placing order
        data class PlacingOrder(val message: String, val data: String): UiUpdate()
        data class PlacedOrder(val message: String, val data: String): UiUpdate()
        data class ValidatingTransaction(val message: String, val data: String): UiUpdate()

//        status
        data class CreateStatusDialog(val message: String?, val data: String?): UiUpdate()
        data class DismissStatusDialog(val dismiss: Boolean): UiUpdate()
        data class UpdateStatusDialog(val message: String, val data: String): UiUpdate()

        object Empty: UiUpdate()
    }
}