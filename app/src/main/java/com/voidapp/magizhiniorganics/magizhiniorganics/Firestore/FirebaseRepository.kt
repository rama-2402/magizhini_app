package com.voidapp.magizhiniorganics.magizhiniorganics.Firestoreimport com.voidapp.magizhiniorganics.magizhiniorganics.data.models.CustomerProfileclass FirebaseRepository (    private val firebase: Firebase) {    suspend fun createNewCustomerProfile() = firebase.createNewCustomerProfile()    suspend fun uploadUserProfile(profile: CustomerProfile):Boolean = firebase.uploadUserProfile(profile)}