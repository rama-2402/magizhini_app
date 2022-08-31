package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

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
import java.text.SimpleDateFormat


class FoodSubscriptionViewModel(
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository,
    private val foodSubscriptionUseCase: FoodSubscriptionUseCase
) : ViewModel() {
    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate
    private val _uiEvent: MutableLiveData<UIEvent> = MutableLiveData()
    val uiEvent: LiveData<UIEvent> = _uiEvent

    var userProfile: UserProfileEntity? = null
    var wallet: Wallet? = null
    var ammaSpecialOrders: MutableList<AmmaSpecialOrder> = mutableListOf()

    val ammaSpecials: MutableList<AmmaSpecial> = mutableListOf()
    var lunchMap: HashMap<String, Double> = hashMapOf()

    var nonDeliveryDatesLong: MutableList<Long>? = null
    var nonDeliveryDatesString: MutableList<String> = mutableListOf()
    val selectedEventDates: MutableList<Long> = mutableListOf()
    var currentSubOption: String = "month"
    var currentCountOption: Int = 0
    var totalPrice: Double = 0.0

    //the outer hashmap holds the data as key and the orderids as values
    //the inner hashmap has orderid as key and the status as value
    val orderStatusMap: HashMap<String, HashMap<String, String>?> = hashMapOf()
    var userID: String? = null
    var selectedOrder: AmmaSpecialOrder? = null

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
        ammaSpecials.clear()
        specials.let { specialsList ->
            ammaSpecials.addAll(specialsList)
            specialsList.forEach {
                lunchMap[it.foodDay] = if (it.discountedPrice == 0.0) {
                    it.price
                } else {
                    it.discountedPrice
                }
            }
        }
//        _uiUpdate.value = UiUpdaate.PopulateAmmaSpecials(specials, banners)
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
            ingredients = arrayListOf(
                "podi",
                "powder",
                "water/thanni",
                "sombu/ nee vanthu oombu",
                "enga da pora unakaaga thaan ithu panren/and somethig like that"
            )
        )

        val tue = AmmaSpecial(
            "",
            thumbnailUrl = "https://firebasestorage.googleapis.com/v0/b/magizhiniorganics-56636.appspot.com/o/products%2FTomato%20%2F%20%E0%AE%A4%E0%AE%95%E0%AF%8D%E0%AE%95%E0%AE%BE%E0%AE%B3%E0%AE%BF.jpg?alt=media&token=5171b4cf-0a61-47f8-99e4-2ff91abe828c",
            foodName = "random saatham and rice for eating and dinning and something more like that for eating",
            foodDay = "Tuesday",
            foodTime = "Dinner",
            price = 100.50,
            description = "Something something and something for that and this products purchase",
            ingredients = arrayListOf(
                "podi",
                "powder",
                "water/thanni",
                "sombu/ nee vanthu oombu",
                "enga da pora unakaaga thaan ithu panren/and somethig like that"
            )
        )
        val wed = AmmaSpecial(
            "",
            thumbnailUrl = "https://firebasestorage.googleapis.com/v0/b/magizhiniorganics-56636.appspot.com/o/products%2FTomato%20%2F%20%E0%AE%A4%E0%AE%95%E0%AF%8D%E0%AE%95%E0%AE%BE%E0%AE%B3%E0%AE%BF.jpg?alt=media&token=5171b4cf-0a61-47f8-99e4-2ff91abe828c",
            foodName = "random saatham and rice for eating and dinning and something more like that for eating",
            foodDay = "Wednesday",
            foodTime = "Dinner",
            price = 200.0,
            description = "Something something and something for that and this products purchase",
            ingredients = arrayListOf(
                "podi",
                "powder",
                "water/thanni",
                "sombu/ nee vanthu oombu",
                "enga da pora unakaaga thaan ithu panren/and somethig like that"
            )
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
            ingredients = arrayListOf(
                "podi",
                "powder",
                "water/thanni",
                "sombu/ nee vanthu oombu",
                "enga da pora unakaaga thaan ithu panren/and somethig like that"
            )
        )
        val fri = AmmaSpecial(
            "",
            thumbnailUrl = "https://firebasestorage.googleapis.com/v0/b/magizhiniorganics-56636.appspot.com/o/products%2FTomato%20%2F%20%E0%AE%A4%E0%AE%95%E0%AF%8D%E0%AE%95%E0%AE%BE%E0%AE%B3%E0%AE%BF.jpg?alt=media&token=5171b4cf-0a61-47f8-99e4-2ff91abe828c",
            foodName = "random saatham and rice for eating and dinning and something more like that for eating",
            foodDay = "Friday",
            foodTime = "Dinner",
            price = 20.50,
            description = "Something something and something for that and this products purchase",
            ingredients = arrayListOf(
                "podi",
                "powder",
                "water/thanni",
                "sombu/ nee vanthu oombu",
                "enga da pora unakaaga thaan ithu panren/and somethig like that"
            )
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
            ingredients = arrayListOf(
                "podi",
                "powder",
                "water/thanni",
                "sombu/ nee vanthu oombu",
                "enga da pora unakaaga thaan ithu panren/and somethig like that"
            )
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
        val deliveryDatesString = mutableListOf<String>()
        selectedEventDates.forEach {
            deliveryDatesString.add(TimeUtil().getCustomDate(dateLong = it))
        }
        userProfile?.let { profile ->
            AmmaSpecialOrder(
                id = "",
                customerID = profile.id,
                orderDate = TimeUtil().getCurrentDate(),
                startDate = orderDetailsMap["start"].toString().toLong(),
                endDate = orderDetailsMap["end"].toString().toLong(),
                price = totalPrice,
                userName = profile.name,
                addressOne = orderDetailsMap["one"].toString(),
                addressTwo = orderDetailsMap["two"].toString(),
                city = orderDetailsMap["city"].toString(),
                code = orderDetailsMap["code"].toString(),
                phoneNumber = orderDetailsMap["phoneNumber"].toString(),
                mailID = orderDetailsMap["mailID"].toString(),
                orderFoodTime = arrayListOf("lunch"),
                leafNeeded = orderDetailsMap["leaf"].toString().toBoolean(),
                orderType = currentSubOption,
                orderCount = currentCountOption,
                deliveryDates = deliveryDatesString as ArrayList<String>
            ).let {
                _uiUpdate.value = UiUpdate.CreateStatusDialog(null, null)
                foodSubscriptionUseCase.placeFoodSubscriptionOnlinePayment(
                    it,
                    orderDetailsMap["transactionID"]!!.toString()
                ).onEach { result ->
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is NetworkResult.Success -> {
                                when (result.message) {
                                    "placing" -> _uiUpdate.value = UiUpdate.PlacingOrder(
                                        "Validating Transaction...",
                                        "validating"
                                    )
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
                                _uiUpdate.value =
                                    UiUpdate.UpdateStatusDialog(result.message, "fail")
                                delay(1800)
                                _uiUpdate.value = UiUpdate.DismissStatusDialog(false)
                            }
                            else -> Unit
                        }
                    }
                }.launchIn(this)
            }
        } ?: run {
            _uiEvent.value = UIEvent.ProgressBar(false)
            _uiEvent.value = UIEvent.SnackBar(
                "Couldn't fetch your profile. Please logout and log back in again to continue",
                true
            )
        }
    }

    suspend fun fetchWallet(): Wallet? {
        userProfile?.let {
            when (val result = fbRepository.getWallet(it.id)) {
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
        val deliveryDatesString = mutableListOf<String>()
        selectedEventDates.forEach {
            deliveryDatesString.add(TimeUtil().getCustomDate(dateLong = it))
        }
        userProfile?.let { profile ->
            AmmaSpecialOrder(
                id = "",
                customerID = profile.id,
                orderDate = TimeUtil().getCurrentDate(),
                startDate = orderDetailsMap["start"].toString().toLong(),
                endDate = orderDetailsMap["end"].toString().toLong(),
                price = totalPrice,
                userName = profile.name,
                addressOne = orderDetailsMap["one"].toString(),
                addressTwo = orderDetailsMap["two"].toString(),
                city = orderDetailsMap["city"].toString(),
                code = orderDetailsMap["code"].toString(),
                phoneNumber = orderDetailsMap["phoneNumber"].toString(),
                mailID = orderDetailsMap["mailID"].toString(),
                orderFoodTime = arrayListOf("lunch"),
                leafNeeded = orderDetailsMap["leaf"].toString().toBoolean(),
                orderType = currentSubOption,
                orderCount = currentCountOption,
                deliveryDates = deliveryDatesString as ArrayList<String>
            ).let {
                _uiUpdate.value = UiUpdate.CreateStatusDialog(null, null)
                foodSubscriptionUseCase.placeFoodSubscriptionWithWallet(
                    it
                ).onEach { result ->
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is NetworkResult.Success -> {
                                when (result.message) {
                                    "transaction" -> _uiUpdate.value =
                                        UiUpdate.ValidatingTransaction(
                                            "Making payment from wallet... ",
                                            "transaction"
                                        )
                                    "validating" -> _uiUpdate.value = UiUpdate.PlacingOrder(
                                        "Validating Transaction...",
                                        "validating"
                                    )
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
                                _uiUpdate.value =
                                    UiUpdate.UpdateStatusDialog(result.message, "fail")
                                delay(1000)
                                _uiUpdate.value = UiUpdate.DismissStatusDialog(false)
                            }
                            else -> Unit
                        }
                    }
                }.launchIn(this)
            }
        } ?: run {
            _uiEvent.value = UIEvent.ProgressBar(false)
            _uiEvent.value = UIEvent.SnackBar(
                "Couldn't fetch your profile. Please logout and log back in again to continue",
                true
            )
        }
    }

    fun renewSubWithWallet() = viewModelScope.launch {
        _uiUpdate.value = UiUpdate.CreateStatusDialog("wallet", null)

        selectedOrder?.let {
            fetchWallet()?.let { wallet ->
                if (wallet.amount < it.price) {
                    _uiUpdate.value = UiUpdate.DismissStatusDialog(true)
                    _uiEvent.value = UIEvent.SnackBar("Insufficient Balance in Wallet", true)
                    return@launch
                }
            } ?: let {
                _uiUpdate.value = UiUpdate.DismissStatusDialog(true)
                _uiEvent.value =
                    UIEvent.SnackBar("Failed to fetch Wallet. Try different payment method", true)
                return@launch
            }
            foodSubscriptionUseCase
                .renewSubWithWallet(getRenewedSubDetails(it))
                .onEach { result ->
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is NetworkResult.Success -> {
                                when (result.message) {
                                    "transaction" -> _uiUpdate.value =
                                        UiUpdate.ValidatingTransaction(
                                            "Making payment from wallet... ",
                                            "transaction"
                                        )
                                    "validating" -> _uiUpdate.value = UiUpdate.PlacingOrder(
                                        "Validating Transaction...",
                                        "validating"
                                    )
                                    "placed" -> {
                                        _uiUpdate.value = UiUpdate.PlacedOrder(
                                            "Subscription Renewed Successfully!",
                                            "success"
                                        )
                                        delay(1800)
                                        _uiUpdate.value = UiUpdate.DismissStatusDialog(true)
                                        sendPushNotification(it.customerID)
                                    }
                                }
                            }
                            is NetworkResult.Failed -> {
                                _uiUpdate.value =
                                    UiUpdate.UpdateStatusDialog(result.message, "fail")
                                delay(1000)
                                _uiUpdate.value = UiUpdate.DismissStatusDialog(false)
                            }
                            else -> Unit
                        }
                    }
                }.launchIn(this)
        } ?: let {
            _uiUpdate.value = UiUpdate.DismissStatusDialog(true)
            _uiEvent.value = UIEvent.SnackBar(
                "Failed to fetch your order details. Refresh the page and try again",
                true
            )
        }
    }

    fun renewSubWithOnlinePayment(transactionID: String) = viewModelScope.launch {

        _uiUpdate.value = UiUpdate.CreateStatusDialog(null, null)

        selectedOrder?.let {
            foodSubscriptionUseCase
                .renewSubscriptionWithOnline(
                    it,
                    transactionID
                ).onEach { result ->
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is NetworkResult.Success -> {
                                when (result.message) {
                                    "placing" -> _uiUpdate.value = UiUpdate.PlacingOrder(
                                        "Validating Transaction...",
                                        "validating"
                                    )
                                    "placed" -> {
                                        _uiUpdate.value = UiUpdate.PlacedOrder(
                                            "Subscription Renewed Successfully!",
                                            "success"
                                        )
                                        delay(1800)
                                        _uiUpdate.value = UiUpdate.DismissStatusDialog(true)
                                        sendPushNotification(it.customerID)
                                    }
                                }
                            }
                            is NetworkResult.Failed -> {
                                _uiUpdate.value =
                                    UiUpdate.UpdateStatusDialog(result.message, "fail")
                                delay(1800)
                                _uiUpdate.value = UiUpdate.DismissStatusDialog(false)
                            }
                            else -> Unit
                        }
                    }
                }.launchIn(this)
        } ?: let {
            _uiUpdate.value = UiUpdate.DismissStatusDialog(true)
            _uiEvent.value = UIEvent.SnackBar(
                "Failed to fetch your order details. Refresh the page and try again",
                true
            )
        }
    }

    private fun getRenewedSubDetails(order: AmmaSpecialOrder): AmmaSpecialOrder {
        order.deliveryDates.clear()
        selectedEventDates.forEach {
            order.deliveryDates.add(TimeUtil().getCustomDate(dateLong = it))
        }
        order.endDate = selectedEventDates.max()
        return order
    }

    private suspend fun sendPushNotification(customerID: String) {
        PushNotificationUseCase(fbRepository).sendPushNotification(
            customerID,
            "Amma's Special Subscription Created!",
            "Your Amma's Special Subscription has been created successfully. You can check the progress of your purchase in Subscription History Page.",
            "amma"
        )
    }

    fun getFoodStatus(date: Long) = viewModelScope.launch {
        if (ammaSpecialOrders.isNotEmpty()) {
            _uiUpdate.value =
                if (orderStatusMap[SimpleDateFormat("dd-MM-yyyy").format(date)].isNullOrEmpty()) {
                    val status = foodSubscriptionUseCase.getFoodStatus(
                        date,
                        ammaSpecialOrders.filter { it.endDate >= date })
                    orderStatusMap[SimpleDateFormat("dd-MM-yyyy").format(date)] =
                        status
                    UiUpdate.UpdateFoodDeliveryStatus(status, true)
                } else {
                    UiUpdate.UpdateFoodDeliveryStatus(
                        orderStatusMap[SimpleDateFormat("dd-MM-yyyy").format(
                            date
                        )], true
                    )
                }
        } else {
            _uiUpdate.value = UiUpdate.UpdateFoodDeliveryStatus(null, false)
        }
    }

    fun getAmmaSpecialsOrderDetails(date: Long) = viewModelScope.launch {
        if (userID.isNullOrEmpty()) {
            _uiEvent.value = UIEvent.ProgressBar(false)
            _uiEvent.value = UIEvent.SnackBar(
                "Failed to fetch user profile. Please Log out and Log back in to continue",
                true
            )
            return@launch
        } else {
            foodSubscriptionUseCase.getAmmaSpecialOrders(userID!!)?.let { orders ->
                ammaSpecialOrders.clear()
                ammaSpecialOrders.addAll(orders.filter { it.endDate >= System.currentTimeMillis() })
                getFoodStatus(date)
            } ?: let {
                _uiUpdate.value = UiUpdate.UpdateFoodDeliveryStatus(null, false)
            }
        }
    }

    fun cancelDeliveryOn(date: Long, refund: String) = viewModelScope.launch {
        selectedOrder?.let {
            val status = foodSubscriptionUseCase
                .cancelDeliveryOn(date, it, refund)
            _uiUpdate.value = if (status) {
                UiUpdate.CancelOrderStatus(status, "Delivery cancelled")
            } else {
                UiUpdate.CancelOrderStatus(
                    status,
                    "Failed to cancel delivery. Reload page and try again"
                )
            }
        } ?: let {
            _uiUpdate.value = UiUpdate.CancelOrderStatus(
                false,
                "Failed to fetch order details. Reload page and try again"
            )
        }

    }

    fun cancelSubscription() = viewModelScope.launch {
        selectedOrder?.let {
            val status = foodSubscriptionUseCase.cancelSubscription(it)
            _uiUpdate.value = if (status) {
                UiUpdate.CancelSubscription(status, "Subscription Cancelled.")
            } else {
                UiUpdate.CancelSubscription(
                    status,
                    "Subscription Cancellation Failed. Please contact support for further assistance"
                )
            }
        } ?: let {
            _uiUpdate.value = UiUpdate.CancelSubscription(
                false,
                "Subscription Cancellation Failed. Please contact support for further assistance"
            )
        }
    }

    fun getNonDeliveryDays() = viewModelScope.launch {
        nonDeliveryDatesLong?.let {
            _uiUpdate.value = UiUpdate.PopulateNonDeliveryDates(it)
        } ?:let {
            foodSubscriptionUseCase.getNonDeliveryDays().let { dates ->
                nonDeliveryDatesLong = mutableListOf()
                nonDeliveryDatesLong!!.clear()
                dates?.let { it -> nonDeliveryDatesLong!!.addAll(it) }
                nonDeliveryDatesString.addAll(nonDeliveryDatesLong!!.map { TimeUtil().getCustomDate(dateLong = it) })
                _uiUpdate.value = UiUpdate.PopulateNonDeliveryDates(nonDeliveryDatesLong)
            }
        }
    }


    sealed class UiUpdate {
        data class PopulateAmmaSpecials(
            val ammaSpecials: List<AmmaSpecial>?,
            val banners: List<Banner>?
        ) : UiUpdate()

        data class PopulateUserProfile(val userProfile: UserProfileEntity) : UiUpdate()
        data class PopulateNonDeliveryDates(val dates: List<Long>?): UiUpdate()

        //placing order
        data class PlacingOrder(val message: String, val data: String) : UiUpdate()
        data class PlacedOrder(val message: String, val data: String) : UiUpdate()
        data class ValidatingTransaction(val message: String, val data: String) : UiUpdate()

        //        status
        data class CreateStatusDialog(val message: String?, val data: String?) : UiUpdate()
        data class DismissStatusDialog(val dismiss: Boolean) : UiUpdate()
        data class UpdateStatusDialog(val message: String, val data: String) : UiUpdate()

        // cancel status
        data class CancelOrderStatus(val status: Boolean, val message: String) : UiUpdate()
        data class CancelSubscription(val status: Boolean, val message: String) : UiUpdate()
        data class RenewSubscription(val status: Boolean, val message: String) : UiUpdate()

        //food delivery
        data class UpdateFoodDeliveryStatus(val status: HashMap<String, String>?, val isOrdersAvailable: Boolean) : UiUpdate()
        object Empty : UiUpdate()
    }
}
