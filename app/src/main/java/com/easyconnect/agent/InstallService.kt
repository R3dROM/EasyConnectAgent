package com.easyconnect.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.jvm.java

class InstallService : Service()
{
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        createNotification()
        startForeground(2, createNotification())
        val apkPath = intent?.getStringExtra("apkPath") ?: return START_NOT_STICKY
        val apkFile = File(apkPath)

        CoroutineScope(Dispatchers.IO).launch {
            InstallExperience(applicationContext, apkFile)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun InstallExperience(context: Context, bundle: File)
    {
        val packageInstall = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
//        params.setRequireUserAction(
//            PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED
//        )
        val sessionID = packageInstall.createSession(params)
        val session = packageInstall.openSession(sessionID)

        bundle.inputStream().use { input ->
            session.openWrite("app_install", 0, bundle.length()).use { output ->
                input.copyTo(output)
                session.fsync(output)
            }
        }
        val intent = Intent(this, InstallResultActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val pendingIntent = PendingIntent.getActivity(
            this,
            sessionID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        session.commit(pendingIntent.intentSender)
        session.close()
    }
    private fun createNotification(): Notification {
        val channelId = "deploy_channel"

        val channel = NotificationChannel(
            channelId,
            "Deploy",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Deploy", NotificationManager.IMPORTANCE_LOW)
            nm?.createNotificationChannel(channel)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("EasyDeploy")
            .setContentText("Downloading...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()
    }
    override fun onBind(intent: Intent?) = null
}