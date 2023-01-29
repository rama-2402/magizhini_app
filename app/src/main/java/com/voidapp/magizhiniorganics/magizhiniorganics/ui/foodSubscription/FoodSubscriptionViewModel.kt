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
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.CouponEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.*
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.TimeUtil
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.NetworkResult
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
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
    var tempAddress: Address? = null

    var ammaSpecials: MutableList<MenuImage> = mutableListOf()
//    val budgetPlanRecipes: MutableList<AmmaSpecial> = mutableListOf()
//    val premiumPlanRecipes: MutableList<AmmaSpecial> = mutableListOf()

    var nonDeliveryDatesLong: MutableList<Long>? = null
    var nonDeliveryDatesString: MutableList<String> = mutableListOf()
    val selectedEventDates: MutableList<Long> = mutableListOf()
    var currentSubOption: String = "single"
    var deliveryCharge: Double = 0.0

    //    var currentCountOption: Int = 0
//    var currentServingOption: Int = 0
    var totalPrice: Double = 0.0

    var couponPrice: Double? = null
    var currentCoupon: CouponEntity? = null

    //the outer hashmap holds the data as key and the orderids as values
    //the inner hashmap has orderid as key and the status as value
    var orderStatusMap: HashMap<String, HashMap<String, String>?> = hashMapOf()
    var userID: String? = null
    var selectedOrder: AmmaSpecialOrder? = null

    fun setEmptyUiEvent() {
        _uiEvent.value = UIEvent.EmptyUIEvent
    }

    fun setEmptyStatus() {
        _uiUpdate.value = UiUpdate.Empty
    }

    fun getAmmaSpecials() = viewModelScope.launch(Dispatchers.IO) {
        val specials = foodSubscriptionUseCase.getAllAmmaSpecials()
        val banners = foodSubscriptionUseCase.getAllBanners()
//        val specials = generateSampleSpecials()
        ammaSpecials.clear()
//        budgetPlanRecipes.clear()
//        premiumPlanRecipes.clear()
        specials?.let { specialsList ->
            ammaSpecials.addAll(specialsList.sortedBy { it.displayOrder })
//            specialsList.forEach {
//                lunchMap[it.foodDay] = if (it.discountedPrice == 0.0) {
//                    it.price
//                } else {
//                    it.discountedPrice
//                }

//                if (it.plan == "budget") {
//                    budgetPlanRecipes.add(it)
//                } else {
//                    premiumPlanRecipes.add(it)
//                }
//            }
        }
        withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.PopulateAmmaSpecials(specials, banners)
        }
