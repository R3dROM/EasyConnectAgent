package com.easyconnect.agent.pico

import android.content.Context
import android.util.Log
import com.easyconnect.agent.data.PicoConfigurationDataStore
import com.easyconnect.agent.data.PersistentData
import com.easyconnect.agent.interfaces.IPicoActivity
import com.easyconnect.agent.interfaces.IPicoControlAPP
import com.easyconnect.agent.interfaces.IPicoDebug
import com.easyconnect.agent.interfaces.IPicoFile
import com.easyconnect.agent.interfaces.IPicoInformation
import com.easyconnect.agent.interfaces.IPicoLBE
import com.easyconnect.agent.interfaces.IPicoUtilities
import com.easyconnect.agent.interfaces.IPicoWifi
import com.pvr.tobservice.ToBServiceHelper
import com.pvr.tobservice.interfaces.IToBServiceProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

class PicoConfigManager(
    private val context: Context)
{
    private var picoBinding = false
    private var picoHelper: ToBServiceHelper?= null
    val picoStore = PicoConfigurationDataStore(context)
    var picoFileService: IPicoFile ?= null
    var picoActivityService: IPicoActivity ?= null
    var picoDebugService: IPicoDebug ?= null
    var picoLbeService: IPicoLBE ?= null
    var picoUtilitiesService: IPicoUtilities ?= null
    var picoWifiService: IPicoWifi ?= null
    var picoInformationService: IPicoInformation ?= null
    var picoControlAPP: IPicoControlAPP ?= null
    suspend fun picoConnection() : Boolean =
        suspendCancellableCoroutine { continuation ->
            if (picoBinding)
            {
                Log.e("picoServiceAsProxy","Already bind to PICO")
                continuation.resume(true)
                return@suspendCancellableCoroutine
            }
            picoBinding = true
            picoHelper = ToBServiceHelper.getInstance()

            picoHelper?.bindTobService(context.applicationContext) { p0 ->
                val proxy = picoHelper?.serviceBinder as? IToBServiceProxy
                val binder = picoHelper?.serviceBinder

                if (proxy == null || binder == null) {
                    picoBinding = false
                    Log.e("PICO_BINDER", "serviceBinder is null or invalid")
                    continuation.resume(false)
                    return@bindTobService
                }
                if (p0 == true) {

                    picoFileService = PicoFileSystem(proxy)
                    picoActivityService = PicoActivity(proxy)
                    picoDebugService = PicoDebug(proxy)
                    picoLbeService = PicoLBE(proxy)
                    picoUtilitiesService = PicoUtilities(proxy)
                    picoWifiService = PicoWifi(proxy)
                    picoInformationService = PicoInformation(proxy)
                    picoControlAPP = PicoControlAPP(context, binder, proxy)
                    Log.e("PICO_BINDER", "picoServiceAsProxy is working")
                } else {
                    picoBinding = false
                    Log.i("PICO_BINDER", "bind failure")
                }

                if (continuation.isActive) {
                    continuation.resume(p0 == true)
                }
            }
        }
    fun picoDisconnection()
    {
        picoHelper?.unBindTobService(context)
        picoHelper = null
        picoFileService = null
        picoBinding = false
    }
}