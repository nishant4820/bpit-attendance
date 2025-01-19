package com.bpitindia.attendance.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.bpitindia.attendance.data.Repository

suspend fun canUpdateOnServer(context: Context?=null,repository: Repository): Boolean {
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