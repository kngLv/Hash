package com.hash.net.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** 返回当前活动网络是否具备访问互联网的能力。 */
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
