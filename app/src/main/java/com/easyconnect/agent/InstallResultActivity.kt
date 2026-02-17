package com.easyconnect.agent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.util.Log

class InstallResultActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)

        when (status) {

            PackageInstaller.STATUS_SUCCESS -> {
                Log.d("INSTALL", "Instalación exitosa")
            }

            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent =
                    intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)

                startActivity(confirmIntent)
            }

            else -> {
                val message =
                    intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Log.e("INSTALL", "Error instalación: $message")
            }
        }

        finish()
    }
}
