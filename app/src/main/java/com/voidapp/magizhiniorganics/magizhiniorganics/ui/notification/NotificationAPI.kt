package com.voidapp.magizhiniorganics.magizhiniorganics.ui.notification

import com.voidapp.magizhiniorganics.magizhiniorganics.data.models.PushNotification
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.CONTENT_TYPE
import com.voidapp.magizhiniorganics.magizhiniorganics.utils.Constants.SERVER_KEY
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface NotificationAPI {

    @Headers("Authorization: key=$SERVER_KEY", "Content-Type:$CONTENT_TYPE")
    @POST("fcm/send")
    suspend fun postNotification(
        @Body notification: PushNotification
    ): Response<ResponseBody>
}