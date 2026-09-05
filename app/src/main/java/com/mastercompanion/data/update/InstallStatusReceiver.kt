package com.mastercompanion.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import timber.log.Timber

/**
 * BroadcastReceiver for handling PackageInstaller session callbacks.
 */
class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                Timber.i("PackageInstaller requested user action.")
                @Suppress("DEPRECATION")
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (confirmIntent != null) {
                    context.startActivity(confirmIntent)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Timber.i("PackageInstaller installation succeeded! Waiting for restart broadcast...")
            }
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Timber.w("PackageInstaller returned status: $status ($message)")
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_STATUS = "com.mastercompanion.INSTALL_STATUS"
    }
}
