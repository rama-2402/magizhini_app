package com.voidapp.magizhiniorganics.magizhiniorganics.ui.foodSubscription

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.useCase.FoodSubscriptionUseCase
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.UserProfileEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.AmmaSpecial
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Banner
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.UserProfile
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.callbacks.UIEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val ammaSpecials: MutableList<AmmaSpecial> = mutableListOf()
    var lunchMap: HashMap<String, Double> = hashMapOf()

    val selectedEventDates: MutableList<Long> = mutableListOf()
    var currentSubOption: String = "month"
    var currentCountOption : Int = 0

    fun setEmptyUiEvent() {
        _uiEvent.value = UIEvent.EmptyUIEvent
    }

    fun setEmptyStatus() {
        _uiUpdate.value = UiUpdate.Empty
    }

    fun getAmmaSpecials() = viewModelScope.launch {
//        val specials = foodSubscriptionUseCase.getAllAmmaSpecials()
//        val banners = foodSubscriptionUseCase.getAllBanners()
//        _uiUpdate.value = UiUpdate.PopulateAmmaSpecials(specials, banners)
        val specials = generateSampleSpecials()
        ammaSpecials.clear()
        ammaSpecials.addAll(specials)
        specials.forEach {
            lunchMap[it.foodDay] = if (it.discountedPrice == 0.0) {
                it.price
            } else {
                it.discountedPrice
            }
            Log.e("qqq", "map: ${lunchMap["Monday"]}" )
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

    sealed class UiUpdate {
        data class PopulateAmmaSpecials(val ammaSpecials: List<AmmaSpecial>?, val banners: List<Banner>?): UiUpdate()
        data class PopulateUserProfile(val userProfile: UserProfileEntity): UiUpdate()

        object Empty: UiUpdate()
    }
}