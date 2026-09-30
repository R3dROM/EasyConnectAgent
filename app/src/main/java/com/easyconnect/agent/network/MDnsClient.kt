package com.easyconnect.agent.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.easyconnect.agent.interfaces.IUdpBroadcast
import com.easyconnect.agent.network.connectionManager.ConnectionInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MDnsClient(
    appContext: Context
) : IUdpBroadcast {
    private val nsdManager =
        appContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    override suspend fun startClient () : ConnectionInfo? =
        suspendCancellableCoroutine { continuation ->
            lateinit var discoveryListener: NsdManager.DiscoveryListener

            val resolveListener = object : NsdManager.ResolveListener {

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    try {
                        nsdManager.stopServiceDiscovery(discoveryListener)
                    } catch (e: Exception) {
                        Log.e("NSD", "Error stopping discovery", e)
                    }

                    val attributes = serviceInfo.attributes

                    val ip = attributes.getValue("ipAddress")?.toString(Charsets.UTF_8) ?: ""
                    val dport = attributes.getValue("downloadPort")?.toString(Charsets.UTF_8) ?: ""
                    val wbport = attributes.getValue("websocketPort")?.toString(Charsets.UTF_8) ?: ""

                    val information = ConnectionInfo(
                        ip,
                        dport,
                        wbport
                    )

                    if (continuation.isActive) {
                        continuation.resume(information)
                    }
                }

                override fun onResolveFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int
                ) {
                    Log.e("NSD", "Resolve failed: $errorCode")
                    if (continuation.isActive) {
                        try {
                            nsdManager.stopServiceDiscovery(discoveryListener)
                        } catch (e: Exception) {
                            Log.e("NSD", "Error stopping discovery", e)
                        }
                        continuation.resume(null)
                    }
                }
            }

            discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {
                    Log.i("NSD", "Discovery started: $serviceType")
                }

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    Log.i("NSD", "Found: ${serviceInfo.serviceName}")

                    if (serviceInfo.serviceType == "_easyconnect._tcp.") {
                        Log.i("NSD", "Found serviceType: ${serviceInfo.serviceType}")
                        nsdManager.resolveService(
                            serviceInfo,
                            resolveListener
                        )
                    }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    Log.i("NSD", "Lost: ${serviceInfo.serviceName}")
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    Log.i("NSD", "Discovery stopped")
                }

                override fun onStartDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int
                ) {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }

                override fun onStopDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int
                ) {

                }
            }
            nsdManager.discoverServices(
                "_easyconnect._tcp.",
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
            continuation.invokeOnCancellation {
                try {
                    nsdManager.stopServiceDiscovery(discoveryListener)
                } catch (e: Exception) {
                    Log.e("NSD", "Error stopping discovery", e)
                }
            }
        }
}