package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.*
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.BirthdayCard
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.Partners
import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.TestimonialsEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.toBannerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class HomeViewModel (
    private val fbRepository: FirestoreRepository,
    private val dbRepository: DatabaseRepository
): ViewModel() {
//
//    private var _referralStatus: MutableLiveData<Boolean> = MutableLiveData()
//    val referralStatus: LiveData<Boolean> = _referralStatus
//    private var _allowReferral: MutableLiveData<String> = MutableLiveData()
//    val allowReferral: LiveData<String> = _allowReferral
//    private var _showBirthday: MutableLiveData<BirthdayCard?> = MutableLiveData()
//    val showBirthday: LiveData<BirthdayCard?> = _showBirthday
//    private var _specialsOne: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
//    val specialsOne: LiveData<MutableList<ProductEntity>> = _specialsOne
//    private var _specialsTwo: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
//    val specialsTwo: LiveData<MutableList<ProductEntity>> = _specialsTwo
//    private var _specialsThree: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
//    val specialsThree: LiveData<MutableList<ProductEntity>> = _specialsThree
//    private var _bestSellers: MutableLiveData<MutableList<ProductEntity>> = MutableLiveData()
//    val bestSellers: LiveData<MutableList<ProductEntity>> = _bestSellers
//    private var _testimonials: MutableLiveData<MutableList<TestimonialsEntity>> = MutableLiveData()
//    val testimonials: LiveData<MutableList<TestimonialsEntity>> = _testimonials
//    private var _recyclerPosition: MutableLiveData<Int> = MutableLiveData()
//    val recyclerPosition: LiveData<Int> = _recyclerPosition
//    private var _specialBanners: MutableLiveData<List<SpecialBanners>> = MutableLiveData()
//    val specialBanners: LiveData<List<SpecialBanners>> = _specialBanners
//    private var _notifications: MutableLiveData<List<UserNotificationEntity>?> = MutableLiveData()
//    val notifications: LiveData<List<UserNotificationEntity>?> = _notifications
//    private var _partners: MutableLiveData<List<Partners>?> = MutableLiveData()
//    val partners: LiveData<List<Partners>?> = _partners
//
//    val bannersList: MutableList<SpecialBanners> = mutableListOf()
//
//    var recyclerToRefresh = ""
//
//    var bestSellerHeader = ""
//    var specialsOneHeader = ""
//    var specialsTwoHeader = ""
//    var specialsThreeHeader = ""

    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate

    val specialsProductsList: MutableList<List<ProductEntity>> = mutableListOf()
    val specialsTitles: MutableList<String> = mutableListOf()
    val specialBannersList: MutableList<List<BannerEntity>> = mutableListOf()
    val partners: MutableList<Partners> = mutableListOf()
    val testimonials: MutableList<TestimonialsEntity> = mutableListOf()

    fun setEmptyStatus() {
        _uiUpdate.value = UiUpdate.Empty
    }

    fun getAllBanners() = dbRepository.getAllBanners()

    fun getALlCategories() = dbRepository.getAllProductCategories()

    fun signOut() {
        fbRepository.signOut()
    }

    fun getDataToPopulate() = viewModelScope.launch(Dispatchers.IO) {
        val getBestSellers = async { getBestSellers() }
        val getSpecialsOne = async { getSpecialsOne() }
        val getSpecialsTwo = async { getSpecialsTwo() }
        val getSpecialsThree = async { getSpecialsThree() }
        val getSpecialBanners = async { getSpecialBanners() }
//        val getAllTestimonials = async { getAllTestimonials() }

        getBestSellers.await()
        getSpecialsOne.await()
        getSpecialsTwo.await()
        getSpecialsThree.await()
        getSpecialBanners.await()
//        getAllTestimonials.await()

        withContext(Dispatchers.Main) {
           _uiUpdate.value = UiUpdate.PopulateData
        }
    }

    private suspend fun getSpecialBanners() {
        try {
            dbRepository.getSpecialBanners().let {
                for (i in it.indices step 3) {
                    val list: MutableList<BannerEntity> = mutableListOf<BannerEntity>()
                    list.add(it[i].toBannerEntity())
                    list.add(it[i+1].toBannerEntity())
                    list.add(it[i+2].toBannerEntity())
                    specialBannersList.add(list)
                }
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating spl banners from db", it) }
        }
    }

    private suspend fun getBestSellers() {
        try {
            val bestSeller = dbRepository.getBestSellers()
//            bestSellerHeader = bestSeller.name
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(bestSeller.id)
            for (item in items.indices) {
                dbRepository.getProductWithIdForUpdate(items[item])?.let { products.add(it) }
            }
            withContext(Dispatchers.Main) {
                specialsTitles.add(bestSeller.name)
                specialsProductsList.add(products)
//                _bestSellers.value = products
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating best sellers from db", it) }
        }
    }
    private suspend fun getSpecialsOne() {
        try {
            val one = dbRepository.getSpecialsOne()
//            specialsOneHeader = one.name
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(one.id)
            for (item in items.indices) {
                dbRepository.getProductWithIdForUpdate(items[item])?.let{products.add(it)}
            }
            withContext(Dispatchers.Main) {
                specialsTitles.add(one.name)
                specialsProductsList.add(products)
//                _specialsOne.value = products
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating spl one from db", it) }
        }
    }
    private suspend fun getSpecialsTwo() {
        try {
            val two = dbRepository.getSpecialsTwo()
//            specialsTwoHeader = two.name
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(two.id)
            for (item in items.indices) {
                dbRepository.getProductWithIdForUpdate(items[item])?.let{products.add(it)}
            }
            withContext(Dispatchers.Main) {
                specialsTitles.add(two.name)
                specialsProductsList.add(products)
//                _specialsTwo.value = products
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating spl two from db", it) }
        }
    }
    private suspend fun getSpecialsThree() {
        try {
            val three = dbRepository.getSpecialsThree()
//            specialsThreeHeader = three.name
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(three.id)
            for (item in items.indices) {
                dbRepository.getProductWithIdForUpdate(items[item])?.let{products.add(it)}
            }
            withContext(Dispatchers.Main) {
                specialsTitles.add(three.name)
                specialsProductsList.add(products)
//                _specialsThree.value = products
            }
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating spl three from db", it) }
        }
    }

    fun updateToken(token: String?)= viewModelScope.launch(Dispatchers.IO) {
        token?.let {
            val birthdayCard: BirthdayCard? = fbRepository.updateToken(it)
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.ShowBirthDayCard(birthdayCard)
            }
        }
    }

    suspend fun getProductByID(id: String): ProductEntity? = withContext(Dispatchers.IO) {
        return@withContext dbRepository.getProductWithIdForUpdate(id)
    }

    suspend fun getCategoryByID(id: String): String = withContext(Dispatchers.IO) {
        dbRepository.getCategoryByID(id)?.let { return@withContext it } ?: "Dairy Products"
    }

    fun getAllNotifications() = viewModelScope.launch(Dispatchers.IO) {
        dbRepository.getAllNotifications()?.let { it ->
            withContext(Dispatchers.Main) {
                _uiUpdate.value = UiUpdate.ShowNotificationsCount(it.size, null)
            }
        }
    }

    fun applyReferralNumber(currentUserID: String, code: String) = viewModelScope.launch {
        val referralStatus = fbRepository.applyReferralNumber(currentUserID, code, true)
        withContext(Dispatchers.Main) {
            _uiUpdate.value =
                UiUpdate.ReferralStatus(referralStatus, null)
        }
    }

    fun checkForReferral() = viewModelScope.launch(Dispatchers.IO) {
        dbRepository.getProfileData()?.let { profile ->
            withContext(Dispatchers.Main) {
                if (profile.referralId.isNullOrEmpty()) {
                    _uiUpdate.value = UiUpdate.AllowReferral(true, profile.id)
                } else {
                    _uiUpdate.value = UiUpdate.AllowReferral(
                        false,
                        "You have already claimed Magizhini Referral Bonus"
                    )
                }
            }
        } ?: withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.AllowReferral(
                false,
                "User Profile Data not available. Please Log In again"
            )
        }
    }

    fun updateBirthdayCard(customerID: String) = viewModelScope.launch(Dispatchers.IO) {
        fbRepository.updateBirthdayCard(customerID)
    }

    fun getEndData() = viewModelScope.launch(Dispatchers.IO) {
        val partnersLocal =
            withContext(Dispatchers.Default) { fbRepository.getAllPartners() }
        val testimonialsLocal =
            withContext(Dispatchers.Default) { dbRepository.getAllTestimonials() }

        withContext(Dispatchers.Main) {
            partnersLocal?.let { partners.addAll(it) }
            testimonialsLocal?.let { testimonials.addAll(it) }
            _uiUpdate.value = UiUpdate.PopulateEnd(testimonials, partners)
        }
    }

    sealed class UiUpdate {
        //populating data
        object PopulateData: UiUpdate()

        //Notifications
        data class ShowNotificationsCount(val count: Int?, val message: String?): UiUpdate()
        //Testimonials
        data class PopulateEnd(val testimonials: List<TestimonialsEntity>, val partners: List<Partners>?): UiUpdate()
       //Birthday Card
        data class ShowBirthDayCard(val birthdayCard: BirthdayCard?): UiUpdate()
        //Referral
        data class AllowReferral(val status: Boolean, val message: String?): UiUpdate()
        data class ReferralStatus(val status: Boolean, val message: String?): UiUpdate()

        object Empty : HomeViewModel.UiUpdate()
    }
}

