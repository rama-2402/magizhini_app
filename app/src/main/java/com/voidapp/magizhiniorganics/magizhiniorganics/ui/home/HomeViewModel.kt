package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.BannerEntity
import com.voidapp.magizhiniorganics.magizhiniorganics.data.entities.ProductEntity
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

    val specialsTitles: MutableList<String> = mutableListOf()
    val bestSellersList: MutableList<List<ProductEntity>> = mutableListOf()
    val bannersList: MutableList<BannerEntity> = mutableListOf()
//    val bannersList: MutableList<List<BannerEntity>> = mutableListOf()

    val partners: MutableList<Partners> = mutableListOf()
    val testimonials: MutableList<TestimonialsEntity> = mutableListOf()

    private val _uiUpdate: MutableLiveData<UiUpdate> = MutableLiveData()
    val uiUpdate: LiveData<UiUpdate> = _uiUpdate

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

        getBestSellers.await()
        getSpecialsOne.await()
        getSpecialsTwo.await()
        getSpecialsThree.await()
        getSpecialBanners.await()

        withContext(Dispatchers.Main) {
            _uiUpdate.value = UiUpdate.PopulateData
        }

    }

    private suspend fun getSpecialBanners(): Boolean = withContext(Dispatchers.IO){
        try {
            dbRepository.getSpecialBanners().let { it ->
                bannersList.clear()
                for (i in it) {
                    bannersList.add(i.toBannerEntity())
                }
//                for (i in it.indices step 3) {
//                    val list: MutableList<BannerEntity> = mutableListOf<BannerEntity>()
//                    list.add(it[i].toBannerEntity())
//                    list.add(it[i+1].toBannerEntity())
//                    list.add(it[i+2].toBannerEntity())
//                    bannersList.add(list)
//                }
            }
            true
        } catch (e: Exception) {
            e.message?.let { fbRepository.logCrash("Home: populating spl banners from db", it) }
            false
        }
    }

    private suspend fun getBestSellers() = withContext(Dispatchers.IO) {
        try {
            val bestSeller = dbRepository.getBestSellers()
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(bestSeller.id)
            for (item in items.indices) {
                dbRepository.getProductWithIdForUpdate(items[item])?.let { products.add(it) }
            }
            bestSellersList.add(products)
            specialsTitles.add(bestSeller.name)
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating best sellers from db", it) }
        }
    }
    private suspend fun getSpecialsOne() = withContext(Dispatchers.IO) {
        try {
            val one = dbRepository.getSpecialsOne()
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(one.id)
            for (item in items.indices) {
                dbRepository.getProductWithIdForUpdate(items[item])?.let{products.add(it)}
            }
            bestSellersList.add(products)
            specialsTitles.add(one.name)
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating spl one from db", it) }
        }
    }
    private suspend fun getSpecialsTwo() = withContext(Dispatchers.IO) {
        try {
            val two = dbRepository.getSpecialsTwo()
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(two.id)
            for (item in items.indices) {
                dbRepository.getProductWithIdForUpdate(items[item])?.let{products.add(it)}
            }
            bestSellersList.add(products)
            specialsTitles.add(two.name)
        } catch (e: IOException) {
            e.message?.let { fbRepository.logCrash("Home: populating spl two from db", it) }
        }
    }
    private suspend fun getSpecialsThree() = withContext(Dispatchers.IO) {
        try {
            val three = dbRepository.getSpecialsThree()
            val items = arrayListOf<String>()
            val products = arrayListOf<ProductEntity>()
            items.addAll(three.id)
            for (item in items.indices) {
                dbRepository.getProductWithIdForUpdate(items[item])?.let{products.add(it)}
            }
            bestSellersList.add(products)
            specialsTitles.add(three.name)
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
                UiUpdate.ReferralStatus(referralStatus, code,  null)
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
        data class ReferralStatus(val status: Boolean, val referralCode: String, val message: String?): UiUpdate()

        object Empty : HomeViewModel.UiUpdate()
    }
}

