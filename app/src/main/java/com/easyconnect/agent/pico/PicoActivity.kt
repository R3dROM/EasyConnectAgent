package com.easyconnect.agent.pico

import android.content.ComponentName
import android.content.Intent
import com.easyconnect.agent.interfaces.IPicoActivity
import com.pvr.tobservice.interfaces.IToBServiceProxy

class PicoActivity(
    private val service: IToBServiceProxy
): IPicoActivity {

    override fun startService(intent: Intent) : ComponentName? =
        service.startForegroundService(intent)

    override fun startActivity(
        packageName: String
    ) : Int =
        service.pbsStartActivity(
            packageName,
            "com.unity3d.player.UnityPlayerActivity",
            "",
            "",
            arrayOf(Intent.CATEGORY_DEFAULT),
            intArrayOf(Intent.FLAG_ACTIVITY_NEW_TASK),
            0
        )
}