//        _uiUpdate.value = UiUpdate.PopulateAmmaSpecials(specials, null)
    }

    //order activity
    fun getProfile() = viewModelScope.launch(Dispatchers.IO) {
        try {
         dbRepository.getProfileData()?.let { profile ->
            withContext(Dispatchers.Main) {
                userProfile = profile
                _uiUpdate.value = UiUpdate.PopulateUserProfile(profile)
            }
        } ?: withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.PopulateUserProfile(null)
        }
        } catch (e: Exception) {
             withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.PopulateUserProfile(null)
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
                price = totalPrice + deliveryCharge,
//                plan = selectedPlan,
                userName = orderDetailsMap["name"].toString(),
                addressOne = orderDetailsMap["one"].toString(),
                addressTwo = orderDetailsMap["two"].toString(),
                city = orderDetailsMap["city"].toString(),
                code = orderDetailsMap["code"].toString(),
                phoneNumber = orderDetailsMap["phoneNumber"].toString(),
                mailID = orderDetailsMap["mailID"].toString(),
                orderFoodTime = orderDetailsMap["orders"] as ArrayList<String>,
                leafNeeded = orderDetailsMap["leaf"].toString().toInt(),
                orderType = currentSubOption,
                orderCount = 0,
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
                price = totalPrice + deliveryCharge,
//                plan = selectedPlan,
                userName = orderDetailsMap["name"].toString(),
                addressOne = orderDetailsMap["one"].toString(),
                addressTwo = orderDetailsMap["two"].toString(),
                city = orderDetailsMap["city"].toString(),
                code = orderDetailsMap["code"].toString(),
                phoneNumber = orderDetailsMap["phoneNumber"].toString(),
                mailID = orderDetailsMap["mailID"].toString(),
                orderFoodTime = orderDetailsMap["orders"] as ArrayList<String>,
                leafNeeded = orderDetailsMap["leaf"].toString().toInt(),
                orderType = currentSubOption,
                orderCount = 0,
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

            val deliveryDatesString = mutableListOf<String>()
            selectedEventDates.forEach {
                deliveryDatesString.add(TimeUtil().getCustomDate(dateLong = it))
            }

            it.deliveryDates.addAll(deliveryDatesString)

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

    fun placeOrderCOD(orderDetailsMap: HashMap<String, Any>) = viewModelScope.launch {
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
                price = totalPrice + deliveryCharge,
//                plan = selectedPlan,
                userName = orderDetailsMap["name"].toString(),
                addressOne = orderDetailsMap["one"].toString(),
                addressTwo = orderDetailsMap["two"].toString(),
                city = orderDetailsMap["city"].toString(),
                code = orderDetailsMap["code"].toString(),
                phoneNumber = orderDetailsMap["phoneNumber"].toString(),
                mailID = orderDetailsMap["mailID"].toString(),
                orderFoodTime = orderDetailsMap["orders"] as ArrayList<String>,
                leafNeeded = orderDetailsMap["leaf"].toString().toInt(),
                orderType = currentSubOption,
                orderCount = 0,
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
                                    "validating" -> _uiUpdate.value = UiUpdate.PlacingOrder(
                                        "Validating Purchase...",
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

    fun renewSubWithOnlinePayment(transactionID: String) = viewModelScope.launch {

        _uiUpdate.value = UiUpdate.CreateStatusDialog(null, null)

        selectedOrder?.let {

            val deliveryDatesString = mutableListOf<String>()
            selectedEventDates.forEach {
                deliveryDatesString.add(TimeUtil().getCustomDate(dateLong = it))
            }

            it.deliveryDates.addAll(deliveryDatesString)

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
                        ammaSpecialOrders.filter { it.endDate >= date }
                    )
                    if (!status.isNullOrEmpty()) {
                        orderStatusMap[SimpleDateFormat("dd-MM-yyyy").format(date)] =
                            status
                        UiUpdate.UpdateFoodDeliveryStatus(status, true)
                    } else {
                        UiUpdate.UpdateFoodDeliveryStatus(null, false)
                    }
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
//            _uiEvent.value = UIEvent.SnackBar(
//                "Failed to fetch user profile. Please Log out and Log back in to continue",
//                true
//            )
            _uiUpdate.value = UiUpdate.UpdateFoodDeliveryStatus(
                null,
                true
            ) //this case is when user not logged in but has opted for food
            return@launch
        } else {
            foodSubscriptionUseCase.getAmmaSpecialOrders(userID!!)?.let { orders ->
                ammaSpecialOrders.clear()
//                ammaSpecialOrders.addAll(orders.filter { it.endDate >= System.currentTimeMillis() })
                ammaSpecialOrders.addAll(orders)
                _uiUpdate.value = UiUpdate.UpdateFoodDeliveryStatus(null, true)
//                getFoodStatus(date)
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

    //Coupons
    fun verifyCoupon(couponCode: String, totalPrice: Double) =
        viewModelScope.launch(Dispatchers.IO) {
            if (couponCode == "") {
                return@launch
            } else {
                val code: CouponEntity? = currentCoupon?.let {
                    it
                } ?: dbRepository.getCouponByCode(couponCode)
                code?.let { coupon ->
                    if (!coupon.categories.contains(Constants.AMMASPECIAL)) {
                        withContext(Dispatchers.Main) {
                            _uiEvent.value =
                                UIEvent.Toast("Coupon Applies only for product purchases")
                        }
                        return@launch
                    }
                    if (totalPrice >= coupon.purchaseLimit) {
                        if (couponPrice == null) {
                            withContext(Dispatchers.Main) {
                                currentCoupon = coupon
                                couponPrice = couponDiscount(coupon, totalPrice)
                                _uiUpdate.value = UiUpdate.CouponApplied(
                                    "Coupon Applied Successfully!"
                                )
                            }
                        }
//                        withContext(Dispatchers.Main) {
//                        }
//                    couponAppliedPrice = cartPrice - couponDiscount(coupon, cartPrice)
                    } else {
                        withContext(Dispatchers.Main) {
                            _uiEvent.value =
                                UIEvent.Toast("Coupon Applies only for Purchase more than Rs: ${coupon.purchaseLimit}")
                            _uiUpdate.value = UiUpdate.CouponApplied("")
                        }
                        return@launch
                    }
                } ?: withContext(Dispatchers.Main) {
                    _uiEvent.value = UIEvent.Toast("Coupon Code does not exist.")
                    _uiUpdate.value = UiUpdate.CouponApplied("")
                }
            }
        }

    fun couponDiscount(coupon: CouponEntity, cartPrice: Double): Double {
        var discountPrice: Double = when (coupon.type) {
            "percent" -> (cartPrice * coupon.amount / 100)
            "rupees" -> coupon.amount.toDouble()
            else -> 0.0
        }

        if (discountPrice > coupon.maxDiscount) {
            discountPrice = coupon.maxDiscount.toDouble()
        }

        return discountPrice
    }

    fun getNonDeliveryDays() = viewModelScope.launch {
        nonDeliveryDatesLong?.let {
            _uiUpdate.value = UiUpdate.PopulateNonDeliveryDates(it)
        } ?: let {
            foodSubscriptionUseCase.getNonDeliveryDays().let { dates ->
                nonDeliveryDatesLong = mutableListOf()
                nonDeliveryDatesLong!!.clear()
                dates?.let { it -> nonDeliveryDatesLong!!.addAll(it) }
                nonDeliveryDatesString.addAll(nonDeliveryDatesLong!!.map {
                    TimeUtil().getCustomDate(
                        dateLong = it
                    )
                })
                _uiUpdate.value = UiUpdate.PopulateNonDeliveryDates(nonDeliveryDatesLong)
            }
        }
    }

    suspend fun getDeliveryCharge(code: String): Double = withContext(Dispatchers.IO) {
    return@withContext try {
                dbRepository.getDeliveryCharge(code)?.let { pinCodes ->
                    if (pinCodes.isEmpty()) {
                        //                        deliveryAvailability(null)
                        30.0
                    } else {
                        //                        deliveryAvailability(pinCodes[0])
                        pinCodes[0].deliveryCharge.toDouble()
                    }
                } ?: let {
                    //                    deliveryAvailability(null)
                    30.0
                }
        } catch (E: IOException) {
            30.0
        }
    }


    sealed class UiUpdate {
        data class PopulateAmmaSpecials(
            val ammaSpecials: List<MenuImage>?,
            val banners: List<Banner>?
        ) : UiUpdate()

        data class PopulateUserProfile(val userProfile: UserProfileEntity?) : UiUpdate()
        data class PopulateNonDeliveryDates(val dates: List<Long>?) : UiUpdate()

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

        //coupon
        data class CouponApplied(val message: String) : UiUpdate()

        //food delivery
        data class UpdateFoodDeliveryStatus(
            val status: HashMap<String, String>?,
            val isOrdersAvailable: Boolean
        ) : UiUpdate()

        object Empty : UiUpdate()
    }
}
