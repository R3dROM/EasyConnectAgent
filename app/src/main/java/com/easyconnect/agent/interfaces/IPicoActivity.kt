package com.easyconnect.agent.interfaces

import android.content.ComponentName
import android.content.Intent

interface IPicoActivity {
    fun startService(intent: Intent) : ComponentName?
    fun startActivity(
        packageName: String
    ): Int
}