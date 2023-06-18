@file:JvmName("FetchAndroidExtensions")

package com.tonyodev.fetch2core

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build


fun Context.isOnWiFi(): Boolean {
    val manager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nw = manager.activeNetwork ?: return false
        val actNw = manager.getNetworkCapabilities(nw) ?: return false

        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            else -> false
        }
    } else @Suppress("DEPRECATION")
    {
        val activeNetworkInfo = manager.activeNetworkInfo
        return if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
            activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
        } else {
            false
        }

    }

}

@SuppressLint("ObsoleteSdkInt")
fun Context.isOnMeteredConnection(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= 16) {
        cm.isActiveNetworkMetered
    } else
        @Suppress("DEPRECATION")
        {
            val info: android.net.NetworkInfo = cm.activeNetworkInfo ?: return true
            when (info.type) {
                ConnectivityManager.TYPE_MOBILE,
                ConnectivityManager.TYPE_MOBILE_DUN,
                ConnectivityManager.TYPE_MOBILE_HIPRI,
                ConnectivityManager.TYPE_MOBILE_MMS,
                ConnectivityManager.TYPE_MOBILE_SUPL,
                ConnectivityManager.TYPE_WIMAX,
                -> true

                ConnectivityManager.TYPE_WIFI,
                ConnectivityManager.TYPE_BLUETOOTH,
                ConnectivityManager.TYPE_ETHERNET,
                -> false

                else -> true
            }
        }
}

fun Context.isNetworkAvailable(): Boolean {
    val manager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val nw = manager.activeNetwork ?: return false
        val actNw = manager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            //for other device how are able to connect with Ethernet
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            //for check internet over Bluetooth
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    } else
        @Suppress("DEPRECATION")
        {
            val activeNetworkInfo = manager.activeNetworkInfo ?: return false
            var connected = activeNetworkInfo.isConnected
            if (!connected) {
                connected = manager.allNetworkInfo.any { it.isConnected }
            }
            return connected
        }

}