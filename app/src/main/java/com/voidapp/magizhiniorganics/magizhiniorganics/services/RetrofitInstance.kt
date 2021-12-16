package com.voidapp.magizhiniorganics.magizhiniorganics.services

import com.voidapp.magizhiniorganics.magizhiniorganics.ui.notification.NotificationAPI
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.BASE_URL
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitInstance {

    companion object {
        private val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        val api by lazy {
            retrofit.create(NotificationAPI::class.java)
        }
    }
}