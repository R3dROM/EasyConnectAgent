package com.easyconnect.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.jvm.java

class DeployReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val url = intent.getStringExtra("url") ?: run { Log.e("DEPLOY","url missing"); return  }
        val bundle = intent.getStringExtra("bundle") ?: run { Log.e("DEPLOY","bundle missing"); return  }
        val fileName = intent.getStringExtra("filename") ?: run { Log.e("DEPLOY","filename missing"); return  }

        val serviceIntent = Intent(context, DownloadService::class.java).apply {
            putExtra("url", url)
            putExtra("bundle", bundle)
            putExtra("filename", fileName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}