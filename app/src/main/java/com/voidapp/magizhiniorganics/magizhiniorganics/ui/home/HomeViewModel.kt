package com.voidapp.magizhiniorganics.magizhiniorganics.ui.home

import androidx.lifecycle.*
import com.voidapp.magizhiniorganics.magizhiniorganics.Firestore.FirestoreRepository
import com.voidapp.magizhiniorganics.magizhiniorganics.data.dao.DatabaseRepository

class HomeViewModel (
    private val repository: DatabaseRepository,
    private val firestoreRepository: FirestoreRepository
): ViewModel() {

    var homeListener: HomeListener? = null

    fun getAllBanners() = repository.getAllBanners()

    fun getALlCategories() = repository.getAllProductCategories()

    fun selectedCategory(category: String) {
        homeListener?.displaySelectedCategory(category)
    }

    fun signOut() {
        firestoreRepository.signOut()
    }

    fun onDataTransactionFailure(message: String){
        homeListener?.onDataTransactionFailure(message)
    }
}