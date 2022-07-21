package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.app.Activity
import com.razorpay.Checkout
import org.json.JSONObject

fun startPayment(
    activity: Activity,
    mailID: String,
    price: Float,
    name: String,
    userID: String,
    phoneNumber: String
): Boolean {
    /*
    *  You need to pass current activity in order to let Razorpay create CheckoutActivity
    * */
    val co = Checkout()
    val email = if (mailID.isEmpty()) "magizhiniorganics2018@gmail.com" else mailID

    return try {
        val options = JSONObject()
        options.put("name",name)
        options.put("description","Purchasing from store for $userID")
        //You can omit the image option to fetch the image from dashboard
        options.put("image","https://firebasestorage.googleapis.com/v0/b/magizhiniorganics-56636.appspot.com/o/icon_sh_4.png?alt=media&token=71cf0e67-2f00-4a0f-8950-15459ee02137")
        options.put("theme.color", "#2B6A71")
        options.put("currency","INR")
//            options.put("order_id", "orderIDkjhasgdfkjahsdf");
        options.put("amount",price)//pass amount in currency subunits

//            val retryObj = JSONObject();
//            retryObj.put("enabled", true);
//            retryObj.put("max_count", 4);
//            options.put("retry", retryObj);

        val prefill = JSONObject()
        prefill.put("email",email)  //this place should have customer name
        prefill.put("contact", phoneNumber)     //this place should have customer phone number

        options.put("prefill",prefill)
        co.open(activity,options)
        true
    }catch (e: Exception){
        e.printStackTrace()
        false
    }
}