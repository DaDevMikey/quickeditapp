/**
 * BootReceiver - Ensures persistence of tweaks after a device restart.
 * Developed by: DaDevMikey
 * 
 * This receiver listens for the system 'BOOT_COMPLETED' event. Since the 
 * WRITE_SECURE_SETTINGS permission persists across reboots, this class
 * can re-apply the user's chosen tweaks without needing manual intervention
 * or Shizuku to be actively running at boot time.
 */

package com.example.quickeditapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        const val CHANNEL_ID = "boot_status"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d("BootReceiver", "Reboot detected, reapplying Quick Panel tweaks...")
            
            var success = false
            try {
                val sharedPrefs = context.getSharedPreferences("tweak_prefs", Context.MODE_PRIVATE)
                val editMore = sharedPrefs.getBoolean(QuickPanelTweaks.KEY_EDIT_MORE, false)
                val landscape = sharedPrefs.getBoolean(QuickPanelTweaks.KEY_LANDSCAPE, false)
                val percent = sharedPrefs.getBoolean(QuickPanelTweaks.KEY_PERCENT, false)

                QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_EDIT_MORE, editMore)
                QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_LANDSCAPE, landscape)
                QuickPanelTweaks.setSetting(context, QuickPanelTweaks.KEY_PERCENT, percent)
                success = true
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to apply tweaks", e)
            }

            showBootNotification(context, success)
        }
    }

    private fun showBootNotification(context: Context, success: Boolean) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Boot Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Reports if tweaks were successfully applied on boot"
            }
            manager.createNotificationChannel(channel)
        }

        val message = if (success) {
            "Quick Panel tweaks were successfully reapplied."
        } else {
            "Failed to reapply Quick Panel tweaks. Open the app to fix."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Using existing icon
            .setContentTitle("QuickEdit Boot Status")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}
