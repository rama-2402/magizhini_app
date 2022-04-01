package com.voidapp.magizhiniorganics.magizhiniorganics.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

object NetworkHelper {

    fun isNetworkConnected(context: Context): Boolean {

        var result = false
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).apply {
             result = checkNetworkConnection(this, this.activeNetwork)
        }

        return result
    }

    private fun checkNetworkConnection(connectivityManager: ConnectivityManager, network: Network?): Boolean {

        connectivityManager.getNetworkCapabilities(network)?.also {
            if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true
            } else {
                if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return true
                }
            }
        }
            return false
    }

    //checking the network connection before proceeding
    fun isOnline(context: Context):Boolean {
        return NetworkHelper.isNetworkConnected(context)
    }


}