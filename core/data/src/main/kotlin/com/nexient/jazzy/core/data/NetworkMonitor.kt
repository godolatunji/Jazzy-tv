package com.nexient.jazzy.core.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkStatus(val online: Boolean, val metered: Boolean)

/** Online/metered state for the offline banner (HM-5) and the metered-network warning (TE-11). */
@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    fun current(): NetworkStatus {
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        return NetworkStatus(
            online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
            metered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) != true,
        )
    }

    val status: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(current()) }
            override fun onLost(network: Network) { trySend(current()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(current()) }
        }
        trySend(current())
        cm?.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), callback)
        awaitClose { cm?.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()
}
