package com.bpitindia.attendance.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.bpitindia.attendance.data.Repository
import com.bpitindia.attendance.utils.Constants.LOG_TAG

suspend fun canUpdateOnServer(context: Context?=null,repository: Repository): Boolean {
    Log.d(LOG_TAG, "canUpdateOnServer: check")
    val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    val isInternetAvailable = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)?:false
    val isServerAvailable = try {
        repository.remote.health().isSuccessful
    }catch (e:Exception){
        false
    }
    return isInternetAvailable && isServerAvailable
}