package com.omar.findmysong.network.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ServerDiscovery @Inject constructor(@ApplicationContext val context: Context) {

    private val serviceType = "_findmysong._tcp."
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var listener: NsdManager.DiscoveryListener? = null

    private val _state = MutableStateFlow<State>(State.NotFound)
    val state = _state.asStateFlow()

    private var multicastLock : WifiManager.MulticastLock? = null

    fun startDiscovery() {
        if (listener != null) {
            nsd.stopServiceDiscovery(listener)
        }
        acquireWifiMulticast()
        listener = buildListener()
        nsd.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stopDiscovery() {
        nsd.stopServiceDiscovery(listener)
        releaseLock()
        listener = null
    }

    private fun acquireWifiMulticast() {
        releaseLock()
        multicastLock = (context.getSystemService(Context.WIFI_SERVICE) as WifiManager).createMulticastLock("Find My Song")
        multicastLock!!.setReferenceCounted(true)
        multicastLock!!.acquire()
    }

    private fun releaseLock() {
        multicastLock?.release()
    }

    private fun buildListener() = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Timber.e("Failed to start service discovery ${nsdErrorToString(errorCode)}")
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Timber.e("Failed to stop service discovery ${nsdErrorToString(errorCode)}")
        }

        override fun onDiscoveryStarted(serviceType: String?) {
            Timber.d("Service Discovery started for $serviceType")
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            Timber.d("Service Discovery stopped for $serviceType")
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
            val resolver = createResolveListener()
            nsd.resolveService(serviceInfo, resolver)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            Timber.d("Service lost ${serviceInfo.toString()}")
        }
    }

    private fun createResolveListener() = object : NsdManager.ResolveListener {
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Timber.d("Service Resolved, Name: ${serviceInfo.serviceType}, Host: ${serviceInfo.host}")
            onServiceFound(serviceInfo.host.hostAddress, serviceInfo.port)
            serviceInfo.attributes
        }

        override fun onResolveFailed(
            serviceInfo: NsdServiceInfo?,
            errorCode: Int
        ) {
            Timber.e("Resolve Service Failed, ${nsdErrorToString(errorCode)}")
        }
    }

    private fun onServiceFound(ip: String, port: Int) {
        val currentState = _state.value
        if (currentState is State.Found && currentState.ip == ip && currentState.port == port)
            return
        _state.value = State.Found(ip, port)
    }

    private fun onServiceDisconnected() {
        _state.value = State.NotFound
    }

    private fun nsdErrorToString(errorCode: Int) = when (errorCode) {
        NsdManager.FAILURE_INTERNAL_ERROR -> "Internal Error"
        NsdManager.FAILURE_MAX_LIMIT -> "Max Limit Reached"
        NsdManager.FAILURE_ALREADY_ACTIVE -> "Already Active"
        NsdManager.FAILURE_BAD_PARAMETERS -> "Bad Parameters"
        NsdManager.FAILURE_OPERATION_NOT_RUNNING -> "Operation not running"
        else -> "Unknown Error"
    }

    sealed class State {
        object NotFound: State()
        class Found(val ip: String, val port: Int): State()
    }
}